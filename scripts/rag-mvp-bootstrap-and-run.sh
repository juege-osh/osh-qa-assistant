#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:19088}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
CONSUMER_USER="${CONSUMER_USER:-course_demo_user}"
CONSUMER_PWD="${CONSUMER_PWD:-123456}"
LIB_NAME="${LIB_NAME:-RAG MVP 验收知识库}"
APP_NAME="${APP_NAME:-RAG MVP 验收助手}"
BATCH_PREFIX="${BATCH_PREFIX:-RAG MVP 默认问题集跑测}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd curl
need_cmd python3

TMP_DIR="$(mktemp -d)"
DOC_FILE="$TMP_DIR/rag-mvp-seed.md"
RESULT_FILE="$TMP_DIR/result.json"
trap 'rm -rf "$TMP_DIR"' EXIT

cat > "$DOC_FILE" <<'EOF'
# RAG MVP 验收演示知识

## 知识库覆盖范围
这个知识库主要覆盖三个主题：
1. 内部文档知识问答
2. 训练测试任务的起步指导
3. 知识库命中失败时的体面反馈规范

## 核心结论
当用户提问时，系统应该优先给出有依据的答案。
如果答案来自知识库，应该说明依据来自当前知识库内容。
如果知识库没有覆盖，应该明确说明当前缺少足够依据，避免直接编造答案。

## 开始一个训练测试任务的建议
第一步先确认目标任务名称和所属知识库。
第二步查看知识库当前生效切分版本，确认最近一次发布策略。
第三步阅读应用级提示词和模型配置，确保回答范围符合业务约束。
第四步执行默认问题集跑测，观察命中问题、可信、易懂、失败体面四项结果。

## 走通流程的推荐顺序
先准备文档，再上传到知识库。
然后保存切分实验版本，并发布为当前生效版本。
接着重建索引并绑定应用。
最后运行默认问题集跑测，查看正式验收批次，再根据结果修补知识、切分和提示词。

## 模糊提问时的处理
如果用户只说“这个事情怎么弄”，系统应该先提示用户补充制度名称、流程名称或任务名称。
如果暂时无法补充，也可以先给出一个最小起步建议，例如先查看知识库覆盖范围和当前生效版本。

## 未命中知识时的反馈
如果当前问题不在知识库覆盖范围内，系统应该明确说明“当前知识库没有足够依据支持这个回答”。
系统应该建议用户换一个更具体的问法，或者联系运营人补充知识库内容。

## 检索不可用时的反馈
如果知识库检索暂时不可用，系统应该提示“知识库检索暂时不可用，请稍后重试，或换一个更明确的问题”。
系统不应该把底层报错堆栈直接展示给最终用户。
EOF

python3 - "$BASE_URL" "$REDIS_HOST" "$REDIS_PORT" "$REDIS_PASSWORD" "$CONSUMER_USER" "$CONSUMER_PWD" "$LIB_NAME" "$APP_NAME" "$BATCH_PREFIX" "$DOC_FILE" <<'PY' > "$RESULT_FILE"
import json
import socket
import sys
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path
from urllib.parse import urljoin

base_url, redis_host, redis_port, redis_password, consumer_user, consumer_pwd, lib_name, app_name, batch_prefix, doc_file = sys.argv[1:]
redis_port = int(redis_port)

def redis_cmd(*parts):
    payload = f"*{len(parts)}\r\n".encode()
    for part in parts:
        data = str(part).encode()
        payload += f"${len(data)}\r\n".encode() + data + b"\r\n"
    return payload

def read_line(sock):
    data = b""
    while not data.endswith(b"\r\n"):
        chunk = sock.recv(1)
        if not chunk:
            raise RuntimeError("redis connection closed unexpectedly")
        data += chunk
    return data[:-2]

def read_resp(sock):
    prefix = sock.recv(1)
    if not prefix:
        raise RuntimeError("empty redis response")
    marker = prefix.decode()
    if marker == "+":
        return read_line(sock).decode()
    if marker == "-":
        raise RuntimeError(read_line(sock).decode())
    if marker == ":":
        return int(read_line(sock))
    if marker == "$":
        length = int(read_line(sock))
        if length == -1:
            return None
        data = b""
        while len(data) < length + 2:
            data += sock.recv(length + 2 - len(data))
        return data[:-2].decode()
    if marker == "*":
        count = int(read_line(sock))
        return [read_resp(sock) for _ in range(count)]
    raise RuntimeError(f"unsupported redis response: {marker}")

def redis_get(key):
    with socket.create_connection((redis_host, redis_port), timeout=5) as sock:
        if redis_password:
            sock.sendall(redis_cmd("AUTH", redis_password))
            read_resp(sock)
        sock.sendall(redis_cmd("GET", key))
        raw = read_resp(sock)
    if raw is None:
        return None
    if len(raw) >= 2 and raw[0] == '"' and raw[-1] == '"':
        try:
            return json.loads(raw)
        except Exception:
            return raw.strip('"')
    return raw

def http_json(method, path, data=None, token=None):
    req = urllib.request.Request(urljoin(base_url, path), method=method)
    if data is not None:
        req.add_header("Content-Type", "application/json")
        body = json.dumps(data).encode()
    else:
        body = None
    if token:
        req.add_header("Authorization", token)
    try:
        with urllib.request.urlopen(req, data=body, timeout=120) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as exc:
        payload = exc.read().decode()
        raise RuntimeError(f"http error {exc.code} {path}: {payload}") from exc

def upload_file(path, token):
    boundary = "----CodexRagMvpBoundary"
    file_path = Path(path)
    file_bytes = file_path.read_bytes()
    filename = file_path.name
    parts = []
    parts.append(
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="module"\r\n\r\n'
        "document\r\n"
    )
    parts.append(
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'
        "Content-Type: text/markdown\r\n\r\n"
    )
    body = b"".join(part.encode() for part in parts) + file_bytes + f"\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(urljoin(base_url, "/consumer/storage/uploadFile"), method="POST", data=body)
    req.add_header("Authorization", token)
    req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.loads(resp.read().decode())

def get_captcha():
    result = http_json("GET", "/consumer/user/getCode")
    captcha_id = result["data"]["captchaId"]
    code = redis_get(captcha_id)
    if not code:
        raise RuntimeError(f"captcha not found in redis: {captcha_id}")
    return captcha_id, code

def ensure_login():
    captcha_id, code = get_captcha()
    result = http_json("POST", "/consumer/user/login", {
        "username": consumer_user,
        "pwd": consumer_pwd,
        "captchaId": captcha_id,
        "code": code,
    })
    if result.get("code") == 200:
        return result["data"]["token"], "existing"
    captcha_id, code = get_captcha()
    register = http_json("POST", "/consumer/user/register", {
        "username": consumer_user,
        "pwd": consumer_pwd,
        "captchaId": captcha_id,
        "code": code,
    })
    if register.get("code") != 200:
        raise RuntimeError(f"register failed: {register}")
    captcha_id, code = get_captcha()
    login = http_json("POST", "/consumer/user/login", {
        "username": consumer_user,
        "pwd": consumer_pwd,
        "captchaId": captcha_id,
        "code": code,
    })
    if login.get("code") != 200:
        raise RuntimeError(f"login after register failed: {login}")
    return login["data"]["token"], "registered"

def find_record(records, key, value):
    for item in records:
        if item.get(key) == value:
            return item
    return None

def extract_records(resp):
    data = resp.get("data")
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        records = data.get("records")
        if isinstance(records, list):
            return records
    return []

token, user_status = ensure_login()

libs_resp = http_json("POST", "/consumer/knowledgeLib/queryPage", {"pageNow": 1, "pageSize": 100}, token)
libs = extract_records(libs_resp)
lib = find_record(libs, "libName", lib_name)
if lib is None:
    add_lib = http_json("POST", "/consumer/knowledgeLib/add", {"libName": lib_name, "libDesc": "用于默认问题集真实跑测的最小知识库"}, token)
    if add_lib.get("code") != 200:
        raise RuntimeError(f"add lib failed: {add_lib}")
    libs_resp = http_json("POST", "/consumer/knowledgeLib/queryPage", {"pageNow": 1, "pageSize": 100}, token)
    libs = extract_records(libs_resp)
    lib = find_record(libs, "libName", lib_name)
    if lib is None:
        raise RuntimeError("library created but not found")

apps_resp = http_json("POST", "/consumer/app/queryPage", {"pageNow": 1, "pageSize": 100}, token)
apps = extract_records(apps_resp)
app = find_record(apps, "appName", app_name)
if app is None:
    add_app = http_json("POST", "/consumer/app/add", {
        "appName": app_name,
        "appDesc": "用于默认问题集真实跑测的最小应用",
        "outLibEnable": 0,
        "customPrompt": "请优先基于知识库回答；如果缺少依据，请明确说明缺少依据并给出下一步建议。",
        "chatModel": ""
    }, token)
    if add_app.get("code") != 200:
        raise RuntimeError(f"add app failed: {add_app}")
    apps_resp = http_json("POST", "/consumer/app/queryPage", {"pageNow": 1, "pageSize": 100}, token)
    apps = extract_records(apps_resp)
    app = find_record(apps, "appName", app_name)
    if app is None:
        raise RuntimeError("app created but not found")

if app.get("libId") != lib.get("id"):
    bind_resp = http_json("POST", "/consumer/app/bindLib", {"id": app["id"], "libId": lib["id"]}, token)
    if bind_resp.get("code") != 200:
        raise RuntimeError(f"bind lib failed: {bind_resp}")

upload_resp = upload_file(doc_file, token)
if upload_resp.get("code") != 200:
    raise RuntimeError(f"storage upload failed: {upload_resp}")
relative_path = upload_resp["data"]["relativePath"]

files_resp = http_json("POST", "/consumer/uploadFile/queryPage", {"pageNow": 1, "pageSize": 100, "libId": lib["id"]}, token)
files = extract_records(files_resp)
seed_name = Path(doc_file).name
file_record = find_record(files, "fileName", seed_name)
if file_record is None:
    add_file = http_json("POST", "/consumer/uploadFile/add", {
        "libId": lib["id"],
        "storePath": relative_path,
        "originalFileName": seed_name
    }, token)
    if add_file.get("code") != 200:
        raise RuntimeError(f"add upload file failed: {add_file}")
    files_resp = http_json("POST", "/consumer/uploadFile/queryPage", {"pageNow": 1, "pageSize": 100, "libId": lib["id"]}, token)
    files = extract_records(files_resp)
    file_record = find_record(files, "fileName", seed_name)

semantic_config = {
    "strategy": "semantic",
    "chunkSize": 800,
    "minChunkSizeChars": 350,
    "minChunkLengthToEmbed": 5,
    "maxNumChunks": 10000,
    "keepSeparator": True,
    "semanticSectionMaxChars": 1200,
    "previewChunkLimit": 8
}
token_config = {
    "strategy": "token",
    "chunkSize": 500,
    "minChunkSizeChars": 200,
    "minChunkLengthToEmbed": 5,
    "maxNumChunks": 10000,
    "keepSeparator": True,
    "semanticSectionMaxChars": 1200,
    "previewChunkLimit": 8
}

def ensure_experiment(version_name, split_strategy, config_json):
    exp_list_resp = http_json("GET", f"/consumer/knowledgeLib/experiment/list?libId={lib['id']}", token=token)
    exp_list = exp_list_resp.get("data") or []
    existing = find_record(exp_list, "versionName", version_name)
    if existing is not None:
        return existing
    save_resp = http_json("POST", "/consumer/knowledgeLib/experiment/save", {
        "libId": lib["id"],
        "versionName": version_name,
        "queryText": "默认问题集自动跑测准备",
        "topK": 5,
        "splitStrategy": split_strategy,
        "splitConfigJson": json.dumps(config_json, ensure_ascii=False),
        "rawHitCount": 1,
        "rerankHitCount": 1,
        "diagnosisTitle": "用于默认问题集跑测的最小实验版本",
        "categoryCode": "chunking",
        "categoryLabel": "切分策略",
        "rawTopSummary": "最小样本数据初始化",
        "rerankTopSummary": "最小样本数据初始化",
        "noteText": "由脚本自动创建，用于真实跑测闭环验证",
        "recommendReason": "用于对比不同切分版本的默认问题集结果",
        "recommended": 0
    }, token)
    if save_resp.get("code") != 200:
        raise RuntimeError(f"save experiment failed: {save_resp}")
    exp_list_resp = http_json("GET", f"/consumer/knowledgeLib/experiment/list?libId={lib['id']}", token=token)
    exp_list = exp_list_resp.get("data") or []
    existing = find_record(exp_list, "versionName", version_name)
    if existing is None:
        raise RuntimeError(f"experiment {version_name} created but not found")
    return existing

semantic_exp = ensure_experiment("脚本语义切分版", "semantic", semantic_config)
token_exp = ensure_experiment("脚本Token切分版", "token", token_config)

def publish_experiment(experiment):
    publish_resp = http_json("POST", "/consumer/knowledgeLib/experiment/publish", {
        "libId": lib["id"],
        "id": experiment["id"]
    }, token)
    if publish_resp.get("code") != 200:
        raise RuntimeError(f"publish experiment failed: {publish_resp}")

def run_batch(experiment, suffix):
    publish_experiment(experiment)
    batch_name = f"{batch_prefix}-{suffix}-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
    run_resp = http_json("POST", "/consumer/ragAcceptance/runDefaultBatch", {
        "appId": app["id"],
        "batchName": batch_name,
        "sceneType": "内部知识问答",
        "testerName": "codex"
    }, token)
    if run_resp.get("code") != 200:
        raise RuntimeError(f"run default batch failed: {run_resp}")
    batch_id = run_resp["data"]
    detail_resp = http_json("GET", f"/consumer/ragAcceptance/detail?id={batch_id}", token=token)
    if detail_resp.get("code") != 200:
        raise RuntimeError(f"load batch detail failed: {detail_resp}")
    return detail_resp["data"]

semantic_batch = run_batch(semantic_exp, "semantic")
token_batch = run_batch(token_exp, "token")

output = {
    "baseUrl": base_url,
    "userStatus": user_status,
    "user": consumer_user,
    "lib": {
        "id": lib["id"],
        "name": lib["libName"],
    },
    "app": {
        "id": app["id"],
        "name": app["appName"],
    },
    "uploadFile": {
        "storePath": relative_path,
        "fileName": seed_name,
    },
    "experiments": [
        {
            "id": semantic_exp["id"],
            "versionName": semantic_exp["versionName"],
            "splitStrategy": semantic_exp["splitStrategy"],
        },
        {
            "id": token_exp["id"],
            "versionName": token_exp["versionName"],
            "splitStrategy": token_exp["splitStrategy"],
        }
    ],
    "batches": [
        {
            "id": semantic_batch["id"],
            "batchName": semantic_batch["batchName"],
            "activeExperimentName": semantic_batch.get("activeExperimentName"),
            "activeSplitStrategy": semantic_batch.get("activeSplitStrategy"),
            "itemCount": semantic_batch.get("itemCount"),
            "passCount": semantic_batch.get("passCount"),
            "followUpCount": semantic_batch.get("followUpCount"),
        },
        {
            "id": token_batch["id"],
            "batchName": token_batch["batchName"],
            "activeExperimentName": token_batch.get("activeExperimentName"),
            "activeSplitStrategy": token_batch.get("activeSplitStrategy"),
            "itemCount": token_batch.get("itemCount"),
            "passCount": token_batch.get("passCount"),
            "followUpCount": token_batch.get("followUpCount"),
        }
    ]
}

print(json.dumps(output, ensure_ascii=False))
PY

python3 - "$RESULT_FILE" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as f:
    result = json.load(f)

print("RAG MVP 最小闭环已执行")
print(f"- 目标环境: {result['baseUrl']}")
print(f"- 用户状态: {result['userStatus']} ({result['user']})")
print(f"- 知识库: {result['lib']['name']} #{result['lib']['id']}")
print(f"- 应用: {result['app']['name']} #{result['app']['id']}")
print(f"- 样本文档: {result['uploadFile']['fileName']}")
print(f"- 存储路径: {result['uploadFile']['storePath']}")
for batch in result["batches"]:
    print(
        f"- 验收批次 #{batch['id']}: {batch['batchName']} | "
        f"{batch['activeExperimentName']} ({batch['activeSplitStrategy']}) | "
        f"items={batch['itemCount']} pass={batch['passCount']} followUp={batch['followUpCount']}"
    )
PY

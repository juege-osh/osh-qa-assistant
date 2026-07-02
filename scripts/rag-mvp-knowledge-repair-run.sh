#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.local}"

load_env_file_if_present() {
  if [ ! -f "$1" ]; then
    return
  fi
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      ''|\#*)
        continue
        ;;
    esac
    key="${line%%=*}"
    value="${line#*=}"
    if [ -z "$key" ]; then
      continue
    fi
    if [ -z "${!key+x}" ]; then
      export "$key=$value"
    fi
  done < "$1"
}

load_env_file_if_present "$ENV_FILE"

BASE_URL="${1:-http://127.0.0.1:19088}"
REDIS_HOST="${REDIS_HOST:-${AI_ASSISTANT_REDIS_HOST:-127.0.0.1}}"
REDIS_PORT="${REDIS_PORT:-${AI_ASSISTANT_REDIS_PORT:-6379}}"
REDIS_PASSWORD="${REDIS_PASSWORD:-${AI_ASSISTANT_REDIS_PASSWORD:-}}"
CONSUMER_USER="${CONSUMER_USER:-course_demo_user}"
CONSUMER_PWD="${CONSUMER_PWD:-123456}"
APP_NAME="${APP_NAME:-RAG MVP 验收助手}"
LIB_NAME="${LIB_NAME:-RAG MVP 验收知识库}"
TARGET_EXPERIMENT_NAME="${TARGET_EXPERIMENT_NAME:-脚本语义切分版}"
BATCH_PREFIX="${BATCH_PREFIX:-真实问题集正式验收-补知识复跑}"
PATCH_FILE_NAME="${PATCH_FILE_NAME:-rag-mvp-coverage-patch.md}"
REPORT_PATH="${REPORT_PATH:-}"
QUESTIONS_FILE="${QUESTIONS_FILE:-$ROOT_DIR/scripts/rag-mvp-real-questions.json}"
QUESTION_FILTER="${QUESTION_FILTER:-}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd curl
need_cmd python3

TMP_DIR="$(mktemp -d)"
PATCH_DOC="$TMP_DIR/$PATCH_FILE_NAME"
RESULT_FILE="$TMP_DIR/rag-knowledge-repair-result.json"
trap 'rm -rf "$TMP_DIR"' EXIT

cat > "$PATCH_DOC" <<'EOF'
# RAG MVP 覆盖范围补充说明

## 这份知识库当前最适合回答什么问题
这个知识库当前最适合回答三类问题，而且回答应尽量围绕这三类范围展开：

1. 内部文档知识问答
2. 训练测试任务的起步与执行顺序
3. 知识没命中时应该如何体面反馈

如果用户的问题能明确落在这三类里，系统应该直接回答，不要泛泛而谈。
如果用户的问题明显超出这三类，系统应该明确说明当前知识库没有足够依据支持回答。

## 三类问题的更具体边界

### 一类：内部文档知识问答
适合回答：

- 某份内部文档主要讲了什么
- 某个制度、流程、规则应该看哪类资料
- 当前知识库覆盖了哪些主题，哪些问题适合继续追问

不适合直接回答：

- 外部政策、外部法律、外部公共事实的最新解释
- 没有进入当前知识库的制度细节

### 二类：训练测试任务的起步与执行顺序
适合回答：

- 要开始训练测试任务时第一步做什么
- 整个流程建议按什么顺序推进
- 为什么要先看知识源、再看切分、再跑验收

推荐顺序应稳定包含这些动作：

1. 先确认任务目标、应用范围和对应知识库
2. 再确认当前生效的切分版本
3. 再查看应用级 Prompt 和模型开关
4. 再执行默认问题集或真实问题集验收
5. 最后根据结果回头补知识、调切分、修提示词和失败反馈

### 三类：知识没命中时如何体面反馈
适合回答：

- 当前知识库没有足够依据时应该怎么说
- 问题过于模糊时应该如何引导补充信息
- 检索暂时不可用时应给用户什么反馈

正确的反馈边界：

- 不要编造没有依据的答案
- 不要把底层报错堆栈直接给最终用户
- 要明确告诉用户当前缺什么信息
- 要给一个下一步动作，例如换更具体的问题，或者联系运营人补知识

## 用一句话概括知识库覆盖范围
如果用户直接问“这个知识库现在主要能回答哪些类型的问题”，更推荐用下面这句来概括：

这个知识库目前主要回答三类问题：内部文档知识问答、训练测试任务怎么开始和怎么走通、以及知识没命中时该如何给出体面反馈。

## 超范围时的明确边界
如果用户问的是外部法律、外部财税、外部医疗、外部合规等高风险问题，而当前知识库没有对应依据，系统应直接说明：

当前知识库里没有足够依据支持这个回答，我不能直接给出结论。你可以补充对应制度名称、流程来源，或联系运营人补充知识后再提问。

## 如何判断应该优先补知识、补切分还是补提示词
如果用户直接问“当前应该优先补知识、补切分，还是补提示词”，建议按下面顺序判断：

1. 先看正式验收批次里的四项结论和待跟进分类
2. 再看同一组问题在不同切分版本之间是否出现明显差异
3. 最后再决定进入补知识、补切分还是补提示词

### 更适合优先补知识的场景
- 问题本身就在知识库范围内，但回答反复说“没有足够依据”
- 不同切分版本都答不稳同一类问题
- 回答方向大体正确，但缺少明确规则、分类口径或操作边界

这类问题更像知识内容本身没写清楚，应该优先补知识。

### 更适合优先补切分的场景
- 同一组真实问题里，某个切分版本明显掉分，另一个版本明显更稳
- 长步骤题、覆盖范围题、总结型题更容易出现断裂、遗漏、顺序不稳
- 待跟进分类已经出现 `chunking`

这类问题应优先补切分，再复跑确认。

### 更适合优先补提示词的场景
- 知识依据本身存在，但答案组织顺序不好
- 回答结论不够直接，或者失败反馈不够体面
- 召回内容大体正确，但最终回答没有按“先结论、再依据、再下一步”组织

这类问题更适合优先补提示词。

## 切换生效切分版本后，如何验证是否真的生效
如果用户问“我切换了知识库的生效切分版本之后，下一步应该怎么验证切换到底有没有真的生效”，推荐稳定回答下面这套闭环：

1. 先确认目标实验版本已经发布为当前生效版本
2. 再按当前生效版本重建索引
3. 再按当前生效版本复跑同一组真实问题
4. 查看新批次里的 `activeExperimentName` 和 `activeSplitStrategy`
5. 再把新批次和上一轮正式验收批次做对比

更短的一句话可以直接概括为：

切换生效版本后，不要只看页面状态，要按当前生效版本复跑同一组真实问题，并核对批次里的 `activeExperimentName / activeSplitStrategy`，这样才能确认切换真的生效。

## 忘了重建索引时最容易出现什么假象
如果用户问“我切换了生效切分版本，但忘了重建索引，正式验收里最容易出现什么假象”，推荐明确回答下面这层区别：

1. 页面状态或批次字段里会先显示“当前生效版本已经切换”
2. 但真正被检索使用的内容可能还停留在旧索引
3. 因此最常见的伪生效假象是：
   - `activeExperimentName / activeSplitStrategy` 看起来已经是新版本
   - 但同一组真实问题的回答质量、命中差异、召回表现仍然像旧版本
   - 这会让人误以为“新版本没带来变化”或者误判成“模型 / 提示词问题”

更稳的总结方式应是：

- 切换生效版本，只是先切“标识”
- 重建索引，才会切“实际被检索使用的内容”
- 所以忘了重建索引时，最典型的假象就是“字段像新版本，结果像旧版本”

推荐补一句操作闭环：

1. 确认目标实验版本已发布为当前生效版本
2. 按当前生效版本重建索引
3. 再复跑同一组真实问题
4. 同时核对 `activeExperimentName / activeSplitStrategy` 和回答效果

如果字段已经切到新版本，但回答仍和旧版本几乎一样，应优先怀疑“索引还没按新版本重建完成”。

## 最小可重复验收的标准动作顺序
如果用户问“我只想做一轮最小可重复验收，开始前到复跑后的动作顺序应该怎么排”，推荐优先按下面这套标准闭环回答，而且不要只回答中间的跑测步骤。

### 开始前必须先确认的 4 件事
1. 先确认任务目标、任务名称和对应知识库
2. 再确认当前生效的切分版本
3. 再查看应用级 Prompt 和模型开关
4. 再确认这次要复用哪一组问题集做验收

### 最小可重复验收的推荐顺序
1. 先确认任务目标、知识库、生效切分版本、Prompt 和模型开关
2. 再准备文档并上传到知识库
3. 再保存切分实验版本，并发布为当前生效版本
4. 再重建索引并绑定应用
5. 再运行默认问题集或真实问题集跑测
6. 再查看正式验收批次里的四项结论和待跟进分类
7. 再根据结果决定补知识、补切分还是补提示词
8. 最后按当前生效版本复跑同一组问题，并核对 `activeExperimentName / activeSplitStrategy`

### 更短的一句话概括
如果要一句话说清楚，推荐直接回答：

最小可重复验收不是只跑一次问题集，而是先确认任务和当前生效版本，再跑正式验收，根据结果修补，最后按当前生效版本复跑并核对批次字段，形成闭环。
EOF

python3 - "$BASE_URL" "$REDIS_HOST" "$REDIS_PORT" "$REDIS_PASSWORD" "$CONSUMER_USER" "$CONSUMER_PWD" "$APP_NAME" "$LIB_NAME" "$TARGET_EXPERIMENT_NAME" "$BATCH_PREFIX" "$PATCH_DOC" "$REPORT_PATH" "$QUESTIONS_FILE" "$QUESTION_FILTER" <<'PY' > "$RESULT_FILE"
import json
import socket
import sys
import urllib.error
import urllib.request
from collections import Counter
from datetime import datetime
from pathlib import Path
from urllib.parse import urljoin

(base_url, redis_host, redis_port, redis_password, consumer_user, consumer_pwd,
 app_name, lib_name, target_experiment_name, batch_prefix, patch_doc, report_path, questions_file, question_filter) = sys.argv[1:]
redis_port = int(redis_port)

REAL_QUESTIONS = json.loads(Path(questions_file).read_text(encoding="utf-8"))

def apply_question_filter(questions, raw_filter):
    raw_filter = (raw_filter or "").strip()
    if not raw_filter:
        return questions
    allowed = {item.strip() for item in raw_filter.split(",") if item.strip()}
    return [item for item in questions if item.get("testCaseNo") in allowed]

REAL_QUESTIONS = apply_question_filter(REAL_QUESTIONS, question_filter)
if not REAL_QUESTIONS:
    raise RuntimeError("no questions selected after QUESTION_FILTER")

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
        with urllib.request.urlopen(req, data=body, timeout=180) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as exc:
        payload = exc.read().decode()
        raise RuntimeError(f"http error {exc.code} {path}: {payload}") from exc

def upload_file(path, token):
    boundary = "----CodexRagKnowledgeRepairBoundary"
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

def extract_records(resp):
    data = resp.get("data")
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        records = data.get("records")
        if isinstance(records, list):
            return records
    return []

def find_record(records, key, value):
    for item in records:
        if item.get(key) == value:
            return item
    return None

def require_app_and_lib(token):
    apps = extract_records(http_json("POST", "/consumer/app/queryPage", {"pageNow": 1, "pageSize": 100}, token))
    app = find_record(apps, "appName", app_name)
    if app is None:
        raise RuntimeError(f"app not found: {app_name}")
    libs = extract_records(http_json("POST", "/consumer/knowledgeLib/queryPage", {"pageNow": 1, "pageSize": 100}, token))
    lib = find_record(libs, "libName", lib_name)
    if lib is None:
        raise RuntimeError(f"knowledge lib not found: {lib_name}")
    if app.get("libId") != lib.get("id"):
        raise RuntimeError(f"app {app_name} is not bound to lib {lib_name}")
    return app, lib

def require_experiment(token, lib_id, version_name):
    exp_list_resp = http_json("GET", f"/consumer/knowledgeLib/experiment/list?libId={lib_id}", token=token)
    exp_list = exp_list_resp.get("data") or []
    experiment = find_record(exp_list, "versionName", version_name)
    if experiment is None:
        raise RuntimeError(f"experiment not found: {version_name}")
    return experiment

def publish_experiment(token, lib_id, experiment):
    publish_resp = http_json("POST", "/consumer/knowledgeLib/experiment/publish", {
        "libId": lib_id,
        "id": experiment["id"]
    }, token)
    if publish_resp.get("code") != 200:
        raise RuntimeError(f"publish experiment failed: {publish_resp}")

def list_upload_files(token, lib_id):
    return extract_records(http_json("POST", "/consumer/uploadFile/queryPage", {
        "pageNow": 1,
        "pageSize": 200,
        "libId": lib_id
    }, token))

def delete_upload_file(token, file_id):
    resp = http_json("GET", f"/consumer/uploadFile/deleteById?id={file_id}", token=token)
    if resp.get("code") != 200:
        raise RuntimeError(f"delete upload file failed: {resp}")

def add_upload_file(token, lib_id, store_path, file_name):
    resp = http_json("POST", "/consumer/uploadFile/add", {
        "libId": lib_id,
        "storePath": store_path,
        "originalFileName": file_name
    }, token)
    if resp.get("code") != 200:
        raise RuntimeError(f"add upload file failed: {resp}")

def require_latest_uploaded_file(token, lib_id, file_name):
    files = list_upload_files(token, lib_id)
    matched = [item for item in files if item.get("fileName") == file_name]
    if not matched:
        raise RuntimeError(f"uploaded file not found after add: {file_name}")
    matched.sort(key=lambda item: int(item.get("id")), reverse=True)
    return matched[0]

def rebuild_lib(token, lib_id):
    resp = http_json("GET", f"/consumer/uploadFile/rebuildByLibId?libId={lib_id}", token=token)
    if resp.get("code") != 200:
        raise RuntimeError(f"rebuild lib failed: {resp}")
    return resp.get("msg")

def preview_chunks(token, file_id, experiment):
    preview_resp = http_json("POST", "/consumer/file/previewSplit", {
        "id": file_id,
        "strategy": experiment.get("splitStrategy"),
        "previewChunkLimit": 8
    }, token)
    if preview_resp.get("code") != 200:
        raise RuntimeError(f"preview split failed: {preview_resp}")
    data = preview_resp.get("data") or {}
    return {
        "chunkCount": data.get("chunkCount"),
        "chunks": data.get("chunks") or []
    }

def run_real_batch(token, app_id, batch_prefix_value):
    batch_name = f"{datetime.now().strftime('%Y-%m-%d')} {batch_prefix_value}"
    run_resp = http_json("POST", "/consumer/ragAcceptance/runBatch", {
        "appId": app_id,
        "batchName": batch_name,
        "sceneType": "真实业务问题",
        "testerName": "codex",
        "summaryConclusion": "",
        "nextAction": "",
        "questions": REAL_QUESTIONS
    }, token)
    if run_resp.get("code") != 200:
        raise RuntimeError(f"run real batch failed: {run_resp}")
    batch_id = run_resp["data"]
    detail_resp = http_json("GET", f"/consumer/ragAcceptance/detail?id={batch_id}", token=token)
    if detail_resp.get("code") != 200:
        raise RuntimeError(f"load batch detail failed: {detail_resp}")
    return detail_resp["data"]

def metric_stats(items):
    fields = [
        ("hitConclusion", "hitPass"),
        ("groundedConclusion", "groundedPass"),
        ("readableConclusion", "readablePass"),
        ("gracefulFailureConclusion", "gracefulPass")
    ]
    result = {}
    for field, label in fields:
        result[label] = sum(1 for item in items if item.get(field) == "通过")
    return result

def category_stats(items):
    counter = Counter()
    for item in items:
        category = item.get("followUpCategory") or ""
        if category:
            counter[category] += 1
        elif item.get("followUpAction") or item.get("remark"):
            counter["other"] += 1
    return dict(counter)

def build_item_summary(item):
    return {
        "testCaseNo": item.get("testCaseNo"),
        "questionType": item.get("questionType"),
        "userQuestion": item.get("userQuestion"),
        "hitConclusion": item.get("hitConclusion"),
        "groundedConclusion": item.get("groundedConclusion"),
        "readableConclusion": item.get("readableConclusion"),
        "gracefulFailureConclusion": item.get("gracefulFailureConclusion"),
        "followUpCategory": item.get("followUpCategory"),
        "followUpAction": item.get("followUpAction"),
        "failReason": item.get("failReason"),
        "actualAnswerSummary": item.get("actualAnswerSummary"),
    }

def build_report(app, lib, experiment, preview, deleted_files, uploaded_file, rebuild_msg, batch):
    items = batch.get("items") or []
    metrics = metric_stats(items)
    categories = category_stats(items)
    lines = [
        "# RAG 补知识复跑结果",
        "",
        f"- 生成时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        f"- 应用：{app.get('appName')}",
        f"- 知识库：{lib.get('libName')} #{lib.get('id')}",
        f"- 生效实验版本：{experiment.get('versionName')} ({experiment.get('splitStrategy')})",
        f"- 替换补丁文件数：{len(deleted_files)}",
        f"- 新补丁文件：{uploaded_file.get('fileName')} #{uploaded_file.get('id')}",
        f"- 重建结果：{rebuild_msg}",
        f"- 切分预览 chunk 总数：{preview.get('chunkCount')}",
        "",
        "## 批次汇总",
        "",
        f"- 批次 ID：{batch.get('id')}",
        f"- 批次名称：{batch.get('batchName')}",
        f"- 全部通过：{batch.get('passCount')} / {batch.get('itemCount')}",
        f"- 待跟进：{batch.get('followUpCount')}",
        f"- 命中问题通过：{metrics.get('hitPass')}",
        f"- 可信通过：{metrics.get('groundedPass')}",
        f"- 易懂通过：{metrics.get('readablePass')}",
        f"- 失败体面通过：{metrics.get('gracefulPass')}",
        f"- 跟进分类：{json.dumps(categories, ensure_ascii=False)}",
        "",
        "## 逐题结果",
        "",
        "| 编号 | 结论 | 跟进分类 | 回答摘要 |",
        "| --- | --- | --- | --- |",
    ]
    for item in items:
        conclusion_text = "/".join([
            item.get("hitConclusion") or "-",
            item.get("groundedConclusion") or "-",
            item.get("readableConclusion") or "-",
            item.get("gracefulFailureConclusion") or "-"
        ])
        lines.append(
            f"| {item.get('testCaseNo')} | {conclusion_text} | "
            f"{item.get('followUpCategory') or '-'} | {item.get('actualAnswerSummary') or '-'} |"
        )
    return "\n".join(lines)

token, user_status = ensure_login()
app, lib = require_app_and_lib(token)
experiment = require_experiment(token, lib["id"], target_experiment_name)
publish_experiment(token, lib["id"], experiment)

existing_files = list_upload_files(token, lib["id"])
deleted_files = [
    {"id": item["id"], "fileName": item.get("fileName")}
    for item in existing_files
    if item.get("fileName") == Path(patch_doc).name
]
for item in deleted_files:
    delete_upload_file(token, item["id"])

upload_resp = upload_file(patch_doc, token)
if upload_resp.get("code") != 200:
    raise RuntimeError(f"storage upload failed: {upload_resp}")
relative_path = upload_resp["data"]["relativePath"]
patch_file_name = Path(patch_doc).name
add_upload_file(token, lib["id"], relative_path, patch_file_name)

updated_files = list_upload_files(token, lib["id"])
uploaded_file = require_latest_uploaded_file(token, lib["id"], patch_file_name)

rebuild_msg = rebuild_lib(token, lib["id"])
preview = preview_chunks(token, uploaded_file["id"], experiment)
batch = run_real_batch(token, app["id"], batch_prefix)
report_markdown = build_report(app, lib, experiment, preview, deleted_files, uploaded_file, rebuild_msg, batch)

if report_path:
    Path(report_path).write_text(report_markdown, encoding="utf-8")

result = {
    "baseUrl": base_url,
    "userStatus": user_status,
    "user": consumer_user,
    "app": {
        "id": app["id"],
        "name": app["appName"],
    },
    "lib": {
        "id": lib["id"],
        "name": lib["libName"],
    },
    "experiment": {
        "id": experiment["id"],
        "versionName": experiment["versionName"],
        "splitStrategy": experiment["splitStrategy"],
    },
    "deletedFiles": deleted_files,
    "uploadedFile": {
        "id": uploaded_file["id"],
        "fileName": uploaded_file.get("fileName"),
        "storePath": relative_path,
    },
    "rebuildMessage": rebuild_msg,
    "preview": preview,
    "batch": {
        "id": batch["id"],
        "batchName": batch["batchName"],
        "passCount": batch["passCount"],
        "itemCount": batch["itemCount"],
        "followUpCount": batch["followUpCount"],
        "summaryConclusion": batch.get("summaryConclusion"),
        "nextAction": batch.get("nextAction"),
        "items": [build_item_summary(item) for item in (batch.get("items") or [])]
    },
    "reportMarkdown": report_markdown
}

print(json.dumps(result, ensure_ascii=False))
PY

python3 - "$RESULT_FILE" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as f:
    result = json.load(f)

batch = result["batch"]
experiment = result["experiment"]
uploaded_file = result["uploadedFile"]

print("RAG 补知识复跑已执行")
print(f"- 目标环境: {result['baseUrl']}")
print(f"- 用户状态: {result['userStatus']} ({result['user']})")
print(f"- 应用: {result['app']['name']} #{result['app']['id']}")
print(f"- 知识库: {result['lib']['name']} #{result['lib']['id']}")
print(f"- 生效版本: {experiment['versionName']} ({experiment['splitStrategy']})")
print(f"- 替换补丁文件数: {len(result['deletedFiles'])}")
print(f"- 当前补丁文件: {uploaded_file['fileName']} #{uploaded_file['id']}")
print(f"- 重建结果: {result['rebuildMessage']}")
print(f"- 切分预览 chunk 总数: {result['preview']['chunkCount']}")
print(
    f"- 验收批次 #{batch['id']}: {batch['batchName']} | "
    f"items={batch['itemCount']} pass={batch['passCount']} followUp={batch['followUpCount']}"
)
PY

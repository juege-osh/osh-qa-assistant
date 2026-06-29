#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:19088}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
CONSUMER_USER="${CONSUMER_USER:-course_demo_user}"
CONSUMER_PWD="${CONSUMER_PWD:-123456}"
APP_NAME="${APP_NAME:-RAG MVP 验收助手}"
LIB_NAME="${LIB_NAME:-RAG MVP 验收知识库}"
LEFT_EXPERIMENT_NAME="${LEFT_EXPERIMENT_NAME:-脚本语义切分版}"
RIGHT_EXPERIMENT_NAME="${RIGHT_EXPERIMENT_NAME:-脚本Token切分版}"
LEFT_BATCH_PREFIX="${LEFT_BATCH_PREFIX:-真实问题集正式验收-语义切分}"
RIGHT_BATCH_PREFIX="${RIGHT_BATCH_PREFIX:-真实问题集正式验收-Token切分}"
REPORT_PATH="${REPORT_PATH:-}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd curl
need_cmd python3

TMP_DIR="$(mktemp -d)"
RESULT_FILE="$TMP_DIR/rag-real-compare-result.json"
trap 'rm -rf "$TMP_DIR"' EXIT

python3 - "$BASE_URL" "$REDIS_HOST" "$REDIS_PORT" "$REDIS_PASSWORD" "$CONSUMER_USER" "$CONSUMER_PWD" "$APP_NAME" "$LIB_NAME" "$LEFT_EXPERIMENT_NAME" "$RIGHT_EXPERIMENT_NAME" "$LEFT_BATCH_PREFIX" "$RIGHT_BATCH_PREFIX" "$REPORT_PATH" <<'PY' > "$RESULT_FILE"
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
 app_name, lib_name, left_exp_name, right_exp_name, left_batch_prefix,
 right_batch_prefix, report_path) = sys.argv[1:]
redis_port = int(redis_port)

REAL_QUESTIONS = [
    {
        "testCaseNo": "RQ-01",
        "questionType": "真实业务问题",
        "userQuestion": "这个知识库现在主要能回答哪些类型的问题？",
        "expectedKnowledge": "概括知识库覆盖范围、主题边界和适合提问的问题类型。"
    },
    {
        "testCaseNo": "RQ-02",
        "questionType": "真实业务问题",
        "userQuestion": "如果我要开始做训练测试任务，第一步应该先做什么？",
        "expectedKnowledge": "给出训练测试任务的最小起步动作和准备顺序。"
    },
    {
        "testCaseNo": "RQ-03",
        "questionType": "真实业务问题",
        "userQuestion": "如果我要把这个流程真正走通，建议我按什么顺序做？",
        "expectedKnowledge": "给出文档接入、实验版本、发布、重建索引和跑测的先后顺序。"
    },
    {
        "testCaseNo": "RQ-04",
        "questionType": "真实业务问题",
        "userQuestion": "这个事情我该怎么弄？",
        "expectedKnowledge": "识别问题模糊，提示补充关键词，同时给出安全的最小起步建议。"
    },
    {
        "testCaseNo": "RQ-05",
        "questionType": "真实业务问题",
        "userQuestion": "请直接告诉我美国联邦所得税 2026 年怎么报。",
        "expectedKnowledge": "明确说明知识库无依据，避免强答，并给出下一步建议。"
    },
    {
        "testCaseNo": "RQ-06",
        "questionType": "真实业务问题",
        "userQuestion": "把训练测试任务真正走通的完整顺序、每一步为什么要做、以及如果知识没命中时怎么收尾，一次性讲完整。",
        "expectedKnowledge": "同时覆盖准备文档、上传到知识库、保存实验版本、发布当前生效版本、重建索引、绑定应用、运行默认问题集跑测、根据结果修补知识切分提示词、未命中时的体面反馈。"
    }
]

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

def require_preview_file(token, lib_id):
    files = extract_records(http_json("POST", "/consumer/uploadFile/queryPage", {
        "pageNow": 1,
        "pageSize": 100,
        "libId": lib_id
    }, token))
    if not files:
        raise RuntimeError(f"no upload file found in lib: {lib_id}")
    for item in files:
        if item.get("fileName") == "rag-mvp-seed.md":
            return item
    return files[0]

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

def run_real_batch(token, app_id, experiment, batch_prefix):
    publish_experiment(token, app_id_to_lib[app_id], experiment)
    batch_name = f"{datetime.now().strftime('%Y-%m-%d')} {batch_prefix}"
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

def category_stats(items):
    counter = Counter()
    for item in items:
        category = item.get("followUpCategory") or ""
        if category:
            counter[category] += 1
        elif item.get("followUpAction") or item.get("remark"):
            counter["other"] += 1
    return dict(counter)

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

def build_compare_markdown(left_batch, right_batch, left_exp, right_exp):
    left_items = left_batch.get("items") or []
    right_items = right_batch.get("items") or []
    left_metrics = metric_stats(left_items)
    right_metrics = metric_stats(right_items)
    left_categories = category_stats(left_items)
    right_categories = category_stats(right_items)

    lines = [
        "# RAG 真实问题集跨切分版本对比",
        "",
        f"- 生成时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        f"- 应用：{left_batch.get('appName') or '-'}",
        f"- 知识库：{left_batch.get('knowledgeScope') or '-'}",
        f"- 测试问题数：{len(REAL_QUESTIONS)}",
        "",
        "## 对比版本",
        "",
        f"- A 批次：{left_batch.get('batchName')}",
        f"  生效实验版本：{left_exp.get('versionName')} ({left_exp.get('splitStrategy')})",
        f"  批次 ID：{left_batch.get('id')}",
        f"- B 批次：{right_batch.get('batchName')}",
        f"  生效实验版本：{right_exp.get('versionName')} ({right_exp.get('splitStrategy')})",
        f"  批次 ID：{right_batch.get('id')}",
        "",
        "## 切分预览",
        "",
        f"- A chunk 总数：{left_exp.get('chunkCount')}",
        f"- B chunk 总数：{right_exp.get('chunkCount')}",
        "",
        "## 汇总对比",
        "",
        "| 维度 | A | B |",
        "| --- | --- | --- |",
        f"| 全部通过条目 | {left_batch.get('passCount', 0)} / {left_batch.get('itemCount', 0)} | {right_batch.get('passCount', 0)} / {right_batch.get('itemCount', 0)} |",
        f"| 待跟进条目 | {left_batch.get('followUpCount', 0)} | {right_batch.get('followUpCount', 0)} |",
        f"| 命中问题通过 | {left_metrics['hitPass']} | {right_metrics['hitPass']} |",
        f"| 可信通过 | {left_metrics['groundedPass']} | {right_metrics['groundedPass']} |",
        f"| 易懂通过 | {left_metrics['readablePass']} | {right_metrics['readablePass']} |",
        f"| 失败体面通过 | {left_metrics['gracefulPass']} | {right_metrics['gracefulPass']} |",
        "",
        "## 跟进分类对比",
        "",
        f"- A：{json.dumps(left_categories, ensure_ascii=False)}",
        f"- B：{json.dumps(right_categories, ensure_ascii=False)}",
        "",
        "## 逐题结果",
        "",
        "| 编号 | 问题 | A 结论 | B 结论 |",
        "| --- | --- | --- | --- |",
    ]
    right_by_case = {item.get("testCaseNo"): item for item in right_items}
    for left_item in left_items:
        right_item = right_by_case.get(left_item.get("testCaseNo"), {})
        left_text = "/".join([
            left_item.get("hitConclusion") or "-",
            left_item.get("groundedConclusion") or "-",
            left_item.get("readableConclusion") or "-",
            left_item.get("gracefulFailureConclusion") or "-"
        ])
        right_text = "/".join([
            right_item.get("hitConclusion") or "-",
            right_item.get("groundedConclusion") or "-",
            right_item.get("readableConclusion") or "-",
            right_item.get("gracefulFailureConclusion") or "-"
        ])
        lines.append(f"| {left_item.get('testCaseNo')} | {left_item.get('userQuestion')} | {left_text} | {right_text} |")

    lines.extend([
        "",
        "## 当前判断",
        "",
        "- 先看是否出现待跟进分类差异，如果差异集中在 `chunking`，优先回头修切分；如果集中在 `prompt`，优先修提示词；如果集中在 `knowledge`，优先补知识。",
        "- 若两轮都全通过，说明当前这组真实问题还不足以拉开差距，下一轮应补更长步骤型问题、更模糊问题或更容易断章取义的问题。",
    ])
    return "\n".join(lines)

token, user_status = ensure_login()
app, lib = require_app_and_lib(token)
preview_file = require_preview_file(token, lib["id"])
app_id_to_lib = {app["id"]: lib["id"]}
left_experiment = require_experiment(token, lib["id"], left_exp_name)
right_experiment = require_experiment(token, lib["id"], right_exp_name)
left_preview = preview_chunks(token, preview_file["id"], left_experiment)
right_preview = preview_chunks(token, preview_file["id"], right_experiment)
left_experiment["chunkCount"] = left_preview["chunkCount"]
left_experiment["previewChunks"] = left_preview["chunks"]
right_experiment["chunkCount"] = right_preview["chunkCount"]
right_experiment["previewChunks"] = right_preview["chunks"]
left_batch = run_real_batch(token, app["id"], left_experiment, left_batch_prefix)
right_batch = run_real_batch(token, app["id"], right_experiment, right_batch_prefix)
report_markdown = build_compare_markdown(left_batch, right_batch, left_experiment, right_experiment)

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
    "questions": REAL_QUESTIONS,
    "leftExperiment": {
        "id": left_experiment["id"],
        "versionName": left_experiment["versionName"],
        "splitStrategy": left_experiment["splitStrategy"],
        "chunkCount": left_preview["chunkCount"],
        "previewChunks": left_preview["chunks"],
    },
    "rightExperiment": {
        "id": right_experiment["id"],
        "versionName": right_experiment["versionName"],
        "splitStrategy": right_experiment["splitStrategy"],
        "chunkCount": right_preview["chunkCount"],
        "previewChunks": right_preview["chunks"],
    },
    "leftBatch": {
        "id": left_batch["id"],
        "batchName": left_batch["batchName"],
        "passCount": left_batch["passCount"],
        "itemCount": left_batch["itemCount"],
        "followUpCount": left_batch["followUpCount"],
        "summaryConclusion": left_batch.get("summaryConclusion"),
        "nextAction": left_batch.get("nextAction"),
        "items": [build_item_summary(item) for item in (left_batch.get("items") or [])]
    },
    "rightBatch": {
        "id": right_batch["id"],
        "batchName": right_batch["batchName"],
        "passCount": right_batch["passCount"],
        "itemCount": right_batch["itemCount"],
        "followUpCount": right_batch["followUpCount"],
        "summaryConclusion": right_batch.get("summaryConclusion"),
        "nextAction": right_batch.get("nextAction"),
        "items": [build_item_summary(item) for item in (right_batch.get("items") or [])]
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

print("RAG 真实问题集跨切分版本对比已执行")
print(f"- 目标环境: {result['baseUrl']}")
print(f"- 用户状态: {result['userStatus']} ({result['user']})")
print(f"- 应用: {result['app']['name']} #{result['app']['id']}")
print(f"- 知识库: {result['lib']['name']} #{result['lib']['id']}")
print(f"- A 切分预览 chunk 总数: {result['leftExperiment']['chunkCount']}")
print(f"- B 切分预览 chunk 总数: {result['rightExperiment']['chunkCount']}")
print(f"- A 批次 #{result['leftBatch']['id']}: {result['leftBatch']['batchName']} | {result['leftExperiment']['versionName']} ({result['leftExperiment']['splitStrategy']}) | items={result['leftBatch']['itemCount']} pass={result['leftBatch']['passCount']} followUp={result['leftBatch']['followUpCount']}")
print(f"- B 批次 #{result['rightBatch']['id']}: {result['rightBatch']['batchName']} | {result['rightExperiment']['versionName']} ({result['rightExperiment']['splitStrategy']}) | items={result['rightBatch']['itemCount']} pass={result['rightBatch']['passCount']} followUp={result['rightBatch']['followUpCount']}")
PY

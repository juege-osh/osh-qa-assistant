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
LEFT_EXPERIMENT_NAME="${LEFT_EXPERIMENT_NAME:-脚本语义切分版}"
RIGHT_EXPERIMENT_NAME="${RIGHT_EXPERIMENT_NAME:-脚本Token切分版}"
LEFT_BATCH_PREFIX="${LEFT_BATCH_PREFIX:-真实问题集正式验收-语义切分}"
RIGHT_BATCH_PREFIX="${RIGHT_BATCH_PREFIX:-真实问题集正式验收-Token切分}"
REPORT_PATH="${REPORT_PATH:-}"
QUESTIONS_FILE="${QUESTIONS_FILE:-$ROOT_DIR/scripts/rag-mvp-real-questions.json}"
QUESTION_FILTER="${QUESTION_FILTER:-}"
RUN_BATCH_REQUEST_TIMEOUT_SECONDS="${RUN_BATCH_REQUEST_TIMEOUT_SECONDS:-30}"
BATCH_POLL_WAIT_SECONDS="${BATCH_POLL_WAIT_SECONDS:-600}"
BATCH_POLL_INTERVAL_SECONDS="${BATCH_POLL_INTERVAL_SECONDS:-5}"
HTTP_RETRY_ATTEMPTS="${HTTP_RETRY_ATTEMPTS:-4}"
HTTP_RETRY_DELAY_SECONDS="${HTTP_RETRY_DELAY_SECONDS:-3}"

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

python3 - "$BASE_URL" "$REDIS_HOST" "$REDIS_PORT" "$REDIS_PASSWORD" "$CONSUMER_USER" "$CONSUMER_PWD" "$APP_NAME" "$LIB_NAME" "$LEFT_EXPERIMENT_NAME" "$RIGHT_EXPERIMENT_NAME" "$LEFT_BATCH_PREFIX" "$RIGHT_BATCH_PREFIX" "$REPORT_PATH" "$QUESTIONS_FILE" "$QUESTION_FILTER" "$RUN_BATCH_REQUEST_TIMEOUT_SECONDS" "$BATCH_POLL_WAIT_SECONDS" "$BATCH_POLL_INTERVAL_SECONDS" "$HTTP_RETRY_ATTEMPTS" "$HTTP_RETRY_DELAY_SECONDS" <<'PY' > "$RESULT_FILE"
import json
import socket
import sys
import time
import urllib.error
import urllib.request
from collections import Counter
from datetime import datetime
from pathlib import Path
from urllib.parse import urljoin

(base_url, redis_host, redis_port, redis_password, consumer_user, consumer_pwd,
 app_name, lib_name, left_exp_name, right_exp_name, left_batch_prefix,
 right_batch_prefix, report_path, questions_file, question_filter,
 run_batch_request_timeout_seconds, batch_poll_wait_seconds,
 batch_poll_interval_seconds, http_retry_attempts,
 http_retry_delay_seconds) = sys.argv[1:]
redis_port = int(redis_port)
run_batch_request_timeout_seconds = int(run_batch_request_timeout_seconds)
batch_poll_wait_seconds = int(batch_poll_wait_seconds)
batch_poll_interval_seconds = int(batch_poll_interval_seconds)
http_retry_attempts = int(http_retry_attempts)
http_retry_delay_seconds = int(http_retry_delay_seconds)

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

def http_json(method, path, data=None, token=None, timeout_seconds=180):
    req = urllib.request.Request(urljoin(base_url, path), method=method)
    if data is not None:
        req.add_header("Content-Type", "application/json")
        body = json.dumps(data).encode()
    else:
        body = None
    if token:
        req.add_header("Authorization", token)
    try:
        with urllib.request.urlopen(req, data=body, timeout=timeout_seconds) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as exc:
        payload = exc.read().decode()
        raise RuntimeError(f"http error {exc.code} {path}: {payload}") from exc

def with_retry(label, func, retryable_result=None, attempts=None, base_delay_seconds=None):
    attempts = attempts or http_retry_attempts
    base_delay_seconds = base_delay_seconds or http_retry_delay_seconds
    last_error = None
    for attempt in range(1, attempts + 1):
        try:
            result = func()
            if retryable_result and retryable_result(result):
                last_error = RuntimeError(f"{label} returned retryable result: {result}")
            else:
                return result
        except (TimeoutError, socket.timeout, urllib.error.URLError, RuntimeError) as exc:
            last_error = exc
        if attempt < attempts:
            time.sleep(base_delay_seconds * attempt)
    raise RuntimeError(f"{label} failed after {attempts} attempts: {last_error}") from last_error

def list_rag_batches(token):
    def fetch():
        resp = http_json("GET", "/consumer/ragAcceptance/listMine", token=token)
        if resp.get("code") != 200:
            raise RuntimeError(f"list rag batches failed: {resp}")
        return resp
    resp = with_retry(
        "listMine",
        fetch,
        retryable_result=lambda payload: payload.get("code") == 90000,
    )
    return resp.get("data") or []

def load_batch_detail(token, batch_id):
    def fetch():
        detail_resp = http_json("GET", f"/consumer/ragAcceptance/detail?id={batch_id}", token=token)
        if detail_resp.get("code") != 200:
            raise RuntimeError(f"load batch detail failed: {detail_resp}")
        return detail_resp
    detail_resp = with_retry(
        f"detail({batch_id})",
        fetch,
        retryable_result=lambda payload: payload.get("code") == 90000,
    )
    return detail_resp["data"]

def is_batch_running(detail, expect_count=None):
    item_count = detail.get("itemCount") or 0
    if expect_count and item_count >= expect_count:
        return False
    summary = (detail.get("summaryConclusion") or "").strip()
    next_action = (detail.get("nextAction") or "").strip()
    return "运行中" in summary or "仍在执行中" in next_action

def ensure_batch_completed(detail, batch_name, expect_count=None):
    item_count = detail.get("itemCount") or 0
    if is_batch_running(detail, expect_count):
        raise TimeoutError(
            f"batch still running after polling: {batch_name}, batchId={detail.get('id')}, "
            f"itemCount={item_count}, summary={detail.get('summaryConclusion')}"
        )
    if expect_count and item_count != expect_count:
        raise RuntimeError(
            f"batch finished without full item count: {batch_name}, batchId={detail.get('id')}, "
            f"itemCount={item_count}/{expect_count}, summary={detail.get('summaryConclusion')}"
        )
    return detail

def find_recent_batch_by_name(token, batch_name, existing_ids=None, expect_count=None, wait_seconds=None, poll_interval=None):
    existing_ids = set(existing_ids or [])
    wait_seconds = wait_seconds or batch_poll_wait_seconds
    poll_interval = poll_interval or batch_poll_interval_seconds
    deadline = time.time() + wait_seconds
    batch_id = None
    last_running_detail = None
    while time.time() < deadline:
        if batch_id is None:
            batches = list_rag_batches(token)
            for batch in batches:
                if batch.get("batchName") != batch_name:
                    continue
                if batch.get("id") in existing_ids:
                    continue
                batch_id = batch.get("id")
                break
            if batch_id is None:
                time.sleep(poll_interval)
                continue
        detail = load_batch_detail(token, batch_id)
        if not is_batch_running(detail, expect_count):
            return detail
        last_running_detail = detail
        time.sleep(poll_interval)
    if last_running_detail is not None:
        raise TimeoutError(
            f"batch still running after {wait_seconds}s: {batch_name}, "
            f"batchId={last_running_detail.get('id')}, itemCount={last_running_detail.get('itemCount')}, "
            f"summary={last_running_detail.get('summaryConclusion')}"
        )
    raise TimeoutError(f"batch not visible after fallback polling: {batch_name}")

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
    try:
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
            "chunks": data.get("chunks") or [],
            "warning": ""
        }
    except Exception as exc:
        return {
            "chunkCount": None,
            "chunks": [],
            "warning": f"preview unavailable: {exc}"
        }

def run_real_batch(token, app_id, experiment, batch_prefix):
    publish_experiment(token, app_id_to_lib[app_id], experiment)
    batch_name = f"{datetime.now().strftime('%Y-%m-%d')} {batch_prefix}"
    existing_ids = {
        item.get("id")
        for item in list_rag_batches(token)
        if item.get("batchName") == batch_name
    }
    payload = {
        "appId": app_id,
        "batchName": batch_name,
        "sceneType": "真实业务问题",
        "testerName": "codex",
        "summaryConclusion": "",
        "nextAction": "",
        "questions": REAL_QUESTIONS
    }
    try:
        run_resp = http_json(
            "POST",
            "/consumer/ragAcceptance/runBatch",
            payload,
            token=token,
            timeout_seconds=run_batch_request_timeout_seconds
        )
    except (TimeoutError, socket.timeout, urllib.error.URLError):
        detail = find_recent_batch_by_name(token, batch_name, existing_ids, len(REAL_QUESTIONS))
        return ensure_batch_completed(detail, batch_name, len(REAL_QUESTIONS))
    if run_resp.get("code") != 200:
        raise RuntimeError(f"run real batch failed: {run_resp}")
    batch_id = run_resp["data"]
    detail = load_batch_detail(token, batch_id)
    return ensure_batch_completed(detail, batch_name, len(REAL_QUESTIONS))

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
        f"- A chunk 总数：{left_exp.get('chunkCount') if left_exp.get('chunkCount') is not None else '预览失败'}",
        f"- B chunk 总数：{right_exp.get('chunkCount') if right_exp.get('chunkCount') is not None else '预览失败'}",
        "",
    ]
    if left_exp.get("previewWarning"):
        lines.append(f"- A 预览告警：{left_exp.get('previewWarning')}")
    if right_exp.get("previewWarning"):
        lines.append(f"- B 预览告警：{right_exp.get('previewWarning')}")
    lines.extend([
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
    ])
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
left_experiment["previewWarning"] = left_preview.get("warning") or ""
right_experiment["chunkCount"] = right_preview["chunkCount"]
right_experiment["previewChunks"] = right_preview["chunks"]
right_experiment["previewWarning"] = right_preview.get("warning") or ""
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
        "previewWarning": left_preview.get("warning") or "",
    },
    "rightExperiment": {
        "id": right_experiment["id"],
        "versionName": right_experiment["versionName"],
        "splitStrategy": right_experiment["splitStrategy"],
        "chunkCount": right_preview["chunkCount"],
        "previewChunks": right_preview["chunks"],
        "previewWarning": right_preview.get("warning") or "",
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
if result['leftExperiment'].get('previewWarning'):
    print(f"- A 切分预览告警: {result['leftExperiment']['previewWarning']}")
if result['rightExperiment'].get('previewWarning'):
    print(f"- B 切分预览告警: {result['rightExperiment']['previewWarning']}")
print(f"- A 批次 #{result['leftBatch']['id']}: {result['leftBatch']['batchName']} | {result['leftExperiment']['versionName']} ({result['leftExperiment']['splitStrategy']}) | items={result['leftBatch']['itemCount']} pass={result['leftBatch']['passCount']} followUp={result['leftBatch']['followUpCount']}")
print(f"- B 批次 #{result['rightBatch']['id']}: {result['rightBatch']['batchName']} | {result['rightExperiment']['versionName']} ({result['rightExperiment']['splitStrategy']}) | items={result['rightBatch']['itemCount']} pass={result['rightBatch']['passCount']} followUp={result['rightBatch']['followUpCount']}")
PY

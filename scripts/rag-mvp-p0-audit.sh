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
RESULT_FILE="$TMP_DIR/rag-mvp-p0-audit.json"
trap 'rm -rf "$TMP_DIR"' EXIT

python3 - "$BASE_URL" "$REDIS_HOST" "$REDIS_PORT" "$REDIS_PASSWORD" "$CONSUMER_USER" "$CONSUMER_PWD" "$APP_NAME" "$LIB_NAME" "$LEFT_EXPERIMENT_NAME" "$RIGHT_EXPERIMENT_NAME" "$REPORT_PATH" <<'PY' > "$RESULT_FILE"
import json
import os
import socket
import sys
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import urljoin

(base_url, redis_host, redis_port, redis_password, consumer_user, consumer_pwd,
 app_name, lib_name, left_exp_name, right_exp_name, report_path) = sys.argv[1:]
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
    return app, lib

def find_latest_batch(token, app_id, batch_name_prefix):
    batches = http_json("GET", "/consumer/ragAcceptance/listMine", token=token).get("data") or []
    matched = [item for item in batches if item.get("appId") == app_id and str(item.get("batchName") or "").startswith(batch_name_prefix)]
    if not matched:
        return None
    matched.sort(key=lambda item: int(item.get("id")), reverse=True)
    return matched[0]

def summarize_batch(batch):
    if batch is None:
        return {
            "status": "MISSING",
            "message": "未找到对应批次"
        }
    return {
        "status": "PASS" if batch.get("passCount") == batch.get("itemCount") else "FOLLOW_UP",
        "id": batch.get("id"),
        "batchName": batch.get("batchName"),
        "passCount": batch.get("passCount"),
        "itemCount": batch.get("itemCount"),
        "followUpCount": batch.get("followUpCount"),
        "activeExperimentName": batch.get("activeExperimentName"),
        "activeSplitStrategy": batch.get("activeSplitStrategy"),
        "summaryConclusion": batch.get("summaryConclusion"),
    }

token, user_status = ensure_login()
app, lib = require_app_and_lib(token)
alert_readiness_resp = http_json("GET", "/consumer/ops/alertReadiness", token=token)
if alert_readiness_resp.get("code") != 200:
    raise RuntimeError(f"load alert readiness failed: {alert_readiness_resp}")
alert_readiness = alert_readiness_resp.get("data") or {}

app_detail = http_json("GET", f"/consumer/app/queryById?id={app['id']}", token=token).get("data") or {}

compare_left = find_latest_batch(token, app["id"], "2026-06-30 真实问题集正式验收-语义切分")
compare_right = find_latest_batch(token, app["id"], "2026-06-30 真实问题集正式验收-Token切分")
repair_batch = find_latest_batch(token, app["id"], "2026-06-30 真实问题集正式验收-补知识复跑")

checks = [
    {
        "name": "正式验收批次",
        "result": summarize_batch(repair_batch),
        "requirement": "真实问题集正式验收已落库且当前批次全部通过"
    },
    {
        "name": "跨切分版本对比",
        "result": {
            "left": summarize_batch(compare_left),
            "right": summarize_batch(compare_right),
            "status": "PASS" if compare_left and compare_right else "MISSING"
        },
        "requirement": "语义切分版和 Token 切分版都已真实运行并留痕"
    },
    {
        "name": "生效切分版本",
        "result": {
            "status": "PASS" if lib.get("activeExperimentId") and lib.get("activeSplitStrategy") else "MISSING",
            "activeExperimentId": lib.get("activeExperimentId"),
            "activeExperimentName": lib.get("activeExperimentName"),
            "activeSplitStrategy": lib.get("activeSplitStrategy")
        },
        "requirement": "知识库存在当前生效实验版本和切分策略快照"
    },
    {
        "name": "Prompt/模型开关",
        "result": {
            "status": "PASS" if "customPrompt" in app_detail and "chatModel" in app_detail else "MISSING",
            "customPromptConfigured": bool(app_detail.get("customPrompt")),
            "chatModelConfigured": bool(app_detail.get("chatModel")),
            "chatModel": app_detail.get("chatModel") or ""
        },
        "requirement": "应用支持 Prompt 和模型配置入口，并可查询当前配置"
    },
    {
        "name": "告警 readiness",
        "result": alert_readiness,
        "requirement": "最小故障告警链路能明确区分已就绪、缺配置或未开启"
    }
]

summary_lines = []
for item in checks:
    result = item["result"]
    if isinstance(result, dict):
        status = result.get("status", "UNKNOWN")
    else:
        status = "UNKNOWN"
    summary_lines.append(f"- {item['name']}: {status}")

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
        "activeExperimentId": lib.get("activeExperimentId"),
        "activeExperimentName": lib.get("activeExperimentName"),
        "activeSplitStrategy": lib.get("activeSplitStrategy"),
    },
    "checks": checks,
    "summaryMarkdown": "# RAG MVP P0 审计结果\n\n" + "\n".join(summary_lines)
}

if report_path:
    Path(report_path).write_text(result["summaryMarkdown"], encoding="utf-8")

print(json.dumps(result, ensure_ascii=False))
PY

python3 - "$RESULT_FILE" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as f:
    result = json.load(f)

print("RAG MVP P0 审计已执行")
print(f"- 目标环境: {result['baseUrl']}")
print(f"- 用户状态: {result['userStatus']} ({result['user']})")
print(f"- 应用: {result['app']['name']} #{result['app']['id']}")
print(f"- 知识库: {result['lib']['name']} #{result['lib']['id']}")
for item in result["checks"]:
    check_result = item["result"]
    status = check_result.get("status", "UNKNOWN") if isinstance(check_result, dict) else "UNKNOWN"
    print(f"- {item['name']}: {status}")
PY

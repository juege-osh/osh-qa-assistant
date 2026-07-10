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

BASE_URL="${BASE_URL:-http://127.0.0.1:19088}"
REDIS_HOST="${REDIS_HOST:-${AI_ASSISTANT_REDIS_HOST:-127.0.0.1}}"
REDIS_PORT="${REDIS_PORT:-${AI_ASSISTANT_REDIS_PORT:-6379}}"
REDIS_PASSWORD="${REDIS_PASSWORD:-${AI_ASSISTANT_REDIS_PASSWORD:-}}"
CONSUMER_USER="${CONSUMER_USER:-course_demo_user}"
CONSUMER_PWD="${CONSUMER_PWD:-123456}"
APP_NAME="${APP_NAME:-RAG MVP 验收助手}"
APP_ID="${APP_ID:-}"
PUBLIC_PASSWORD="${PUBLIC_PASSWORD:-codex-public-123}"
PUBLIC_VISITOR_ID="${PUBLIC_VISITOR_ID:-codex-verify-visitor}"
PUBLIC_PROMPT_PUBLIC="${PUBLIC_PROMPT_PUBLIC:-请用一句话说明这个公开应用现在是否可用}"
PUBLIC_PROMPT_PASSWORD="${PUBLIC_PROMPT_PASSWORD:-请再用一句话确认密码访问模式也已经打通}"
PUBLIC_PROMPT_HIGH_RISK="${PUBLIC_PROMPT_HIGH_RISK:-如果我现在追问一个知识库范围外的高风险问题，比如 2026 年美国联邦所得税怎么报，你会怎么处理？}"
EXPECT_REFERENCE_BLOCK="${EXPECT_REFERENCE_BLOCK:-1}"
EXPECT_GRACEFUL_FAILURE="${EXPECT_GRACEFUL_FAILURE:-1}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd python3

TMP_DIR="$(mktemp -d)"
RESULT_FILE="$TMP_DIR/public-app-acceptance.json"
trap 'rm -rf "$TMP_DIR"' EXIT

python3 - "$BASE_URL" "$REDIS_HOST" "$REDIS_PORT" "$REDIS_PASSWORD" "$CONSUMER_USER" "$CONSUMER_PWD" "$APP_NAME" "$APP_ID" "$PUBLIC_PASSWORD" "$PUBLIC_VISITOR_ID" "$PUBLIC_PROMPT_PUBLIC" "$PUBLIC_PROMPT_PASSWORD" "$PUBLIC_PROMPT_HIGH_RISK" <<'PY' > "$RESULT_FILE"
import json
import socket
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import urljoin

(base_url, redis_host, redis_port, redis_password, consumer_user, consumer_pwd,
 app_name, app_id, public_password, public_visitor_id, public_prompt_public,
 public_prompt_password, public_prompt_high_risk) = sys.argv[1:]

redis_port = int(redis_port)
app_id = app_id.strip()

STREAM_END = "[DONE]"


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


def open_request(req, body=None, timeout=60):
    try:
        return urllib.request.urlopen(req, data=body, timeout=timeout)
    except urllib.error.HTTPError as exc:
        return exc


def http_json(method, path, data=None, token=None, timeout=60):
    req = urllib.request.Request(urljoin(base_url, path), method=method)
    body = None
    if data is not None:
        req.add_header("Content-Type", "application/json")
        body = json.dumps(data).encode()
    if token:
        req.add_header("Authorization", token)
    resp = open_request(req, body=body, timeout=timeout)
    payload = resp.read().decode()
    try:
        return json.loads(payload)
    except Exception as exc:
        raise RuntimeError(f"non-json response for {path}: {payload}") from exc


def extract_records(resp):
    data = resp.get("data")
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        records = data.get("records")
        if isinstance(records, list):
            return records
    return []


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
    raise RuntimeError(f"login failed: {result}")


def require_app(token):
    resp = http_json("POST", "/consumer/app/queryPage", {"pageNow": 1, "pageSize": 100}, token=token)
    apps = extract_records(resp)
    if app_id:
        for app in apps:
            if str(app.get("id")) == app_id:
                return app
        raise RuntimeError(f"app not found by id: {app_id}")
    for app in apps:
        if app.get("appName") == app_name:
            return app
    if len(apps) == 1:
        return apps[0]
    raise RuntimeError(f"app not found: {app_name}")


def query_publish_config(token, current_app_id):
    resp = http_json("GET", f"/consumer/app/publishConfig/queryByAppId?id={current_app_id}", token=token)
    if resp.get("code") != 200:
        raise RuntimeError(f"query publish config failed: {resp}")
    return resp.get("data") or {}


def save_publish_config(token, payload):
    resp = http_json("POST", "/consumer/app/publishConfig/save", payload, token=token)
    if resp.get("code") != 200:
        raise RuntimeError(f"save publish config failed: {resp}")
    return resp


def load_public_detail(slug):
    return http_json("GET", f"/consumer/public/app/detail?slug={slug}")


def verify_public_password(slug, access_password):
    return http_json("POST", "/consumer/public/app/verifyPassword", {
        "slug": slug,
        "accessPassword": access_password,
    })


def parse_sse_frame(frame):
    lines = []
    for raw_line in frame.splitlines():
        if raw_line.startswith("data:"):
            lines.append(raw_line[5:].lstrip())
    return "\n".join(lines)


def consume_public_chat(payload):
    req = urllib.request.Request(
        urljoin(base_url, "/consumer/public/app/chat"),
        method="POST",
        headers={"Content-Type": "application/json"},
    )
    body = json.dumps(payload).encode()
    resp = open_request(req, body=body, timeout=180)
    content_type = resp.headers.get("content-type", "")
    if "application/json" in content_type:
        parsed = json.loads(resp.read().decode())
        raise RuntimeError(parsed.get("msg") or f"public chat failed: {parsed}")
    if resp.status >= 400:
        raise RuntimeError(f"public chat http error: {resp.status}")
    text = ""
    buffer = ""
    while True:
        chunk = resp.read(4096)
        if not chunk:
            break
        buffer += chunk.decode("utf-8", errors="ignore")
        frames = buffer.split("\n\n")
        buffer = frames.pop() or ""
        for frame in frames:
            data = parse_sse_frame(frame)
            if not data:
                continue
            if data == STREAM_END:
                return text
            text += data
    tail = parse_sse_frame(buffer)
    if tail and tail != STREAM_END:
        text += tail
    return text


def query_invoke_records(token, keyword):
    resp = http_json("POST", "/consumer/invokeRecord/queryPage", {
        "pageNow": 1,
        "pageSize": 20,
        "appName": app.get("appName"),
        "userInputKeyword": keyword,
    }, token=token)
    if resp.get("code") != 200:
        raise RuntimeError(f"query invoke record failed: {resp}")
    return extract_records(resp)


def find_trace_record(records, visitor_short, keyword):
    for record in records:
        username = str(record.get("username") or "")
        if visitor_short not in username:
            continue
        for detail in record.get("detailList") or []:
            if keyword in str(detail.get("userInput") or ""):
                return record, detail
    return None, None


token, user_status = ensure_login()
app = require_app(token)
current_app_id = int(app["id"])
original_config = query_publish_config(token, current_app_id)
slug = str(original_config.get("slug") or f"app-{current_app_id}").strip().lower()
restore_payload = {
    "appId": current_app_id,
    "enabled": int(original_config.get("enabled") or 0),
    "slug": slug,
    "accessType": str(original_config.get("accessType") or "PUBLIC"),
    "accessPassword": "",
}

public_result = {}
password_result = {}
restore_result = {"attempted": False, "restoredTo": restore_payload}
visitor_short = public_visitor_id[:8]

try:
    public_save = save_publish_config(token, {
        "appId": current_app_id,
        "enabled": 1,
        "slug": slug,
        "accessType": "PUBLIC",
        "accessPassword": "",
    })
    public_detail = load_public_detail(slug)
    public_verify = verify_public_password(slug, public_password)
    public_chat_text = consume_public_chat({
        "slug": slug,
        "visitorId": public_visitor_id,
        "userInput": public_prompt_public,
    })
    graceful_failure_text = consume_public_chat({
        "slug": slug,
        "visitorId": public_visitor_id,
        "userInput": public_prompt_high_risk,
    })
    time.sleep(2)
    public_records = query_invoke_records(token, public_prompt_public)
    public_record, public_detail_record = find_trace_record(public_records, visitor_short, public_prompt_public)
    public_result = {
        "save": public_save,
        "detail": public_detail,
        "verifyPassword": public_verify,
        "chatText": public_chat_text,
        "gracefulFailureText": graceful_failure_text,
        "invokeRecordId": public_record.get("id") if public_record else None,
        "invokeRecordUsername": public_record.get("username") if public_record else None,
        "invokeRecordDetailId": public_detail_record.get("id") if public_detail_record else None,
    }

    original_password_mode = (
        str(original_config.get("accessType") or "PUBLIC") == "PASSWORD"
        and bool(original_config.get("hasPassword"))
    )
    if not original_password_mode:
        password_save = save_publish_config(token, {
            "appId": current_app_id,
            "enabled": 1,
            "slug": slug,
            "accessType": "PASSWORD",
            "accessPassword": public_password,
        })
        password_detail = load_public_detail(slug)
        password_wrong = verify_public_password(slug, public_password + "-wrong")
        password_right = verify_public_password(slug, public_password)
        access_token = str((password_right.get("data") or {}).get("accessToken") or "")
        if not access_token:
            raise RuntimeError(f"password mode did not return access token: {password_right}")
        password_chat_text = consume_public_chat({
            "slug": slug,
            "visitorId": public_visitor_id,
            "userInput": public_prompt_password,
            "accessToken": access_token,
        })
        time.sleep(2)
        password_records = query_invoke_records(token, public_prompt_password)
        password_record, password_detail_record = find_trace_record(password_records, visitor_short, public_prompt_password)
        password_result = {
            "save": password_save,
            "detail": password_detail,
            "verifyWrong": password_wrong,
            "verifyRight": password_right,
            "chatText": password_chat_text,
            "invokeRecordId": password_record.get("id") if password_record else None,
            "invokeRecordUsername": password_record.get("username") if password_record else None,
            "invokeRecordDetailId": password_detail_record.get("id") if password_detail_record else None,
        }
    else:
        password_result = {
            "skipped": True,
            "reason": "original config is PASSWORD with existing secret; skip destructive mutation",
        }
finally:
    restore_result["attempted"] = True
    if restore_payload["accessType"] == "PASSWORD" and bool(original_config.get("hasPassword")):
        restore_result["skipped"] = True
        restore_result["reason"] = "original config uses PASSWORD; cannot restore unknown existing password safely"
    else:
        save_publish_config(token, restore_payload)

print(json.dumps({
    "baseUrl": base_url,
    "userStatus": user_status,
    "consumerUser": consumer_user,
    "appId": current_app_id,
    "appName": app.get("appName"),
    "slug": slug,
    "publicVisitorId": public_visitor_id,
    "publicVisitorShort": visitor_short,
    "originalConfig": original_config,
    "publicMode": public_result,
    "passwordMode": password_result,
    "restore": restore_result,
}, ensure_ascii=False))
PY

python3 - "$RESULT_FILE" "$EXPECT_REFERENCE_BLOCK" "$EXPECT_GRACEFUL_FAILURE" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as f:
    result = json.load(f)

expect_reference_block = sys.argv[2] == "1"
expect_graceful_failure = sys.argv[3] == "1"
public_mode = result["publicMode"]
password_mode = result["passwordMode"]

def contains_any(text, tokens):
    normalized = str(text or "").lower()
    return any(token.lower() in normalized for token in tokens)

failures = []

print("公开应用最小验收已执行")
print(f"- 目标环境: {result['baseUrl']}")
print(f"- 用户状态: {result['userStatus']} ({result['consumerUser']})")
print(f"- 应用: {result['appName']} #{result['appId']}")
print(f"- slug: {result['slug']}")
print(f"- PUBLIC detail: code={public_mode['detail'].get('code')} passwordRequired={public_mode['detail'].get('data', {}).get('passwordRequired')}")
print(f"- PUBLIC verifyPassword: code={public_mode['verifyPassword'].get('code')} msg={public_mode['verifyPassword'].get('msg')}")
print(f"- PUBLIC chat trace: invokeRecordId={public_mode.get('invokeRecordId')} username={public_mode.get('invokeRecordUsername')}")
print(f"- PUBLIC graceful failure preview: {str(public_mode.get('gracefulFailureText') or '')[:80]}")

if public_mode["detail"].get("code") != 200:
    failures.append("PUBLIC detail 未返回 code=200")
if public_mode["detail"].get("data", {}).get("passwordRequired") is not False:
    failures.append("PUBLIC detail 未返回 passwordRequired=false")
if public_mode["verifyPassword"].get("code") != 30000:
    failures.append("PUBLIC 模式 verifyPassword 未返回预期业务提示")
if not public_mode.get("invokeRecordId"):
    failures.append("PUBLIC 模式未查到调用记录留痕")
if "公开访客:" not in str(public_mode.get("invokeRecordUsername") or ""):
    failures.append("PUBLIC 模式调用记录未标记公开访客")
if expect_reference_block and "参考来源" not in str(public_mode.get("chatText") or ""):
    failures.append("PUBLIC 模式回答未带出参考来源")

graceful_text = str(public_mode.get("gracefulFailureText") or "")
if expect_graceful_failure:
    if not contains_any(graceful_text, ["没有足够依据", "没有直接依据", "高风险", "联系对应专业负责人", "制度名称", "流程来源"]):
        failures.append("高风险超范围问题未返回体面拒答文案")
    if contains_any(graceful_text, ["Exception", "Traceback", "java.lang", "org.springframework"]):
        failures.append("高风险超范围问题暴露了技术异常细节")

if password_mode.get("skipped"):
    print(f"- PASSWORD mode: skipped ({password_mode.get('reason')})")
else:
    print(f"- PASSWORD detail: code={password_mode['detail'].get('code')} passwordRequired={password_mode['detail'].get('data', {}).get('passwordRequired')}")
    print(f"- PASSWORD wrong password: code={password_mode['verifyWrong'].get('code')} msg={password_mode['verifyWrong'].get('msg')}")
    print(f"- PASSWORD right password: code={password_mode['verifyRight'].get('code')} expireSeconds={password_mode['verifyRight'].get('data', {}).get('expireSeconds')}")
    print(f"- PASSWORD chat trace: invokeRecordId={password_mode.get('invokeRecordId')} username={password_mode.get('invokeRecordUsername')}")
    if password_mode["detail"].get("code") != 200:
        failures.append("PASSWORD detail 未返回 code=200")
    if password_mode["detail"].get("data", {}).get("passwordRequired") is not True:
        failures.append("PASSWORD detail 未返回 passwordRequired=true")
    if password_mode["verifyWrong"].get("code") != 30000:
        failures.append("PASSWORD 错误密码未返回预期业务提示")
    if password_mode["verifyRight"].get("code") != 200:
        failures.append("PASSWORD 正确密码未返回 code=200")
    if not password_mode.get("invokeRecordId"):
        failures.append("PASSWORD 模式未查到调用记录留痕")
    if "公开访客:" not in str(password_mode.get("invokeRecordUsername") or ""):
        failures.append("PASSWORD 模式调用记录未标记公开访客")

if result["restore"].get("skipped"):
    print(f"- 配置恢复: skipped ({result['restore'].get('reason')})")
else:
    print("- 配置恢复: done")

if failures:
    raise SystemExit("公开应用最小验收未通过:\n- " + "\n- ".join(failures))
PY

#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:9000}"
MANAGER_USER="${MANAGER_USER:-admin}"
MANAGER_PWD="${MANAGER_PWD:-123456}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
CONSUMER_USER="${CONSUMER_USER:-codex_smoke_$(date +%s)}"
CONSUMER_PWD="${CONSUMER_PWD:-123456}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd curl
need_cmd python3
need_cmd redis-cli

json_get() {
  local file="$1"
  local expr="$2"
  python3 - "$file" "$expr" <<'PY'
import json, sys
file_path, expr = sys.argv[1], sys.argv[2]
with open(file_path, "r", encoding="utf-8") as f:
    data = json.load(f)
value = data
for part in expr.split("."):
    if part:
        value = value[part]
if isinstance(value, (dict, list)):
    print(json.dumps(value, ensure_ascii=False))
else:
    print(value)
PY
}

read_captcha_code() {
  local captcha_id="$1"
  local raw
  raw="$(REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" --raw GET "$captcha_id" | tr -d '\r\n')"
  python3 - "$raw" <<'PY'
import json, sys
raw = sys.argv[1]
if raw == "":
    print("")
elif raw.startswith("NOAUTH ") or raw.startswith("WRONGPASS "):
    raise SystemExit(raw)
elif len(raw) >= 2 and raw[0] == '"' and raw[-1] == '"':
    print(json.loads(raw))
else:
    print(raw)
PY
}

assert_success() {
  local file="$1"
  local label="$2"
  python3 - "$file" "$label" <<'PY'
import json, sys
file_path, label = sys.argv[1], sys.argv[2]
with open(file_path, "r", encoding="utf-8") as f:
    data = json.load(f)
if data.get("code") != 200:
    raise SystemExit(f"{label} failed: {data.get('msg')}")
PY
}

ensure_consumer_user() {
  local code_file login_file register_file
  code_file="$(mktemp)"
  login_file="$(mktemp)"
  register_file="$(mktemp)"
  trap 'rm -f "$code_file" "$login_file" "$register_file"' RETURN

  curl -fsS "${BASE_URL}/consumer/user/getCode" > "$code_file"
  local captcha_id captcha_code
  captcha_id="$(json_get "$code_file" "data.captchaId")"
  captcha_code="$(read_captcha_code "$captcha_id")"
  if [[ -z "$captcha_code" ]]; then
    echo "无法从 Redis 读取 consumer 验证码，captchaId=${captcha_id}" >&2
    exit 1
  fi

  curl -sS -X POST "${BASE_URL}/consumer/user/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${CONSUMER_USER}\",\"pwd\":\"${CONSUMER_PWD}\",\"captchaId\":\"${captcha_id}\",\"code\":\"${captcha_code}\"}" \
    > "$login_file"

  local login_code
  login_code="$(json_get "$login_file" "code")"
  if [[ "$login_code" == "200" ]]; then
    return
  fi

  curl -fsS "${BASE_URL}/consumer/user/getCode" > "$code_file"
  captcha_id="$(json_get "$code_file" "data.captchaId")"
  captcha_code="$(read_captcha_code "$captcha_id")"
  if [[ -z "$captcha_code" ]]; then
    echo "无法从 Redis 读取 consumer 注册验证码，captchaId=${captcha_id}" >&2
    exit 1
  fi

  curl -sS -X POST "${BASE_URL}/consumer/user/register" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${CONSUMER_USER}\",\"pwd\":\"${CONSUMER_PWD}\",\"captchaId\":\"${captcha_id}\",\"code\":\"${captcha_code}\"}" \
    > "$register_file"
  assert_success "$register_file" "consumer register"
}

manager_code_file="$(mktemp)"
consumer_code_file="$(mktemp)"
manager_login_file="$(mktemp)"
consumer_login_file="$(mktemp)"
trap 'rm -f "$manager_code_file" "$consumer_code_file" "$manager_login_file" "$consumer_login_file"' EXIT

echo "[1/6] health"
curl -fsS "${BASE_URL}/" >/dev/null

echo "[2/6] manager getCode"
curl -fsS "${BASE_URL}/manager/manager/getCode" > "$manager_code_file"
manager_captcha_id="$(json_get "$manager_code_file" "data.captchaId")"
manager_captcha_code="$(read_captcha_code "$manager_captcha_id")"
if [[ -z "$manager_captcha_code" ]]; then
  echo "无法从 Redis 读取 manager 验证码，captchaId=${manager_captcha_id}" >&2
  exit 1
fi

echo "[3/6] consumer getCode"
ensure_consumer_user
curl -fsS "${BASE_URL}/consumer/user/getCode" > "$consumer_code_file"
consumer_captcha_id="$(json_get "$consumer_code_file" "data.captchaId")"
consumer_captcha_code="$(read_captcha_code "$consumer_captcha_id")"
if [[ -z "$consumer_captcha_code" ]]; then
  echo "无法从 Redis 读取 consumer 验证码，captchaId=${consumer_captcha_id}" >&2
  exit 1
fi

echo "[4/6] manager login"
curl -fsS -X POST "${BASE_URL}/manager/manager/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${MANAGER_USER}\",\"pwd\":\"${MANAGER_PWD}\",\"captchaId\":\"${manager_captcha_id}\",\"code\":\"${manager_captcha_code}\"}" \
  > "$manager_login_file"
assert_success "$manager_login_file" "manager login"
manager_token="$(json_get "$manager_login_file" "data.token")"

echo "[5/6] consumer login"
curl -fsS -X POST "${BASE_URL}/consumer/user/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${CONSUMER_USER}\",\"pwd\":\"${CONSUMER_PWD}\",\"captchaId\":\"${consumer_captcha_id}\",\"code\":\"${consumer_captcha_code}\"}" \
  > "$consumer_login_file"
assert_success "$consumer_login_file" "consumer login"
consumer_token="$(json_get "$consumer_login_file" "data.token")"

echo "[6/6] protected APIs"
curl -fsS -X POST "${BASE_URL}/manager/app/queryPage" \
  -H 'Content-Type: application/json' \
  -H "Authorization: ${manager_token}" \
  -d '{"pageNow":1,"pageSize":2}' >/dev/null

curl -fsS -X POST "${BASE_URL}/consumer/app/queryPage" \
  -H 'Content-Type: application/json' \
  -H "Authorization: ${consumer_token}" \
  -d '{"pageNow":1,"pageSize":2}' >/dev/null

echo "smoke test passed"

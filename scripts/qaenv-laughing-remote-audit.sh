#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

DEPLOY_HOST="${QAENV_DEPLOY_HOST:-43.242.200.25}"
DEPLOY_PORT="${QAENV_DEPLOY_PORT:-58753}"
DEPLOY_USER="${QAENV_DEPLOY_USER:-root}"
DEPLOY_DIR="${QAENV_DEPLOY_DIR:-/opt/qa-assistant-laughing}"
DEPLOY_PASSWORD="${QAENV_DEPLOY_PASSWORD:-}"
SSH_OPTS="${QAENV_REMOTE_SSH_OPTS:- -o StrictHostKeyChecking=accept-new }"
REMOTE_LOG_TAIL_LINES="${QAENV_REMOTE_LOG_TAIL_LINES:-200}"
REMOTE_DOCKER_SERVICE_NAME="${QAENV_REMOTE_DOCKER_SERVICE_NAME:-qa-assistant-backend-laughing}"
REMOTE_BACKEND_CONTAINER_NAME="${QAENV_REMOTE_BACKEND_CONTAINER_NAME:-qa-assistant-backend-laughing}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd ssh
need_cmd python3

SSH_BASE=(ssh)
if [ -n "$DEPLOY_PASSWORD" ]; then
  need_cmd sshpass
  SSH_BASE=(sshpass -p "$DEPLOY_PASSWORD" ssh)
fi

TMP_DIR="$(mktemp -d)"
RESULT_FILE="$TMP_DIR/qaenv-laughing-remote-audit.json"
trap 'rm -rf "$TMP_DIR"' EXIT

REMOTE_SCRIPT="$TMP_DIR/qaenv_laughing_remote_audit.sh"
cat > "$REMOTE_SCRIPT" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="$1"
REMOTE_LOG_TAIL_LINES="$2"
REMOTE_DOCKER_SERVICE_NAME="$3"
REMOTE_BACKEND_CONTAINER_NAME="$4"

ENV_FILE="$DEPLOY_DIR/.env"

python3 - "$ENV_FILE" <<'PY'
import json
import sys
from pathlib import Path

env_file = Path(sys.argv[1])
env_map = {}
if env_file.exists():
    for raw_line in env_file.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        env_map[key] = value

alert_keys = [
    "AI_ASSISTANT_ALERT_ENABLED",
    "AI_ASSISTANT_ALERT_SELF_CHECK_ENABLED",
    "AI_ASSISTANT_ALERT_SELF_CHECK_ALLOWED_USERS",
    "AI_ASSISTANT_ALERT_RECIPIENTS",
    "AI_ASSISTANT_ALERT_FROM",
    "AI_ASSISTANT_ALERT_SMTP_HOST",
    "AI_ASSISTANT_ALERT_SMTP_PORT",
    "AI_ASSISTANT_ALERT_SMTP_USERNAME",
    "AI_ASSISTANT_ALERT_SMTP_PASSWORD",
    "AI_ASSISTANT_ALERT_SMTP_SSL_ENABLE",
    "AI_ASSISTANT_ALERT_SMTP_SSL_TRUST",
]

def mask_value(key, value):
    if not value:
        return ""
    if key in {"AI_ASSISTANT_ALERT_RECIPIENTS", "AI_ASSISTANT_ALERT_SELF_CHECK_ALLOWED_USERS"}:
        parts = [item.strip() for item in value.split(",") if item.strip()]
        return [item[:3] + "***" for item in parts]
    if "PASSWORD" in key:
        return "***"
    if "USERNAME" in key or "FROM" in key or "HOST" in key:
        return value[:3] + "***"
    return value

summary = {}
for key in alert_keys:
    raw = env_map.get(key, "").strip()
    summary[key] = {
        "status": "set" if raw else "missing",
        "maskedValue": mask_value(key, raw),
    }

print(json.dumps({
    "envFile": str(env_file),
    "envExists": env_file.exists(),
    "alertEnv": summary,
}, ensure_ascii=False))
PY

echo "__QAENV_AUDIT_SPLIT__"

if command -v docker >/dev/null 2>&1; then
  docker ps --format '{{json .}}'
fi

echo "__QAENV_AUDIT_SPLIT__"

if command -v docker >/dev/null 2>&1; then
  docker logs --tail "$REMOTE_LOG_TAIL_LINES" "$REMOTE_BACKEND_CONTAINER_NAME" 2>&1 || true
fi

echo "__QAENV_AUDIT_SPLIT__"

if [ -f "$DEPLOY_DIR/logs/backend/backend.log" ]; then
  tail -n "$REMOTE_LOG_TAIL_LINES" "$DEPLOY_DIR/logs/backend/backend.log" || true
fi
EOS
chmod +x "$REMOTE_SCRIPT"

REMOTE_OUTPUT="$TMP_DIR/remote-output.txt"
"${SSH_BASE[@]}" ${SSH_OPTS} -p "$DEPLOY_PORT" "$DEPLOY_USER@$DEPLOY_HOST" \
  "bash -s -- '$DEPLOY_DIR' '$REMOTE_LOG_TAIL_LINES' '$REMOTE_DOCKER_SERVICE_NAME' '$REMOTE_BACKEND_CONTAINER_NAME'" \
  < "$REMOTE_SCRIPT" > "$REMOTE_OUTPUT"

python3 - "$REMOTE_OUTPUT" "$DEPLOY_HOST" "$DEPLOY_PORT" "$DEPLOY_USER" "$DEPLOY_DIR" "$REMOTE_BACKEND_CONTAINER_NAME" <<'PY' > "$RESULT_FILE"
import json
import sys
from pathlib import Path

output_path, deploy_host, deploy_port, deploy_user, deploy_dir, backend_container = sys.argv[1:]
raw = Path(output_path).read_text(encoding="utf-8", errors="replace")
parts = raw.split("__QAENV_AUDIT_SPLIT__\n")
if len(parts) < 4:
    raise RuntimeError("unexpected remote audit output format")

env_json = json.loads(parts[0].strip())

docker_lines = [line for line in parts[1].splitlines() if line.strip()]
docker_items = []
for line in docker_lines:
    try:
        docker_items.append(json.loads(line))
    except Exception:
        docker_items.append({"raw": line})

backend_logs = parts[2]
file_logs = parts[3]

keywords = [
    "alertReadiness",
    "alert self check",
    "AlertProperties",
    "JavaMailSender",
    "UnknownHostException",
    "MailException",
    "BindException",
    "90000",
    "系统异常",
    "AI_ASSISTANT_ALERT",
]

def extract_hits(text):
    hits = []
    for line in text.splitlines():
        if any(word in line for word in keywords):
            hits.append(line[-400:])
    return hits[-80:]

summary = {
    "remote": {
        "host": deploy_host,
        "port": deploy_port,
        "user": deploy_user,
        "deployDir": deploy_dir,
        "backendContainer": backend_container,
    },
    "envAudit": env_json,
    "dockerPs": docker_items,
    "backendLogHits": extract_hits(backend_logs),
    "backendFileLogHits": extract_hits(file_logs),
}

print(json.dumps(summary, ensure_ascii=False, indent=2))
PY

cat "$RESULT_FILE"

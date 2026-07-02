#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ZENTAO_MCP_FILE="${ZENTAO_MCP_FILE:-$ROOT_DIR/../osh-all/osh-backend/mcp.json}"
ZENTAO_BASE_URL="${ZENTAO_BASE_URL:-}"
ZENTAO_TOKEN="${ZENTAO_TOKEN:-}"
TASK_ID="${TASK_ID:-}"
NOTE_FILE="${NOTE_FILE:-}"
VERIFY_TAIL_LINES="${VERIFY_TAIL_LINES:-30}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd curl
need_cmd python3

if [ -z "$TASK_ID" ]; then
  echo "TASK_ID is required" >&2
  exit 1
fi

if [ -z "$NOTE_FILE" ] || [ ! -f "$NOTE_FILE" ]; then
  echo "NOTE_FILE is required and must exist" >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

python3 - "$ZENTAO_MCP_FILE" "$ZENTAO_BASE_URL" "$ZENTAO_TOKEN" <<'PY' > "$TMP_DIR/zentao-auth.json"
import json
import sys
from pathlib import Path

mcp_file, base_url, token = sys.argv[1:]
base_url = (base_url or "").strip()
token = (token or "").strip()

if (not base_url or not token) and Path(mcp_file).exists():
    mcp = json.loads(Path(mcp_file).read_text(encoding="utf-8-sig"))
    server = (mcp.get("mcpServers") or {}).get("zentao") or {}
    headers = server.get("headers") or {}
    if not base_url:
        base_url = (server.get("apiBaseUrl") or "").strip()
    if not token:
        token = (headers.get("token") or "").strip()

if not base_url:
    base_url = "http://43.242.200.25:52001"

if not token:
    raise RuntimeError("missing ZENTAO token")

print(json.dumps({"baseUrl": base_url, "token": token}, ensure_ascii=False))
PY

BASE_URL="$(python3 - <<'PY' "$TMP_DIR/zentao-auth.json"
import json,sys
print(json.loads(open(sys.argv[1]).read())["baseUrl"])
PY
)"
TOKEN="$(python3 - <<'PY' "$TMP_DIR/zentao-auth.json"
import json,sys
print(json.loads(open(sys.argv[1]).read())["token"])
PY
)"

BEFORE_JSON="$TMP_DIR/task-before.json"
BEFORE_DESC="$TMP_DIR/task-desc-before.txt"
APPEND_FILE="$TMP_DIR/task-append.txt"
NEW_DESC="$TMP_DIR/task-desc-new.txt"
UPDATE_JSON="$TMP_DIR/task-update.json"
VERIFY_FILE="$TMP_DIR/task-verify.txt"

curl -fsS "$BASE_URL/api.php/v2/tasks/$TASK_ID" \
  -H "Token: $TOKEN" > "$BEFORE_JSON"

python3 - <<'PY' "$BEFORE_JSON" "$BEFORE_DESC"
import json
import sys
from pathlib import Path

before_json, before_desc = sys.argv[1:]
obj = json.loads(Path(before_json).read_text())
task = obj.get("task") or {}
desc = task.get("desc") or task.get("description") or ""
Path(before_desc).write_text(desc, encoding="utf-8")
PY

cp "$NOTE_FILE" "$APPEND_FILE"

python3 - <<'PY' "$BEFORE_DESC" "$APPEND_FILE" "$NEW_DESC"
import sys
from pathlib import Path

before_desc, append_file, new_desc = map(Path, sys.argv[1:])
before = before_desc.read_text(encoding="utf-8")
append = append_file.read_text(encoding="utf-8").strip()

if before.strip():
    merged = before.rstrip() + "\n\n" + append + "\n"
else:
    merged = append + "\n"

new_desc.write_text(merged, encoding="utf-8")
PY

python3 - <<'PY' "$NEW_DESC" "$UPDATE_JSON"
import json
import sys
from pathlib import Path

new_desc, update_json = map(Path, sys.argv[1:])
payload = {"desc": new_desc.read_text(encoding="utf-8")}
update_json.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
PY

curl -fsS -X PUT "$BASE_URL/api.php/v2/tasks/$TASK_ID" \
  -H "Token: $TOKEN" \
  -H 'Content-Type: application/json' \
  --data-binary "@$UPDATE_JSON" > /dev/null

curl -fsS "$BASE_URL/api.php/v2/tasks/$TASK_ID" \
  -H "Token: $TOKEN" > "$TMP_DIR/task-after.json"

python3 - <<'PY' "$TMP_DIR/task-after.json" "$VERIFY_TAIL_LINES" "$VERIFY_FILE"
import json
import sys
from pathlib import Path

after_json, tail_lines, verify_file = sys.argv[1:]
tail_lines = int(tail_lines)
obj = json.loads(Path(after_json).read_text())
task = obj.get("task") or {}
desc = (task.get("desc") or "").splitlines()
tail = "\n".join(desc[-tail_lines:])
Path(verify_file).write_text(tail, encoding="utf-8")
print(tail)
PY

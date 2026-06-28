#!/usr/bin/env bash
set -euo pipefail

DEPLOY_HOST="${QAENV_DEPLOY_HOST:-43.242.200.25}"

echo "[1/3] Checking web entry"
curl -fsS -o /tmp/qaenv-laughing-web.html "http://$DEPLOY_HOST:18088/ai/"

echo "[2/3] Checking backend captcha API"
curl -fsS "http://$DEPLOY_HOST:19088/auth/getCode" > /tmp/qaenv-laughing-auth.json

echo "[3/3] Printing compact verification summary"
python3 - <<'PY'
import json
from pathlib import Path
auth = json.loads(Path("/tmp/qaenv-laughing-auth.json").read_text())
print({
    "auth_code": auth.get("code"),
    "has_captcha_id": bool(auth.get("data", {}).get("captchaId")),
    "has_image": bool(auth.get("data", {}).get("text")),
})
PY

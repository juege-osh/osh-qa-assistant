#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_HOST="${QAENV_DEPLOY_HOST:-43.242.200.25}"
ENV_SOURCE="${QAENV_RUNTIME_ENV_FILE:-$ROOT_DIR/.qaenv-laughing.env.local}"

echo "[1/3] Checking web entry"
curl -fsS -o /tmp/qaenv-laughing-web.html "http://$DEPLOY_HOST:18088/ai/"

echo "[2/3] Checking backend captcha API"
curl -fsS "http://$DEPLOY_HOST:19088/auth/getCode" > /tmp/qaenv-laughing-auth.json

echo "[3/3] Printing compact verification summary"
python3 - "$ENV_SOURCE" <<'PY'
import json
import os
from pathlib import Path

env_source = Path(os.sys.argv[1])
auth = json.loads(Path("/tmp/qaenv-laughing-auth.json").read_text())

env_map = {}
if env_source.exists():
    for raw_line in env_source.read_text(encoding="utf-8").splitlines():
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

alert_summary = {
    key: ("set" if env_map.get(key, "").strip() else "missing")
    for key in alert_keys
}

def parse_bool(raw):
    value = (raw or "").strip().lower()
    if value in {"true", "1", "yes", "on"}:
        return True
    if value in {"false", "0", "no", "off"}:
        return False
    return None

def has_value(key):
    return bool(env_map.get(key, "").strip())

enabled_state = parse_bool(env_map.get("AI_ASSISTANT_ALERT_ENABLED"))
self_check_state = parse_bool(env_map.get("AI_ASSISTANT_ALERT_SELF_CHECK_ENABLED"))

if "AI_ASSISTANT_ALERT_ENABLED" not in env_map:
    alert_profile_status = "not_included"
    alert_profile_message = "部署源文件未纳入最小故障告警配置。"
elif enabled_state is False:
    alert_profile_status = "disabled_in_source"
    alert_profile_message = "部署源文件已声明告警配置，但当前显式关闭。"
else:
    required_when_enabled = [
        "AI_ASSISTANT_ALERT_RECIPIENTS",
        "AI_ASSISTANT_ALERT_SMTP_HOST",
        "AI_ASSISTANT_ALERT_SMTP_PORT",
        "AI_ASSISTANT_ALERT_SMTP_USERNAME",
        "AI_ASSISTANT_ALERT_SMTP_PASSWORD",
    ]
    missing_required = [key for key in required_when_enabled if not has_value(key)]
    if missing_required:
        alert_profile_status = "incomplete_when_enabled"
        alert_profile_message = "部署源文件已纳入告警配置，但关键 SMTP/收件人配置不完整。"
    elif env_map.get("AI_ASSISTANT_ALERT_SMTP_PORT", "").strip() == "465" and parse_bool(env_map.get("AI_ASSISTANT_ALERT_SMTP_SSL_ENABLE")) is not True:
        alert_profile_status = "ssl_profile_mismatch"
        alert_profile_message = "部署源文件使用 SMTPS 端口 465，但未显式开启 SMTP SSL。"
    elif parse_bool(env_map.get("AI_ASSISTANT_ALERT_SMTP_SSL_ENABLE")) is True and not has_value("AI_ASSISTANT_ALERT_SMTP_SSL_TRUST"):
        alert_profile_status = "ssl_trust_missing"
        alert_profile_message = "部署源文件已开启 SMTP SSL，但未声明 SMTP SSL trust 主机。"
    elif self_check_state is False:
        alert_profile_status = "enabled_but_self_check_off"
        alert_profile_message = "部署源文件里的最小故障告警已开启，但人工自检入口关闭。"
    else:
        alert_profile_status = "configured"
        alert_profile_message = "部署源文件中的最小故障告警配置看起来基本完整。"

expected_readiness_statuses = [
    "DISABLED",
    "SELF_CHECK_DISABLED",
    "CONFIG_MISSING",
    "SENDER_UNAVAILABLE",
    "READY",
]

if alert_profile_status == "not_included":
    runtime_inference = "如果共享 QA 仍按这份部署源下发，alertReadiness 正常更可能返回 DISABLED/CONFIG_MISSING，而不是 code=90000；90000 更像远端真实 .env 漂移或运行态额外异常。"
elif alert_profile_status == "configured":
    runtime_inference = "部署源文件里的告警配置已基本完整，若共享 QA 仍返回 code=90000，应优先读取远端异常栈和真实 .env。"
else:
    runtime_inference = "部署源文件与共享 QA 运行态仍需对照，优先确认远端真实 .env 与后端异常栈。"

print({
    "auth_code": auth.get("code"),
    "has_captcha_id": bool(auth.get("data", {}).get("captchaId")),
    "has_image": bool(auth.get("data", {}).get("text")),
    "qaenv_source": str(env_source),
    "qaenv_exists": env_source.exists(),
    "alert_env": alert_summary,
    "alert_profile_status": alert_profile_status,
    "alert_profile_message": alert_profile_message,
    "expected_readiness_statuses": expected_readiness_statuses,
    "runtime_inference": runtime_inference,
})
PY

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_SOURCE="${QAENV_RUNTIME_ENV_FILE:-$ROOT_DIR/.qaenv-laughing.env.local}"
OUTPUT_PATH="${OUTPUT_PATH:-}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd python3
need_cmd sha256sum

if [ ! -f "$ENV_SOURCE" ]; then
  echo "Runtime env file not found: $ENV_SOURCE" >&2
  echo "Create it from $ROOT_DIR/.qaenv-laughing.env.example" >&2
  exit 1
fi

LOCAL_JAR="$ROOT_DIR/backend/target/backend.jar"
RUNTIME_JAR="$ROOT_DIR/.laughing-runtime/backend.jar"

if [ ! -f "$LOCAL_JAR" ]; then
  echo "Local backend jar not found: $LOCAL_JAR" >&2
  echo "Run: mvn -f $ROOT_DIR/pom.xml -pl backend -am clean package -DskipTests" >&2
  exit 1
fi

python3 - "$ENV_SOURCE" "$LOCAL_JAR" "$RUNTIME_JAR" "$OUTPUT_PATH" <<'PY'
import json
import os
import subprocess
import sys
from pathlib import Path

env_source = Path(sys.argv[1])
local_jar = Path(sys.argv[2])
runtime_jar = Path(sys.argv[3])
output_path = sys.argv[4].strip()

def parse_env(path: Path):
    result = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key] = value
    return result

def sha256(path: Path):
    return subprocess.check_output(["sha256sum", str(path)], text=True).split()[0]

def stat_mtime(path: Path):
    return subprocess.check_output(
        ["stat", "-f", "%Sm", "-t", "%Y-%m-%d %H:%M:%S", str(path)],
        text=True,
    ).strip()

env_map = parse_env(env_source)

required_alert_keys = [
    "AI_ASSISTANT_ALERT_ENABLED",
    "AI_ASSISTANT_ALERT_SELF_CHECK_ENABLED",
    "AI_ASSISTANT_ALERT_SELF_CHECK_ALLOWED_USERS",
    "AI_ASSISTANT_ALERT_RECIPIENTS",
    "AI_ASSISTANT_ALERT_SMTP_HOST",
    "AI_ASSISTANT_ALERT_SMTP_PORT",
    "AI_ASSISTANT_ALERT_SMTP_USERNAME",
    "AI_ASSISTANT_ALERT_SMTP_PASSWORD",
    "AI_ASSISTANT_ALERT_SMTP_SSL_ENABLE",
    "AI_ASSISTANT_ALERT_SMTP_SSL_TRUST",
]

missing_alert_keys = [key for key in required_alert_keys if not env_map.get(key, "").strip()]

plan = {
    "envSource": str(env_source),
    "localJar": {
        "path": str(local_jar),
        "mtime": stat_mtime(local_jar),
        "sha256": sha256(local_jar),
    },
    "runtimeJar": {
        "path": str(runtime_jar),
        "exists": runtime_jar.exists(),
        "mtime": stat_mtime(runtime_jar) if runtime_jar.exists() else None,
        "sha256": sha256(runtime_jar) if runtime_jar.exists() else None,
    },
    "alertConfig": {
        "requiredKeys": required_alert_keys,
        "missingKeysInSource": missing_alert_keys,
        "readyInSource": not missing_alert_keys,
    },
    "remoteFixChecklist": [
        "确认共享 QA laughing 将使用最新 backend.jar，而不是 2026-06-25 的旧包。",
        "将本机部署源文件中的 AI_ASSISTANT_ALERT_* 整组同步到远端 /opt/qa-assistant-laughing/.env。",
        "共享 SMTP 若使用 465 端口，需要同时携带 AI_ASSISTANT_ALERT_SMTP_SSL_ENABLE=true 和 AI_ASSISTANT_ALERT_SMTP_SSL_TRUST。",
        "执行 docker compose up -d --remove-orphans --force-recreate，确保 backend 容器重建。",
        "回归验证 /consumer/ops/alertReadiness。",
        "如 readiness 返回 READY，再验证 /consumer/ops/alertSelfCheck。",
    ],
    "remoteVerifyChecklist": [
        "远端 backend.jar 时间与哈希更新到本地最新包。",
        "远端 .env 中 AI_ASSISTANT_ALERT_* 不再缺失。",
        "远端日志不再出现 No static resource consumer/ops/alertReadiness。",
        "alertReadiness 返回 DISABLED / SELF_CHECK_DISABLED / CONFIG_MISSING / SENDER_UNAVAILABLE / READY 之一，而不是 404/90000。",
    ],
}

text = json.dumps(plan, ensure_ascii=False, indent=2)
print(text)

if output_path:
    Path(output_path).write_text(text, encoding="utf-8")
PY

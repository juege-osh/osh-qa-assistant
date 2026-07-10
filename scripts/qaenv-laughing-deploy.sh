#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

DEPLOY_HOST="${QAENV_DEPLOY_HOST:-43.242.200.25}"
DEPLOY_PORT="${QAENV_DEPLOY_PORT:-58753}"
DEPLOY_USER="${QAENV_DEPLOY_USER:-root}"
DEPLOY_DIR="${QAENV_DEPLOY_DIR:-/opt/qa-assistant-laughing}"
DEPLOY_PASSWORD="${QAENV_DEPLOY_PASSWORD:-}"
ENV_SOURCE="${QAENV_RUNTIME_ENV_FILE:-$ROOT_DIR/.qaenv-laughing.env.local}"
SSH_OPTS="${QAENV_REMOTE_SSH_OPTS:- -o StrictHostKeyChecking=accept-new }"

if [ ! -f "$ENV_SOURCE" ]; then
  echo "Runtime env file not found: $ENV_SOURCE" >&2
  echo "Create it from $ROOT_DIR/.qaenv-laughing.env.example" >&2
  exit 1
fi

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

need_cmd mvn
need_cmd npm
need_cmd rsync
need_cmd ssh

SSH_BASE=(ssh)
RSYNC_RSH="ssh ${SSH_OPTS} -p $DEPLOY_PORT"
if [ -n "$DEPLOY_PASSWORD" ]; then
  need_cmd sshpass
  SSH_BASE=(sshpass -p "$DEPLOY_PASSWORD" ssh)
  RSYNC_BASE=(sshpass -p "$DEPLOY_PASSWORD" rsync)
else
  RSYNC_BASE=(rsync)
fi

echo "[1/7] Building backend jar"
mvn -f "$ROOT_DIR/pom.xml" -pl backend -am clean package -DskipTests

echo "[2/7] Building unified frontend"
(
  cd "$ROOT_DIR/frontend/ai-assistant-web"
  npm ci --legacy-peer-deps
  VITE_BASE_URL='' \
    VITE_BASE_RESOURCES_URL='/consumer/' \
    npm run build
)

echo "[3/7] Preparing remote directories"
"${SSH_BASE[@]}" ${SSH_OPTS} -p "$DEPLOY_PORT" "$DEPLOY_USER@$DEPLOY_HOST" "
  mkdir -p '$DEPLOY_DIR/config' '$DEPLOY_DIR/logs/backend' '$DEPLOY_DIR/upload' '$DEPLOY_DIR/nginx' '$DEPLOY_DIR/html/ai'
"

echo "[4/7] Uploading backend and deploy configs"
"${RSYNC_BASE[@]}" -az --inplace \
  -e "$RSYNC_RSH" \
  "$ROOT_DIR/backend/target/backend.jar" \
  "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR/backend.jar"

"${RSYNC_BASE[@]}" -az \
  -e "$RSYNC_RSH" \
  "$ROOT_DIR/ops/laughing/docker-compose.laughing.yml" \
  "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR/docker-compose.yml"

"${RSYNC_BASE[@]}" -az \
  -e "$RSYNC_RSH" \
  "$ROOT_DIR/ops/laughing/backend-prod-laughing.yml" \
  "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR/config/backend-prod-laughing.yml"

"${RSYNC_BASE[@]}" -az \
  -e "$RSYNC_RSH" \
  "$ROOT_DIR/ops/laughing/nginx.laughing.conf" \
  "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR/nginx/default.conf"

echo "[5/7] Uploading frontend dist"
"${RSYNC_BASE[@]}" -az --delete \
  -e "$RSYNC_RSH" \
  "$ROOT_DIR/frontend/ai-assistant-web/dist/" \
  "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR/html/ai/"

echo "[6/7] Writing runtime env"
"${RSYNC_BASE[@]}" -az \
  -e "$RSYNC_RSH" \
  "$ENV_SOURCE" \
  "$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_DIR/.env"

echo "[7/7] Starting laughing stack on QA env"
"${SSH_BASE[@]}" ${SSH_OPTS} -p "$DEPLOY_PORT" "$DEPLOY_USER@$DEPLOY_HOST" "
  cd '$DEPLOY_DIR'
  docker compose pull
  docker compose up -d --remove-orphans --force-recreate
  docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
"

echo
echo "QA laughing deploy finished."
echo "Runtime note: always validate against the recreated container after deploy."
echo "Directly replacing backend.jar without a container restart can surface false errors such as ZipException / NoClassDefFoundError."
echo "Web: http://$DEPLOY_HOST:18088/ai/"
echo "Backend: http://$DEPLOY_HOST:19088/auth/getCode"

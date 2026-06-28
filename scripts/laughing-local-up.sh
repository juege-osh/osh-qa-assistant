#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUNTIME_DIR="${LAUGHING_RUNTIME_DIR:-$ROOT_DIR/.laughing-runtime}"
ENV_SOURCE="${LAUGHING_ENV_FILE:-$ROOT_DIR/.env.local}"
COMPOSE_PROJECT="${LAUGHING_COMPOSE_PROJECT:-qa-assistant-laughing}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

need_cmd docker
need_cmd rsync

if [ ! -f "$ENV_SOURCE" ]; then
  echo "Env file not found: $ENV_SOURCE" >&2
  exit 1
fi

if [ "${LAUGHING_SKIP_BUILD:-0}" != "1" ]; then
  need_cmd mvn
  need_cmd npm

  echo "[1/4] Building backend jar"
  mvn -f "$ROOT_DIR/pom.xml" -pl backend -am package -DskipTests

  echo "[2/4] Building unified frontend"
  (
    cd "$ROOT_DIR/frontend/ai-assistant-web"
    npm ci --legacy-peer-deps
    VITE_BASE_URL="${LAUGHING_VITE_BASE_URL:-}" \
      VITE_BASE_RESOURCES_URL="${LAUGHING_VITE_BASE_RESOURCES_URL:-/consumer/}" \
      npm run build
  )
else
  echo "[1/4] Skipping builds because LAUGHING_SKIP_BUILD=1"
fi

if [ ! -f "$ROOT_DIR/backend/target/backend.jar" ]; then
  echo "Backend jar is missing: $ROOT_DIR/backend/target/backend.jar" >&2
  exit 1
fi

if [ ! -d "$ROOT_DIR/frontend/ai-assistant-web/dist" ]; then
  echo "Frontend dist is missing: $ROOT_DIR/frontend/ai-assistant-web/dist" >&2
  exit 1
fi

echo "[3/4] Preparing runtime directory: $RUNTIME_DIR"
mkdir -p "$RUNTIME_DIR/config" "$RUNTIME_DIR/nginx" "$RUNTIME_DIR/html/ai" "$RUNTIME_DIR/logs/backend" "$RUNTIME_DIR/upload"
cp "$ROOT_DIR/backend/target/backend.jar" "$RUNTIME_DIR/backend.jar"
cp "$ROOT_DIR/ops/laughing/backend-prod-laughing.yml" "$RUNTIME_DIR/config/backend-prod-laughing.yml"
cp "$ROOT_DIR/ops/laughing/nginx.laughing.conf" "$RUNTIME_DIR/nginx/default.conf"
cp "$ROOT_DIR/ops/laughing/docker-compose.laughing.yml" "$RUNTIME_DIR/docker-compose.yml"
rsync -az --delete "$ROOT_DIR/frontend/ai-assistant-web/dist/" "$RUNTIME_DIR/html/ai/"

cp "$ENV_SOURCE" "$RUNTIME_DIR/.env"
cat >> "$RUNTIME_DIR/.env" <<EOF

# Local laughing stack defaults. Override these before running the script if needed.
LAUGHING_HTTP_PORT=${LAUGHING_HTTP_PORT:-18088}
LAUGHING_BACKEND_PORT=${LAUGHING_BACKEND_PORT:-19088}
LAUGHING_BACKEND_MEMORY_LIMIT=${LAUGHING_BACKEND_MEMORY_LIMIT:-1024m}
LAUGHING_JAVA_INITIAL_RAM_PERCENTAGE=${LAUGHING_JAVA_INITIAL_RAM_PERCENTAGE:-20}
LAUGHING_JAVA_MAX_RAM_PERCENTAGE=${LAUGHING_JAVA_MAX_RAM_PERCENTAGE:-60}
AI_ASSISTANT_CONTAINER_UPLOAD_DIR=/opt/qa-assistant/upload
EOF

echo "[4/4] Starting laughing app-only stack"
docker compose -f "$RUNTIME_DIR/docker-compose.yml" --project-name "$COMPOSE_PROJECT" up -d --remove-orphans --force-recreate

echo
docker compose -f "$RUNTIME_DIR/docker-compose.yml" --project-name "$COMPOSE_PROJECT" ps
echo
echo "Web:     http://127.0.0.1:${LAUGHING_HTTP_PORT:-18088}/ai/"
echo "Backend: http://127.0.0.1:${LAUGHING_BACKEND_PORT:-19088}/auth/getCode"
echo "Monitor: ./scripts/laughing-monitor.sh"

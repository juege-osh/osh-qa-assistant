#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUNTIME_DIR="${LAUGHING_RUNTIME_DIR:-$ROOT_DIR/.laughing-runtime}"
ENV_SOURCE="${LAUGHING_ENV_FILE:-$ROOT_DIR/.env.local}"
COMPOSE_PROJECT="${LAUGHING_COMPOSE_PROJECT:-qa-assistant-laughing}"
ENABLE_ALERT_SANDBOX="${LAUGHING_ENABLE_ALERT_SANDBOX:-0}"
ENABLE_LOCAL_QDRANT="${LAUGHING_ENABLE_LOCAL_QDRANT:-0}"

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
  mvn -f "$ROOT_DIR/pom.xml" -pl backend -am clean package -DskipTests

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
mkdir -p "$RUNTIME_DIR/config" "$RUNTIME_DIR/nginx" "$RUNTIME_DIR/html/ai" "$RUNTIME_DIR/logs/backend" "$RUNTIME_DIR/upload" "$RUNTIME_DIR/qdrant"
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
LAUGHING_QDRANT_HTTP_PORT=${LAUGHING_QDRANT_HTTP_PORT:-16333}
LAUGHING_QDRANT_GRPC_PORT=${LAUGHING_QDRANT_GRPC_PORT:-16334}
AI_ASSISTANT_CONTAINER_UPLOAD_DIR=/opt/qa-assistant/upload
EOF

COMPOSE_ARGS=(-f "$RUNTIME_DIR/docker-compose.yml" --project-name "$COMPOSE_PROJECT")
if [ "$ENABLE_ALERT_SANDBOX" = "1" ]; then
  cat >> "$RUNTIME_DIR/.env" <<EOF
AI_ASSISTANT_ALERT_ENABLED=${AI_ASSISTANT_ALERT_ENABLED:-true}
AI_ASSISTANT_ALERT_SELF_CHECK_ENABLED=${AI_ASSISTANT_ALERT_SELF_CHECK_ENABLED:-true}
AI_ASSISTANT_ALERT_SELF_CHECK_ALLOWED_USERS=${AI_ASSISTANT_ALERT_SELF_CHECK_ALLOWED_USERS:-course_demo_user}
AI_ASSISTANT_ALERT_RECIPIENTS=${AI_ASSISTANT_ALERT_RECIPIENTS:-codex-local@example.com}
AI_ASSISTANT_ALERT_FROM=${AI_ASSISTANT_ALERT_FROM:-codex-local@example.com}
AI_ASSISTANT_ALERT_SMTP_HOST=${AI_ASSISTANT_ALERT_SMTP_HOST:-qa-assistant-mailpit-laughing}
AI_ASSISTANT_ALERT_SMTP_PORT=${AI_ASSISTANT_ALERT_SMTP_PORT:-1025}
AI_ASSISTANT_ALERT_SMTP_PROTOCOL=${AI_ASSISTANT_ALERT_SMTP_PROTOCOL:-smtp}
AI_ASSISTANT_ALERT_SMTP_AUTH=${AI_ASSISTANT_ALERT_SMTP_AUTH:-false}
AI_ASSISTANT_ALERT_SMTP_STARTTLS=${AI_ASSISTANT_ALERT_SMTP_STARTTLS:-false}
AI_ASSISTANT_ALERT_SMTP_USERNAME=${AI_ASSISTANT_ALERT_SMTP_USERNAME:-}
AI_ASSISTANT_ALERT_SMTP_PASSWORD=${AI_ASSISTANT_ALERT_SMTP_PASSWORD:-}
LAUGHING_MAILPIT_SMTP_PORT=${LAUGHING_MAILPIT_SMTP_PORT:-1025}
LAUGHING_MAILPIT_UI_PORT=${LAUGHING_MAILPIT_UI_PORT:-18025}
EOF
  COMPOSE_ARGS+=(--profile mailpit)
fi

if [ "$ENABLE_LOCAL_QDRANT" = "1" ]; then
  cat >> "$RUNTIME_DIR/.env" <<EOF
AI_ASSISTANT_QDRANT_HOST=qa-assistant-qdrant-laughing
AI_ASSISTANT_QDRANT_PORT=6334
AI_ASSISTANT_QDRANT_COLLECTION=${AI_ASSISTANT_QDRANT_COLLECTION:-assistant_knowledge_lib}
SPRING_AI_VECTORSTORE_QDRANT_INITIALIZE_SCHEMA=true
EOF
  COMPOSE_ARGS+=(--profile local-qdrant)
fi

echo "[4/4] Starting laughing app-only stack"
docker compose "${COMPOSE_ARGS[@]}" up -d --remove-orphans --force-recreate

echo
docker compose "${COMPOSE_ARGS[@]}" ps
echo
echo "Note: if you manually replace $RUNTIME_DIR/backend.jar later, restart or recreate qa-assistant-backend-laughing before validating."
echo "      Hot-swapping the jar file without a container restart can cause false runtime errors such as ZipException / NoClassDefFoundError."
echo "Web:     http://127.0.0.1:${LAUGHING_HTTP_PORT:-18088}/ai/"
echo "Backend: http://127.0.0.1:${LAUGHING_BACKEND_PORT:-19088}/auth/getCode"
if [ "$ENABLE_ALERT_SANDBOX" = "1" ]; then
  echo "Mail UI: http://127.0.0.1:${LAUGHING_MAILPIT_UI_PORT:-18025}/"
fi
if [ "$ENABLE_LOCAL_QDRANT" = "1" ]; then
  echo "Qdrant:  http://127.0.0.1:${LAUGHING_QDRANT_HTTP_PORT:-16333}/"
fi
echo "Monitor: ./scripts/laughing-monitor.sh"

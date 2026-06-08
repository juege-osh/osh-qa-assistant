#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT_DIR/.local-runlogs"
BACKEND_JAR="$ROOT_DIR/backend/target/backend.jar"
ENV_FILE="$ROOT_DIR/.env.local"
BACKEND_PORT="${BACKEND_PORT:-9010}"
WEB_PORT="${WEB_PORT:-9100}"

load_local_env() {
  if [ -f "$ENV_FILE" ]; then
    echo "loading env from $ENV_FILE"
    set -a
    # shellcheck disable=SC1090
    . "$ENV_FILE"
    set +a
  fi
}

detect_java_home() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
    local current_major
    current_major="$("${JAVA_HOME}/bin/java" -version 2>&1 | awk -F[\\\".] '/version/ {print $2}')"
    if [ "${current_major:-0}" -ge 17 ]; then
      echo "$JAVA_HOME"
      return
    fi
  fi

  if command -v java >/dev/null 2>&1; then
    local java_bin detected_home current_major
    java_bin="$(command -v java)"
    detected_home="$("$java_bin" -XshowSettings:properties -version 2>&1 | awk -F'= ' '/^[[:space:]]*java.home = /{print $2; exit}')"
    current_major="$("$java_bin" -version 2>&1 | awk -F[\\\".] '/version/ {print $2}')"
    if [ -n "${detected_home:-}" ] && [ -x "$detected_home/bin/java" ] && [ "${current_major:-0}" -ge 17 ]; then
      echo "$detected_home"
      return
    fi
  fi

  if [ -x "/Users/rengang/Library/Java/JavaVirtualMachines/ms-17.0.17/Contents/Home/bin/java" ]; then
    echo "/Users/rengang/Library/Java/JavaVirtualMachines/ms-17.0.17/Contents/Home"
    return
  fi

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    local detected
    detected="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    if [ -n "$detected" ] && [ -x "$detected/bin/java" ]; then
      echo "$detected"
      return
    fi
  fi

  echo ""
}

detect_maven_bin() {
  if command -v mvn >/dev/null 2>&1; then
    command -v mvn
    return
  fi

  if [ -x "$ROOT_DIR/.tools/apache-maven-3.9.9/bin/mvn" ]; then
    echo "$ROOT_DIR/.tools/apache-maven-3.9.9/bin/mvn"
    return
  fi

  if [ -x "/Users/rengang/chuangye/osh-projects/.tools/apache-maven-3.9.9/bin/mvn" ]; then
    echo "/Users/rengang/chuangye/osh-projects/.tools/apache-maven-3.9.9/bin/mvn"
    return
  fi

  if [ -x "/Users/rengang/Downloads/apache-maven-3.8.8-bin/apache-maven-3.8.8/bin/mvn" ]; then
    echo "/Users/rengang/Downloads/apache-maven-3.8.8-bin/apache-maven-3.8.8/bin/mvn"
    return
  fi

  echo ""
}

ensure_port_free() {
  local port="$1"
  if lsof -ti "tcp:${port}" >/dev/null 2>&1; then
    echo "端口 ${port} 已被占用，请先执行 ./stop-local.sh 或手工释放端口。"
    exit 1
  fi
}

wait_http_up() {
  local url="$1"
  local label="$2"
  local max_retry="${3:-30}"
  local i
  for i in $(seq 1 "$max_retry"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "$label 已就绪: $url"
      return 0
    fi
    sleep 1
  done
  echo "$label 启动超时: $url"
  return 1
}

JAVA_HOME_USE="$(detect_java_home)"
if [ -z "$JAVA_HOME_USE" ]; then
  echo "未找到 JDK 17。请先安装 Java 17，或先执行："
  echo "export JAVA_HOME=\$(/usr/libexec/java_home -v 17)"
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_USE"
export PATH="$JAVA_HOME/bin:$PATH"

JAVA_BIN="$JAVA_HOME_USE/bin/java"
JAVA_MAJOR="$("$JAVA_BIN" -version 2>&1 | awk -F[\\\".] '/version/ {print $2}')"
if [ "${JAVA_MAJOR:-0}" -lt 17 ]; then
  echo "当前检测到的 Java 版本不是 17：$("$JAVA_BIN" -version 2>&1 | head -n 1)"
  echo "请先执行：export JAVA_HOME=\$(/usr/libexec/java_home -v 17)"
  exit 1
fi

mkdir -p "$LOG_DIR"

load_local_env

echo "using JAVA_HOME=$JAVA_HOME_USE"

for port in "$BACKEND_PORT" "$WEB_PORT"; do
  ensure_port_free "$port"
done

if [ ! -f "$BACKEND_JAR" ]; then
  MVN_BIN="$(detect_maven_bin)"
  if [ -z "$MVN_BIN" ]; then
    echo "未找到 Maven，且 $BACKEND_JAR 不存在，无法自动构建后端。"
    exit 1
  fi
  echo "[0/4] building unified backend jar"
  PATH="$(dirname "$MVN_BIN"):$JAVA_HOME_USE/bin:$PATH" \
    "$MVN_BIN" -f "$ROOT_DIR/pom.xml" -pl backend -am package -DskipTests
fi

echo "[1/4] starting unified backend on $BACKEND_PORT"
nohup "$JAVA_BIN" -jar "$BACKEND_JAR" \
  --server.port="$BACKEND_PORT" \
  > "$LOG_DIR/backend.log" 2>&1 < /dev/null &

echo "[2/3] starting unified frontend on $WEB_PORT"
(
  cd "$ROOT_DIR/frontend/ai-assistant-web"
  : > "$LOG_DIR/web-ui.log"
  nohup npm run dev -- --host 0.0.0.0 --port "$WEB_PORT" --strictPort > "$LOG_DIR/web-ui.log" 2>&1 < /dev/null &
)

wait_http_up "http://127.0.0.1:${BACKEND_PORT}/" "backend"
wait_http_up "http://127.0.0.1:${WEB_PORT}/" "web ui"

echo
echo "Started. Visit:"
echo "  web ui       http://127.0.0.1:${WEB_PORT}/"
echo "  manager api  http://127.0.0.1:${BACKEND_PORT}/manager/"
echo "  consumer api http://127.0.0.1:${BACKEND_PORT}/consumer/"
echo "  auth api     http://127.0.0.1:${BACKEND_PORT}/auth/"
echo
echo "Logs:"
echo "  $LOG_DIR/backend.log"
echo "  $LOG_DIR/web-ui.log"

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT_DIR/.local-runlogs"
BACKEND_JAR="$ROOT_DIR/backend/target/backend.jar"

detect_java_home() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
    echo "$JAVA_HOME"
    return
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

  if [ -x "/Users/rengang/chuangye/osh-projects/.tools/apache-maven-3.9.9/bin/mvn" ]; then
    echo "/Users/rengang/chuangye/osh-projects/.tools/apache-maven-3.9.9/bin/mvn"
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

JAVA_HOME_USE="$(detect_java_home)"
if [ -z "$JAVA_HOME_USE" ]; then
  echo "未找到 JDK 17。请先安装 Java 17，或先执行："
  echo "export JAVA_HOME=\$(/usr/libexec/java_home -v 17)"
  exit 1
fi

JAVA_BIN="$JAVA_HOME_USE/bin/java"
JAVA_MAJOR="$("$JAVA_BIN" -version 2>&1 | awk -F[\\\".] '/version/ {print $2}')"
if [ "${JAVA_MAJOR:-0}" -lt 17 ]; then
  echo "当前检测到的 Java 版本不是 17：$("$JAVA_BIN" -version 2>&1 | head -n 1)"
  echo "请先执行：export JAVA_HOME=\$(/usr/libexec/java_home -v 17)"
  exit 1
fi

mkdir -p "$LOG_DIR"

echo "using JAVA_HOME=$JAVA_HOME_USE"

for port in 9000 8001 9001; do
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

echo "[1/4] starting unified backend on 9000"
nohup "$JAVA_BIN" -jar "$BACKEND_JAR" \
  > "$LOG_DIR/backend.log" 2>&1 < /dev/null &

echo "[2/4] starting manager frontend on 8001"
(
  cd "$ROOT_DIR/frontend/ai-assistant-manager"
  : > "$LOG_DIR/manager-ui.log"
  nohup npm run dev -- --host 0.0.0.0 --port 8001 --strictPort > "$LOG_DIR/manager-ui.log" 2>&1 < /dev/null &
)

echo "[3/4] starting consumer frontend on 9001"
(
  cd "$ROOT_DIR/frontend/ai-assistant-consumer"
  : > "$LOG_DIR/consumer-ui.log"
  nohup npm run dev -- --host 0.0.0.0 --port 9001 --strictPort > "$LOG_DIR/consumer-ui.log" 2>&1 < /dev/null &
)

sleep 4

echo
echo "Started. Visit:"
echo "  manager ui   http://127.0.0.1:8001/"
echo "  consumer ui  http://127.0.0.1:9001/"
echo "  manager api  http://127.0.0.1:9000/manager/"
echo "  consumer api http://127.0.0.1:9000/consumer/"
echo
echo "Logs:"
echo "  $LOG_DIR/backend.log"
echo "  $LOG_DIR/manager-ui.log"
echo "  $LOG_DIR/consumer-ui.log"

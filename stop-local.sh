#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PORT="${BACKEND_PORT:-9010}"
WEB_PORT="${WEB_PORT:-9100}"

wait_port_release() {
  local port="$1"
  local i
  for i in $(seq 1 20); do
    if ! lsof -ti "tcp:${port}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "port ${port} 释放超时"
  return 1
}

pkill -f "$ROOT_DIR/backend/target/backend.jar" 2>/dev/null || true
pkill -f 'ai-assistant-web/node_modules/.bin/vite' 2>/dev/null || true

for port in "$BACKEND_PORT" "$WEB_PORT" 9000 8001 9001 9101 8101; do
  pid="$(lsof -ti tcp:$port || true)"
  if [ -n "$pid" ]; then
    echo "stopping port $port pid $pid"
    kill "$pid" || true
  fi
  wait_port_release "$port" || true
done

echo "done"

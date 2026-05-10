#!/usr/bin/env bash
set -euo pipefail

pkill -f '/Users/rengang/chuangye/osh-projects/ai-assistant/backend/target/backend.jar' 2>/dev/null || true
pkill -f 'ai-assistant-manager/node_modules/.bin/vite' 2>/dev/null || true
pkill -f 'ai-assistant-consumer/node_modules/.bin/vite' 2>/dev/null || true

for port in 9000 8001 9001; do
  pid="$(lsof -ti tcp:$port || true)"
  if [ -n "$pid" ]; then
    echo "stopping port $port pid $pid"
    kill "$pid" || true
  fi
done

echo "done"

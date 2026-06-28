#!/usr/bin/env bash
set -euo pipefail

CONTAINERS=(
  qa-assistant-backend-laughing
  qa-assistant-nginx-laughing
)

if [ "${1:-}" = "--watch" ]; then
  watch -n "${LAUGHING_WATCH_INTERVAL:-2}" "docker stats ${CONTAINERS[*]}"
  exit 0
fi

docker ps --filter "name=qa-assistant-.*-laughing" \
  --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo
docker stats --no-stream "${CONTAINERS[@]}"

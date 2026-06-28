#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUNTIME_DIR="${LAUGHING_RUNTIME_DIR:-$ROOT_DIR/.laughing-runtime}"
COMPOSE_PROJECT="${LAUGHING_COMPOSE_PROJECT:-qa-assistant-laughing}"

docker compose -f "$RUNTIME_DIR/docker-compose.yml" --project-name "$COMPOSE_PROJECT" down

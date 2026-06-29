#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${AI_ASSISTANT_DB_HOST:-43.242.200.25}"
DB_PORT="${AI_ASSISTANT_DB_PORT:-53306}"
DB_NAME="${AI_ASSISTANT_DB_NAME:-ai_assistant}"
DB_USERNAME="${AI_ASSISTANT_DB_USERNAME:-root}"
DB_PASSWORD="${AI_ASSISTANT_DB_PASSWORD:-321qwerty}"
SQL_FILE="${1:-deploy/server/initdb/003-rag-mvp-upgrade.sql}"

if ! command -v mysql >/dev/null 2>&1; then
  echo "missing command: mysql" >&2
  exit 1
fi

if [ ! -f "$SQL_FILE" ]; then
  echo "sql file not found: $SQL_FILE" >&2
  exit 1
fi

mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" < "$SQL_FILE"
echo "applied: $SQL_FILE"

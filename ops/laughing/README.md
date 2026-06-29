# Laughing app-only deployment

This profile runs only the QA Assistant application layer:

- `qa-assistant-backend-laughing`
- `qa-assistant-nginx-laughing`

It expects MySQL, Redis, Qdrant, embedding, rerank, and object storage to be external services configured through the runtime `.env`.

## Local verification

From the repository root:

```bash
./scripts/laughing-local-up.sh
```

The script stages a runtime directory at `.laughing-runtime/`, copies build artifacts into it, and starts the compose stack.

Default local ports:

- Web: `http://127.0.0.1:18088/ai/`
- Backend: `http://127.0.0.1:19088/auth/getCode`

Stop the local stack:

```bash
./scripts/laughing-local-down.sh
```

Check memory and container status:

```bash
./scripts/laughing-monitor.sh
./scripts/laughing-monitor.sh --watch
```

## Useful overrides

Set these before running `laughing-local-up.sh` when you want different local behavior:

```bash
LAUGHING_HTTP_PORT=18088
LAUGHING_BACKEND_PORT=19088
LAUGHING_BACKEND_MEMORY_LIMIT=1024m
LAUGHING_JAVA_MAX_RAM_PERCENTAGE=60
LAUGHING_SKIP_BUILD=1
```

For a smaller trial footprint:

```bash
LAUGHING_BACKEND_MEMORY_LIMIT=768m LAUGHING_JAVA_MAX_RAM_PERCENTAGE=55 ./scripts/laughing-local-up.sh
```

## Moving to the 25 server

Use the same runtime layout as `.laughing-runtime/` on the server, for example `/opt/qa-assistant-laughing`:

```text
/opt/qa-assistant-laughing
├── .env
├── backend.jar
├── docker-compose.yml
├── config/backend-prod-laughing.yml
├── html/ai/
├── logs/backend/
├── nginx/default.conf
└── upload/
```

The server `.env` should point to the existing MySQL, Redis, Qdrant, embedding, rerank, and storage services. Do not start duplicate stateful services unless the existing service is intentionally retired.

## Alert self-check

The laughing profile now supports a minimal alert self-check so we can verify SMTP and recipient wiring before a real outage.

For a local-only rehearsal without a real mailbox provider, you can enable the built-in Mailpit sandbox:

```bash
LAUGHING_ENABLE_ALERT_SANDBOX=1 ./scripts/laughing-local-up.sh
```

Then the local stack will additionally expose:

- Mail UI: `http://127.0.0.1:18025/`
- SMTP: `127.0.0.1:1025`

In this mode the script automatically injects a minimal local alert configuration into `.laughing-runtime/.env`, including:

- `AI_ASSISTANT_ALERT_ENABLED=true`
- `AI_ASSISTANT_ALERT_SELF_CHECK_ENABLED=true`
- `AI_ASSISTANT_ALERT_SELF_CHECK_ALLOWED_USERS=course_demo_user`
- `AI_ASSISTANT_ALERT_RECIPIENTS=codex-local@example.com`
- `AI_ASSISTANT_ALERT_SMTP_HOST=qa-assistant-mailpit-laughing`
- `AI_ASSISTANT_ALERT_SMTP_PORT=1025`

You can then verify end to end with:

```bash
./scripts/laughing-alert-self-check.sh http://127.0.0.1:19088
```

Expected local sandbox result:

- `GET /consumer/ops/alertReadiness` returns `status=READY`
- `POST /consumer/ops/alertSelfCheck` returns `status=SENT`
- Mailpit UI shows the self-check message
- backend logs contain `alert self check triggered`

Required runtime variables:

```bash
AI_ASSISTANT_ALERT_ENABLED=true
AI_ASSISTANT_ALERT_RECIPIENTS=owner@example.com,backup@example.com
AI_ASSISTANT_ALERT_SELF_CHECK_ENABLED=true
AI_ASSISTANT_ALERT_SELF_CHECK_ALLOWED_USERS=laughing
AI_ASSISTANT_ALERT_SMTP_HOST=smtp.example.com
AI_ASSISTANT_ALERT_SMTP_PORT=587
AI_ASSISTANT_ALERT_SMTP_USERNAME=alert@example.com
AI_ASSISTANT_ALERT_SMTP_PASSWORD=replace-me
```

After logging in as an allowed consumer user, trigger:

```bash
curl -X POST "http://127.0.0.1:19088/consumer/ops/alertSelfCheck" \
  -H "Authorization: <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"scene":"qdrant","note":"laughing 环境告警链路演练"}'
```

Expected result:

- API returns `status=SENT`
- recipients receive the self-check mail
- backend logs contain `alert self check triggered`

## Manual deploy script

For the local manual deploy helper:

1. Copy `.qaenv-laughing.env.example` to `.qaenv-laughing.env.local`
2. Fill in the real runtime values
3. Run `QAENV_DEPLOY_PASSWORD='...' ./scripts/qaenv-laughing-deploy.sh`

The `.qaenv-laughing.env.local` file is ignored by git and should stay local-only.

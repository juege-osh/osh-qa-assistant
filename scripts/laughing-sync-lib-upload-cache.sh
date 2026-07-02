#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.local}"
BASE_URL="${BASE_URL:-http://127.0.0.1:19088}"
RUNTIME_UPLOAD_DIR="${RUNTIME_UPLOAD_DIR:-$ROOT_DIR/.laughing-runtime/upload}"
CONSUMER_USER="${CONSUMER_USER:-course_demo_user}"
CONSUMER_PWD="${CONSUMER_PWD:-123456}"
LIB_NAME="${LIB_NAME:-RAG MVP 验收知识库}"
FILE_NAME_FILTER="${FILE_NAME_FILTER:-}"
OVERWRITE_EXISTING="${OVERWRITE_EXISTING:-0}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd python3

if [ ! -f "$ENV_FILE" ]; then
  echo "env file not found: $ENV_FILE" >&2
  exit 1
fi

mkdir -p "$RUNTIME_UPLOAD_DIR"

python3 - "$ENV_FILE" "$BASE_URL" "$RUNTIME_UPLOAD_DIR" "$CONSUMER_USER" "$CONSUMER_PWD" "$LIB_NAME" "$FILE_NAME_FILTER" "$OVERWRITE_EXISTING" <<'PY'
import json
import socket
import sys
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import urljoin

from botocore.config import Config
from botocore.session import get_session

(env_file, base_url, runtime_upload_dir, consumer_user, consumer_pwd,
 lib_name, file_name_filter, overwrite_existing) = sys.argv[1:]

overwrite_existing = overwrite_existing == "1"


def load_env(path):
    values = {}
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


env = load_env(env_file)


def redis_cmd(*parts):
    payload = f"*{len(parts)}\r\n".encode()
    for part in parts:
        data = str(part).encode()
        payload += f"${len(data)}\r\n".encode() + data + b"\r\n"
    return payload


def read_line(sock):
    data = b""
    while not data.endswith(b"\r\n"):
        chunk = sock.recv(1)
        if not chunk:
            raise RuntimeError("redis connection closed unexpectedly")
        data += chunk
    return data[:-2]


def read_resp(sock):
    prefix = sock.recv(1)
    if not prefix:
        raise RuntimeError("empty redis response")
    marker = prefix.decode()
    if marker == "+":
        return read_line(sock).decode()
    if marker == "-":
        raise RuntimeError(read_line(sock).decode())
    if marker == ":":
        return int(read_line(sock))
    if marker == "$":
        length = int(read_line(sock))
        if length == -1:
            return None
        data = b""
        while len(data) < length + 2:
            data += sock.recv(length + 2 - len(data))
        return data[:-2].decode()
    if marker == "*":
        count = int(read_line(sock))
        return [read_resp(sock) for _ in range(count)]
    raise RuntimeError(f"unsupported redis response: {marker}")


def redis_get(key):
    with socket.create_connection((env["AI_ASSISTANT_REDIS_HOST"], int(env["AI_ASSISTANT_REDIS_PORT"])), timeout=5) as sock:
        redis_password = env.get("AI_ASSISTANT_REDIS_PASSWORD", "")
        if redis_password:
            sock.sendall(redis_cmd("AUTH", redis_password))
            read_resp(sock)
        sock.sendall(redis_cmd("GET", key))
        raw = read_resp(sock)
    if raw is None:
        return None
    if len(raw) >= 2 and raw[0] == '"' and raw[-1] == '"':
        try:
            return json.loads(raw)
        except Exception:
            return raw.strip('"')
    return raw


def http_json(method, path, data=None, token=None):
    req = urllib.request.Request(urljoin(base_url, path), method=method)
    body = None
    if data is not None:
        req.add_header("Content-Type", "application/json")
        body = json.dumps(data).encode()
    if token:
        req.add_header("Authorization", token)
    try:
        with urllib.request.urlopen(req, data=body, timeout=30) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as exc:
        payload = exc.read().decode()
        raise RuntimeError(f"http error {exc.code} {path}: {payload}") from exc


def login():
    captcha_resp = http_json("GET", "/consumer/user/getCode")
    captcha_id = captcha_resp["data"]["captchaId"]
    code = redis_get(captcha_id)
    if not code:
        raise RuntimeError(f"captcha not found in redis: {captcha_id}")
    login_resp = http_json("POST", "/consumer/user/login", {
        "username": consumer_user,
        "pwd": consumer_pwd,
        "captchaId": captcha_id,
        "code": code,
    })
    if login_resp.get("code") != 200:
        raise RuntimeError(f"login failed: {login_resp}")
    return login_resp["data"]["token"]


def find_lib(token):
    resp = http_json("POST", "/consumer/knowledgeLib/queryPage", {"pageNow": 1, "pageSize": 100}, token)
    if resp.get("code") != 200:
        raise RuntimeError(f"query knowledge lib failed: {resp}")
    for item in resp.get("data") or []:
        if item.get("libName") == lib_name:
            return item
    raise RuntimeError(f"knowledge lib not found: {lib_name}")


def list_files(token, lib_id):
    resp = http_json("POST", "/consumer/uploadFile/queryPage", {
        "pageNow": 1,
        "pageSize": 200,
        "libId": lib_id,
    }, token)
    if resp.get("code") != 200:
        raise RuntimeError(f"query upload files failed: {resp}")
    files = resp.get("data") or []
    if file_name_filter:
        allowed = {item.strip() for item in file_name_filter.split(",") if item.strip()}
        files = [item for item in files if item.get("fileName") in allowed]
    return [item for item in files if item.get("status") == 1]


def build_s3():
    return get_session().create_client(
        "s3",
        region_name="auto",
        endpoint_url=env["AI_ASSISTANT_OSS_ENDPOINT"],
        aws_access_key_id=env["AI_ASSISTANT_OSS_ACCESS_KEY"],
        aws_secret_access_key=env["AI_ASSISTANT_OSS_SECRET_KEY"],
        config=Config(s3={"addressing_style": "path"}),
    )


token = login()
lib = find_lib(token)
files = list_files(token, lib["id"])
if not files:
    raise RuntimeError("no enabled files matched the current filter")

s3 = build_s3()
downloaded = 0
skipped = 0

print(f"libId={lib['id']} libName={lib['libName']} fileCount={len(files)}")

for item in files:
    store_path = item["storePath"].lstrip("/")
    dest_path = Path(runtime_upload_dir, store_path)
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    if dest_path.exists() and not overwrite_existing:
        skipped += 1
        print(f"skip  {item['fileName']} -> {dest_path}")
        continue
    response = s3.get_object(Bucket=env["AI_ASSISTANT_OSS_BUCKET_NAME"], Key=store_path)
    with dest_path.open("wb") as fh:
        fh.write(response["Body"].read())
    downloaded += 1
    print(f"sync  {item['fileName']} -> {dest_path}")

print(f"done downloaded={downloaded} skipped={skipped}")
PY

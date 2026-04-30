#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

REDIS_CONTAINER_NAME="insta-e2e-redis"
APP_PID=""
REDIS_STARTED_BY_SCRIPT="false"

cleanup() {
  if [[ -n "${APP_PID}" ]] && kill -0 "${APP_PID}" >/dev/null 2>&1; then
    kill "${APP_PID}" >/dev/null 2>&1 || true
    wait "${APP_PID}" 2>/dev/null || true
  fi

  if [[ "${REDIS_STARTED_BY_SCRIPT}" == "true" ]]; then
    docker stop "${REDIS_CONTAINER_NAME}" >/dev/null 2>&1 || true
    docker rm "${REDIS_CONTAINER_NAME}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] '$1' command is required."
    exit 1
  fi
}

require_command curl
require_command python3
require_command docker

if ! docker info >/dev/null 2>&1; then
  echo "[ERROR] Docker daemon is not running. Start Docker Desktop and retry."
  exit 1
fi

if docker ps --format '{{.Names}}' | rg -x "${REDIS_CONTAINER_NAME}" >/dev/null 2>&1; then
  echo "[INFO] Reusing running Redis container: ${REDIS_CONTAINER_NAME}"
elif docker ps -a --format '{{.Names}}' | rg -x "${REDIS_CONTAINER_NAME}" >/dev/null 2>&1; then
  echo "[INFO] Starting existing Redis container: ${REDIS_CONTAINER_NAME}"
  docker start "${REDIS_CONTAINER_NAME}" >/dev/null
  REDIS_STARTED_BY_SCRIPT="true"
else
  echo "[INFO] Creating Redis container: ${REDIS_CONTAINER_NAME}"
  docker run -d --name "${REDIS_CONTAINER_NAME}" -p 6379:6379 redis:7-alpine >/dev/null
  REDIS_STARTED_BY_SCRIPT="true"
fi

echo "[INFO] Starting Spring Boot app..."
./gradlew bootRun >/tmp/insta-e2e-app.log 2>&1 &
APP_PID=$!

echo "[INFO] Waiting for app readiness..."
for _ in $(seq 1 90); do
  code="$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "http://localhost:8080/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{}')"
  if [[ "${code}" != "000" ]]; then
    break
  fi
  sleep 1
done

if [[ "${code:-000}" == "000" ]]; then
  echo "[ERROR] App did not become ready. See /tmp/insta-e2e-app.log"
  exit 1
fi

TS="$(date +%s)"
EMAIL="e2e_${TS}@example.com"
USERNAME="e2euser_${TS}"
PASSWORD="ValidPass1!"
IMAGE_PATH="/tmp/e2e-upload-${TS}.png"

printf 'fake-png-content' > "${IMAGE_PATH}"

echo "[STEP 1] Signup"
signup_resp="$(curl -s -w '\n%{http_code}' \
  -X POST "http://localhost:8080/api/v1/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\",\"username\":\"${USERNAME}\"}")"
signup_body="$(printf '%s' "${signup_resp}" | sed '$d')"
signup_code="$(printf '%s' "${signup_resp}" | tail -n 1)"
echo "  HTTP ${signup_code}"
echo "  ${signup_body}"
[[ "${signup_code}" == "201" ]] || { echo "[ERROR] Signup failed."; exit 1; }

echo "[STEP 2] Login"
login_resp="$(curl -s -w '\n%{http_code}' \
  -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}")"
login_body="$(printf '%s' "${login_resp}" | sed '$d')"
login_code="$(printf '%s' "${login_resp}" | tail -n 1)"
echo "  HTTP ${login_code}"
echo "  ${login_body}"
[[ "${login_code}" == "200" ]] || { echo "[ERROR] Login failed."; exit 1; }

access_token="$(python3 -c "import json,sys; print(json.loads(sys.argv[1]).get('data',{}).get('accessToken',''))" "${login_body}")"
if [[ -z "${access_token}" ]]; then
  echo "[ERROR] Failed to parse access token."
  exit 1
fi

echo "[STEP 3] Create post (multipart)"
post_resp="$(curl -s -w '\n%{http_code}' \
  -X POST "http://localhost:8080/api/v1/posts" \
  -H "Authorization: Bearer ${access_token}" \
  -F "image=@${IMAGE_PATH};type=image/png" \
  -F "caption=E2E upload ${TS}")"
post_body="$(printf '%s' "${post_resp}" | sed '$d')"
post_code="$(printf '%s' "${post_resp}" | tail -n 1)"
echo "  HTTP ${post_code}"
echo "  ${post_body}"
[[ "${post_code}" == "201" ]] || { echo "[ERROR] Post create failed."; exit 1; }

echo
echo "[SUCCESS] E2E completed"
echo "- signup: 201"
echo "- login: 200"
echo "- post create: 201"

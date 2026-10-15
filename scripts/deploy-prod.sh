#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
BRANCH="${DEPLOY_BRANCH:-main}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yaml}"

echo "[deploy] project dir: ${PROJECT_DIR}"
echo "[deploy] branch: ${BRANCH}"
echo "[deploy] compose file: ${COMPOSE_FILE}"

cd "${PROJECT_DIR}"

echo "[deploy] syncing latest code..."
git fetch origin "${BRANCH}"
git checkout "${BRANCH}"
git pull --ff-only origin "${BRANCH}"

echo "[deploy] ensuring temp directories..."
mkdir -p tmp/code_output tmp/code_deploy tmp/covers certbot/www certbot/conf

echo "[deploy] ensuring TLS files exist so nginx can start..."
bash "${PROJECT_DIR}/scripts/ssl-certbot.sh" ensure-placeholder

echo "[deploy] pulling latest base images..."
docker compose -f "${COMPOSE_FILE}" pull

echo "[deploy] rebuilding and restarting services..."
docker compose -f "${COMPOSE_FILE}" up -d --build --remove-orphans

echo "[deploy] issuing or renewing Let's Encrypt certificates..."
if ! bash "${PROJECT_DIR}/scripts/ssl-certbot.sh" issue-or-renew; then
  echo "[deploy] TLS issue/renew failed; nginx will keep the previous or placeholder cert"
fi

echo "[deploy] reloading nginx to pick up certificates..."
if ! bash "${PROJECT_DIR}/scripts/ssl-certbot.sh" reload; then
  echo "[deploy] nginx reload skipped"
fi

echo "[deploy] removing dangling images..."
docker image prune -f

echo "[deploy] done."

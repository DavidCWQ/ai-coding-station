#!/usr/bin/env bash
# Let's Encrypt issue/renew helpers for compose.prod.yaml.
# Safe on every CD run: existing lineages only call `renew` (no-op until due).
set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yaml}"

cd "${PROJECT_DIR}"

if [[ -f "${PROJECT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${PROJECT_DIR}/.env"
  set +a
fi

PRIMARY_DOMAIN="${CERTBOT_PRIMARY_DOMAIN:-davidcwq.online}"
CERTBOT_DOMAINS="${CERTBOT_DOMAINS:-davidcwq.online,www.davidcwq.online}"
CERTBOT_CONF_DIR="${PROJECT_DIR}/certbot/conf"
CERTBOT_WWW_DIR="${PROJECT_DIR}/certbot/www"
LIVE_DIR="${CERTBOT_CONF_DIR}/live/${PRIMARY_DOMAIN}"
RENEWAL_CONF="${CERTBOT_CONF_DIR}/renewal/${PRIMARY_DOMAIN}.conf"

has_real_cert() {
  [[ -f "${RENEWAL_CONF}" && -f "${LIVE_DIR}/fullchain.pem" && ! -f "${LIVE_DIR}/.placeholder" ]]
}

write_placeholder() {
  mkdir -p "${LIVE_DIR}"
  if command -v openssl >/dev/null 2>&1; then
    openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
      -keyout "${LIVE_DIR}/privkey.pem" \
      -out "${LIVE_DIR}/fullchain.pem" \
      -subj "/CN=${PRIMARY_DOMAIN}"
  else
    docker run --rm -v "${CERTBOT_CONF_DIR}:/etc/letsencrypt" alpine:3.21 \
      sh -c "apk add --no-cache openssl >/dev/null && mkdir -p /etc/letsencrypt/live/${PRIMARY_DOMAIN} && openssl req -x509 -nodes -newkey rsa:2048 -days 1 -keyout /etc/letsencrypt/live/${PRIMARY_DOMAIN}/privkey.pem -out /etc/letsencrypt/live/${PRIMARY_DOMAIN}/fullchain.pem -subj /CN=${PRIMARY_DOMAIN}"
  fi
  touch "${LIVE_DIR}/.placeholder"
  echo "[ssl] wrote placeholder certificate for ${PRIMARY_DOMAIN}"
}

ensure_placeholder() {
  mkdir -p "${CERTBOT_WWW_DIR}" "${CERTBOT_CONF_DIR}"
  if has_real_cert; then
    echo "[ssl] existing Let's Encrypt certificate found"
    return 0
  fi
  if [[ -f "${LIVE_DIR}/fullchain.pem" && -f "${LIVE_DIR}/privkey.pem" ]]; then
    echo "[ssl] certificate files present; keeping them so nginx can start"
    return 0
  fi
  write_placeholder
}

wait_for_nginx() {
  local i
  for i in $(seq 1 15); do
    if docker compose -f "${COMPOSE_FILE}" exec -T nginx nginx -t >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "[ssl] nginx is not ready" >&2
  return 1
}

reload_nginx() {
  local i
  for i in $(seq 1 5); do
    if docker compose -f "${COMPOSE_FILE}" exec -T nginx nginx -s reload; then
      echo "[ssl] nginx reloaded"
      return 0
    fi
    sleep 2
  done
  echo "[ssl] nginx reload failed" >&2
  return 1
}

certbot_run() {
  docker compose -f "${COMPOSE_FILE}" run --rm --entrypoint certbot certbot "$@"
}

build_domain_args() {
  DOMAIN_ARGS=()
  local domain
  local seen_primary=0
  IFS=',' read -r -a domains <<< "${CERTBOT_DOMAINS}"
  for domain in "${domains[@]}"; do
    domain="${domain#"${domain%%[![:space:]]*}"}"
    domain="${domain%"${domain##*[![:space:]]}"}"
    [[ -z "${domain}" ]] && continue
    if [[ "${domain}" == "${PRIMARY_DOMAIN}" ]]; then
      seen_primary=1
    fi
    DOMAIN_ARGS+=(-d "${domain}")
  done
  if [[ "${seen_primary}" -ne 1 ]]; then
    DOMAIN_ARGS=(-d "${PRIMARY_DOMAIN}" "${DOMAIN_ARGS[@]}")
  fi
}

build_email_args() {
  EMAIL_ARGS=()
  if [[ -n "${CERTBOT_EMAIL:-}" ]]; then
    EMAIL_ARGS+=(--email "${CERTBOT_EMAIL}" --agree-tos --no-eff-email)
  else
    EMAIL_ARGS+=(--register-unsafely-without-email --agree-tos)
    echo "[ssl] CERTBOT_EMAIL is empty; registering without email" >&2
  fi
}

build_staging_args() {
  STAGING_ARGS=()
  if [[ "${CERTBOT_STAGING:-}" == "1" ]]; then
    STAGING_ARGS+=(--staging)
    echo "[ssl] using Let's Encrypt staging"
  fi
}

issue_or_renew() {
  wait_for_nginx
  build_domain_args
  build_email_args
  build_staging_args

  if has_real_cert; then
    echo "[ssl] renewing Let's Encrypt certificate (no-op if not due)"
    certbot_run renew --webroot -w /var/www/certbot
    return 0
  fi

  echo "[ssl] requesting a new Let's Encrypt certificate for ${CERTBOT_DOMAINS}"
  local backup
  backup="$(mktemp -d)"
  if [[ -d "${LIVE_DIR}" ]]; then
    cp -a "${LIVE_DIR}" "${backup}/live"
  fi

  rm -rf "${LIVE_DIR}" \
    "${CERTBOT_CONF_DIR}/archive/${PRIMARY_DOMAIN}" \
    "${RENEWAL_CONF}"

  if certbot_run certonly --webroot -w /var/www/certbot \
      --non-interactive --preferred-challenges http \
      "${EMAIL_ARGS[@]}" "${STAGING_ARGS[@]}" "${DOMAIN_ARGS[@]}"; then
    rm -rf "${backup}"
    echo "[ssl] issued Let's Encrypt certificate"
    return 0
  fi

  echo "[ssl] issue failed; restoring previous certificate files so the next compose up still works" >&2
  mkdir -p "$(dirname "${LIVE_DIR}")"
  if [[ -d "${backup}/live" ]]; then
    rm -rf "${LIVE_DIR}"
    mv "${backup}/live" "${LIVE_DIR}"
  else
    write_placeholder
  fi
  rm -rf "${backup}"
  return 1
}

case "${1:-issue-or-renew}" in
  ensure-placeholder)
    ensure_placeholder
    ;;
  issue-or-renew)
    issue_or_renew
    ;;
  reload)
    reload_nginx
    ;;
  *)
    echo "usage: $0 {ensure-placeholder|issue-or-renew|reload}" >&2
    exit 2
    ;;
esac

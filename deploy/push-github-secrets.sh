#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

usage() {
  cat <<'EOF'
Usage:
  ./push-github-secrets.sh [owner/repo]

Reads deploy/.env and pushes matching values into GitHub Actions secrets.
If owner/repo is omitted, gh uses the current repository context.
EOF
}

case "${1:-}" in
  -h|--help)
    usage
    exit 0
    ;;
esac

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) is required."
  exit 1
fi

if [ ! -f "${ENV_FILE}" ]; then
  echo "Missing ${ENV_FILE}"
  echo "Create it from ${SCRIPT_DIR}/.env.example first."
  exit 1
fi

REPO="${1:-}"
if [ -n "${REPO}" ]; then
  GH_REPO_ARGS=(--repo "${REPO}")
else
  GH_REPO_ARGS=()
fi

set -a
. "${ENV_FILE}"
set +a

required_vars=(
  SPRING_DATASOURCE_URL
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
  MINIO_URL
  MINIO_ACCESS_KEY
  MINIO_SECRET_KEY
  MINIO_BUCKET_NAME
)

for var in "${required_vars[@]}"; do
  if [ -z "${!var:-}" ]; then
    echo "Missing required variable in ${ENV_FILE}: ${var}"
    exit 1
  fi
done

secrets=(
  DEPLOY_HOST
  DEPLOY_PORT
  DEPLOY_USER
  DEPLOY_APP_DIR
  SPRING_DATASOURCE_URL
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
  SPRING_MAIL_HOST
  SPRING_MAIL_PORT
  SPRING_MAIL_USERNAME
  SPRING_MAIL_PASSWORD
  APP_MAIL_FROM
  MINIO_URL
  MINIO_ACCESS_KEY
  MINIO_SECRET_KEY
  MINIO_BUCKET_NAME
)

for secret_name in "${secrets[@]}"; do
  secret_value="${!secret_name:-}"
  if [ -n "${secret_value}" ]; then
    printf '%s' "${secret_value}" | gh secret set "${secret_name}" "${GH_REPO_ARGS[@]}"
    echo "Updated ${secret_name}"
  fi
done

echo "GitHub secrets push complete."

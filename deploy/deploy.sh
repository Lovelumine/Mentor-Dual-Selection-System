#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"
EXAMPLE_FILE="${SCRIPT_DIR}/.env.example"

usage() {
  cat <<'EOF'
Usage:
  ./deploy.sh           Build and deploy the backend with Docker Compose
  ./deploy.sh --check   Validate the Docker Compose configuration only
  ./deploy.sh --help    Show this help message
EOF
}

case "${1:-}" in
  -h|--help)
    usage
    exit 0
    ;;
  --check)
    mode="check"
    ;;
  "")
    mode="deploy"
    ;;
  *)
    echo "Unknown argument: ${1}"
    usage
    exit 1
    ;;
esac

if [ ! -f "${ENV_FILE}" ]; then
  echo "Missing ${ENV_FILE}"
  echo "Copy ${EXAMPLE_FILE} to ${ENV_FILE} and fill in the real values first."
  exit 1
fi

cd "${SCRIPT_DIR}"

if [ "${mode}" = "check" ]; then
  docker compose config >/dev/null
  echo "Docker Compose configuration is valid."
  exit 0
fi

docker compose up -d --build --remove-orphans

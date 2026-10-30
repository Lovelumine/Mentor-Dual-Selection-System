#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/Mentor-Dual-Selection-System"
FRONTEND_DIR="$ROOT_DIR/Mentor-Dual-Selection-System-Frontend"
DEPLOY_ENV="$ROOT_DIR/deploy/.env"
SITE_ROOT="/www/wwwroot/select.lumoxuan.cn"
NGINX_BIN="/www/server/nginx/sbin/nginx"
CONTAINER_NAME="mentor-dual-selection-system"
IMAGE_NAME="mentor-dual-selection-system:latest"
BRANCH="${1:-main}"

if [[ ! -f "$DEPLOY_ENV" ]]; then
  echo "Missing deploy env file: $DEPLOY_ENV" >&2
  exit 1
fi

echo "Updating repository..."
git -C "$ROOT_DIR" fetch origin
git -C "$ROOT_DIR" pull --ff-only origin "$BRANCH"

echo "Running backend tests..."
(cd "$BACKEND_DIR" && ./mvnw test)

echo "Building frontend..."
(cd "$FRONTEND_DIR" && pnpm install --frozen-lockfile && pnpm build)

echo "Syncing frontend assets to $SITE_ROOT ..."
sudo mkdir -p "$SITE_ROOT"
sudo rsync -a --delete --exclude='.well-known/' "$FRONTEND_DIR/dist/" "$SITE_ROOT/"
sudo chown -R www:www "$SITE_ROOT"

echo "Building backend image..."
docker build -t "$IMAGE_NAME" "$BACKEND_DIR"

echo "Recreating production backend container..."
docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
docker run -d \
  --name "$CONTAINER_NAME" \
  --restart unless-stopped \
  --env-file "$DEPLOY_ENV" \
  -p 50000:50000 \
  "$IMAGE_NAME" >/dev/null

echo "Reloading Nginx..."
sudo "$NGINX_BIN" -t
sudo "$NGINX_BIN" -s reload

echo "Waiting for backend health check..."
for _ in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:50000/hello >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "Verifying deployment..."
curl -fsS http://127.0.0.1:50000/hello >/dev/null
curl -kfsS --resolve select.lumoxuan.cn:443:127.0.0.1 https://select.lumoxuan.cn/hello >/dev/null

echo "Update completed successfully."

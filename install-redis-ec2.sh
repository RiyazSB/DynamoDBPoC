#!/usr/bin/env bash
set -euo pipefail

# Installs Redis on Amazon Linux (AL2023/AL2), starts the service,
# and validates connectivity with PING.

if [[ "${EUID}" -ne 0 ]]; then
  echo "Please run as root: sudo bash install-redis-ec2.sh"
  exit 1
fi

echo "[1/5] Installing Redis package..."
if dnf install -y redis6; then
  REDIS_SERVICE="redis6"
  REDIS_CLI="/usr/bin/redis6-cli"
  REDIS_SERVER_BIN="/usr/bin/redis6-server"
elif dnf install -y redis; then
  REDIS_SERVICE="redis"
  REDIS_CLI="/usr/bin/redis-cli"
  REDIS_SERVER_BIN="/usr/bin/redis-server"
else
  echo "Failed to install Redis package (tried redis6 and redis)."
  exit 1
fi

echo "[2/5] Enabling and starting Redis service..."
systemctl enable "${REDIS_SERVICE}"
systemctl restart "${REDIS_SERVICE}"

echo "[3/5] Verifying service status..."
systemctl --no-pager --full status "${REDIS_SERVICE}" | sed -n '1,20p'

echo "[4/5] Creating redis-cli convenience symlink (if needed)..."
if [[ ! -x /usr/local/bin/redis-cli ]]; then
  ln -sf "${REDIS_CLI}" /usr/local/bin/redis-cli
fi
if [[ -x "${REDIS_SERVER_BIN}" && ! -x /usr/local/bin/redis-server ]]; then
  ln -sf "${REDIS_SERVER_BIN}" /usr/local/bin/redis-server
fi

echo "[5/5] Testing Redis connection..."
PONG_OUTPUT="$(${REDIS_CLI} ping || true)"
if [[ "${PONG_OUTPUT}" != "PONG" ]]; then
  echo "Redis ping failed. Output: ${PONG_OUTPUT}"
  exit 1
fi

echo "Redis installed and running successfully."
echo "CLI path: ${REDIS_CLI}"
echo "Try: redis-cli KEYS \"*\""


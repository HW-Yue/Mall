#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/docker-compose-apps.yml"

MODULES=(
  "mall/mall-app"
  "order-service/order-service-app"
  "group-buy-service/group-buy-service-app"
  "seckill-service/seckill-service-app"
  "pay/pay-app"
  "springcloud-gateway/app"
  "ops-agent-spring-ai"
)

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

cd "${ROOT_DIR}"

if [[ "${SKIP_MAVEN:-false}" != "true" ]]; then
  echo "[1/2] Packaging application jars..."
  mvn -pl "$(IFS=,; echo "${MODULES[*]}")" -am clean package -DskipTests
else
  echo "[1/2] SKIP_MAVEN=true, using existing target jars."
fi

echo "[2/2] Building Docker images..."
compose -f "${COMPOSE_FILE}" build "$@"

echo "Done. Images are tagged with IMAGE_TAG=${IMAGE_TAG:-local}."

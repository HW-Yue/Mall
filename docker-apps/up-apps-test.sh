#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/dev-ops/docker-compose-apps-test.yml"

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

cd "${ROOT_DIR}"
compose -f "${COMPOSE_FILE}" up -d "$@"
compose -f "${COMPOSE_FILE}" ps

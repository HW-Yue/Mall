#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"
K6_WEB_DASHBOARD=true \
K6_WEB_DASHBOARD_PORT=5665 \
MODE=goods \
PRESET=rps-10k-50k \
EXECUTOR_MODE=ramping-arrival-rate \
TIME_UNIT=1s \
PRE_ALLOCATED_VUS=10000 \
MAX_VUS=60000 \
exec k6 run k6/seckill-full-flow.js

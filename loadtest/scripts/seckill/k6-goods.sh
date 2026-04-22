#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"
K6_WEB_DASHBOARD=true \
K6_WEB_DASHBOARD_PORT=5665 \
MODE=goods \
exec k6 run k6/seckill-full-flow.js

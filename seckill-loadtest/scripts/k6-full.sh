#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

export K6_WEB_DASHBOARD="${K6_WEB_DASHBOARD:-true}"
export K6_WEB_DASHBOARD_PORT="${K6_WEB_DASHBOARD_PORT:-5665}"
export MODE="${MODE:-full}"
export FULL_FLOW_IS_TEST="${FULL_FLOW_IS_TEST:-false}"

exec k6 run k6/seckill-full-flow.js "$@"

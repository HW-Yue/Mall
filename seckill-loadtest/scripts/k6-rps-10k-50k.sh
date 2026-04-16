#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

export K6_WEB_DASHBOARD="${K6_WEB_DASHBOARD:-true}"
export K6_WEB_DASHBOARD_PORT="${K6_WEB_DASHBOARD_PORT:-5665}"

export MODE="${MODE:-goods}"
export EXECUTOR_MODE="${EXECUTOR_MODE:-ramping-arrival-rate}"
export PRESET="${PRESET:-rps-10k-50k}"
export TIME_UNIT="${TIME_UNIT:-1s}"
export PRE_ALLOCATED_VUS="${PRE_ALLOCATED_VUS:-10000}"
export MAX_VUS="${MAX_VUS:-60000}"

exec k6 run k6/seckill-full-flow.js "$@"

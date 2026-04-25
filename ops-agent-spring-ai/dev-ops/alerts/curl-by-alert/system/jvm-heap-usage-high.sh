#!/usr/bin/env bash
# 告警: JvmHeapUsageHigh
# 来源 category: system
# 依赖: dev-ops/alerts/samples/alert-receive-bodies.json

set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export BODIES_JSON="${SCRIPT_DIR}/../../samples/alert-receive-bodies.json"
BASE_URL="${BASE_URL:-http://127.0.0.1:2322}"

curl -sS -X POST "${BASE_URL}/api/v1/alert/receive" \
  -H 'Content-Type: application/json' \
  -d "$(ALERT_KEY='JvmHeapUsageHigh' python3 -c "import json,os; p=os.environ['BODIES_JSON']; k=os.environ['ALERT_KEY']; d=json.load(open(p,encoding='utf-8')); print(json.dumps(d[k], ensure_ascii=False))")"
echo ""

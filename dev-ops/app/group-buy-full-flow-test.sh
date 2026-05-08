#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://127.0.0.1:8090}"
GROUP_BUY_PREFIX="${GROUP_BUY_PREFIX:-/gw/api/v1/group-buy}"
ORDER_PREFIX="${ORDER_PREFIX:-/gw/api/v1/order}"

SOURCE="${SOURCE:-s01}"
CHANNEL="${CHANNEL:-c01}"
ACTIVITY_ID="${ACTIVITY_ID:-100123}"
PRODUCT_ID="${PRODUCT_ID:-1001}"

RUN_ID="${RUN_ID:-$(date +%Y%m%d%H%M%S)}"
SHORT_RUN_ID="${SHORT_RUN_ID:-$(date +%M%S)$((RANDOM % 10))}"
USER1="${USER1:-u${SHORT_RUN_ID}a}"
USER2="${USER2:-u${SHORT_RUN_ID}b}"
OUT_TRADE_NO1="${OUT_TRADE_NO1:-GB${RUN_ID}A}"
OUT_TRADE_NO2="${OUT_TRADE_NO2:-GB${RUN_ID}B}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "missing command: $1" >&2
    exit 1
  }
}

json_get() {
  local json="$1"
  local path="$2"
  python3 - "$json" "$path" <<'PY'
import json
import sys

body = json.loads(sys.argv[1])
value = body
for part in sys.argv[2].split("."):
    value = value.get(part) if isinstance(value, dict) else None
    if value is None:
        break
print("" if value is None else value)
PY
}

post_group_buy_json() {
  local path="$1"
  local payload="$2"
  curl -sS \
    -H 'Content-Type: application/json' \
    -X POST \
    "${API_BASE_URL}${GROUP_BUY_PREFIX}${path}" \
    -d "${payload}"
}

post_order_json() {
  local path="$1"
  local payload="$2"
  curl -sS \
    -H 'Content-Type: application/json' \
    -X POST \
    "${API_BASE_URL}${ORDER_PREFIX}${path}" \
    -d "${payload}"
}

assert_success() {
  local name="$1"
  local body="$2"
  local code
  code="$(json_get "$body" "code")"
  if [[ "$code" != "0000" ]]; then
    echo "${name} failed: ${body}" >&2
    exit 1
  fi
}

get_pay_html() {
  local user_id="$1"
  local order_id="$2"
  local name="$3"
  local request
  local response
  local pay_html

  request="$(cat <<JSON
{"userId":"${user_id}","orderId":"${order_id}"}
JSON
)"
  response="$(post_order_json "/get_pay_url" "$request")"
  assert_success "${name} pay url" "$response"
  pay_html="$(json_get "$response" "data")"
  if [[ -z "$pay_html" ]]; then
    echo "${name} pay url response missing html: ${response}" >&2
    exit 1
  fi
  echo "${name} pay html length=${#pay_html}"
}

wait_http() {
  local url="$1"
  local name="$2"
  local i
  for i in $(seq 1 60); do
    if curl -fsS -m 2 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "timeout waiting for ${name}: ${url}" >&2
  exit 1
}

wait_group_buy_route() {
  local i
  local body
  local code
  for i in $(seq 1 60); do
    body="$(curl -sS -m 3 "${API_BASE_URL}${GROUP_BUY_PREFIX}/market/query_goods_list" 2>/dev/null || true)"
    if [[ -n "$body" ]]; then
      code="$(json_get "$body" "code" 2>/dev/null || true)"
      if [[ "$code" == "0000" ]]; then
        return 0
      fi
    fi
    sleep 2
  done
  echo "timeout waiting for group-buy route" >&2
  exit 1
}

require_cmd curl
require_cmd python3

echo "[1/5] waiting for required services..."
wait_group_buy_route
wait_http "http://127.0.0.1:8090/actuator/health" "gateway"
wait_http "http://127.0.0.1:8093/actuator/health" "group-buy-service"
wait_http "http://127.0.0.1:8092/actuator/health" "order-service"
wait_http "http://127.0.0.1:8095/actuator/health" "pay"

echo "[2/5] create first group-buy order..."
REQ1="$(cat <<JSON
{"userId":"${USER1}","productId":"${PRODUCT_ID}","activityId":${ACTIVITY_ID},"source":"${SOURCE}","channel":"${CHANNEL}","outTradeNo":"${OUT_TRADE_NO1}"}
JSON
)"
RESP1="$(post_group_buy_json "/trade/create_pay_order" "$REQ1")"
assert_success "first order" "$RESP1"
ORDER_ID1="$(json_get "$RESP1" "data.orderId")"
TEAM_ID="$(json_get "$RESP1" "data.teamId")"

if [[ -z "$ORDER_ID1" || -z "$TEAM_ID" ]]; then
  echo "first order response missing orderId/teamId: ${RESP1}" >&2
  exit 1
fi

echo "first orderId=${ORDER_ID1} teamId=${TEAM_ID}"

echo "[3/5] request first payment html from order-service..."
get_pay_html "$USER1" "$ORDER_ID1" "first order"

echo "[4/5] create second group-buy order with returned teamId..."
REQ2="$(cat <<JSON
{"userId":"${USER2}","productId":"${PRODUCT_ID}","teamId":"${TEAM_ID}","activityId":${ACTIVITY_ID},"source":"${SOURCE}","channel":"${CHANNEL}","outTradeNo":"${OUT_TRADE_NO2}"}
JSON
)"
RESP2="$(post_group_buy_json "/trade/create_pay_order" "$REQ2")"
assert_success "second order" "$RESP2"
ORDER_ID2="$(json_get "$RESP2" "data.orderId")"
TEAM_ID2="$(json_get "$RESP2" "data.teamId")"

if [[ -z "$ORDER_ID2" || "$TEAM_ID2" != "$TEAM_ID" ]]; then
  echo "second order team mismatch: ${RESP2}" >&2
  exit 1
fi

echo "second orderId=${ORDER_ID2} teamId=${TEAM_ID2}"

echo "[5/5] request second payment html from order-service..."
get_pay_html "$USER2" "$ORDER_ID2" "second order"

echo "[done] success"
echo "order1: user=${USER1} orderId=${ORDER_ID1} outTradeNo=${OUT_TRADE_NO1}"
echo "order2: user=${USER2} orderId=${ORDER_ID2} outTradeNo=${OUT_TRADE_NO2}"

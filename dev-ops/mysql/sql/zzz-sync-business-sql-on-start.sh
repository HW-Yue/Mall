#!/usr/bin/env bash
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-mysql}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}"

ROOT_SQLS=(
  "/work/sql/mall_db.sql"
  "/work/sql/order_service.sql"
  "/work/sql/group_buy_service.sql"
  "/work/sql/seckill_service.sql"
  "/work/sql/s-pay-mall-ddd-market.sql"
)

TEST_SQL_GLOB="/work/sql/test/*.sql"

run_sql() {
  local sql_file="$1"
  echo "[mysql-sync] running ${sql_file}"
  mysql -h"${MYSQL_HOST}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" < "${sql_file}"
}

echo "[mysql-sync] syncing business databases on container startup"

for sql_file in "${ROOT_SQLS[@]}"; do
  if [[ -f "${sql_file}" ]]; then
    run_sql "${sql_file}"
  else
    echo "[mysql-sync] skip missing ${sql_file}"
  fi
done

for sql_file in ${TEST_SQL_GLOB}; do
  if [[ -f "${sql_file}" ]]; then
    run_sql "${sql_file}"
  fi
done

echo "[mysql-sync] business and test databases synced"

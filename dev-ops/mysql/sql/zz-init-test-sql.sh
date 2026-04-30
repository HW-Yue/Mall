#!/usr/bin/env bash
set -euo pipefail

echo "[mysql-init] loading test sql files from /docker-entrypoint-initdb.d/test"

for sql_file in /docker-entrypoint-initdb.d/test/*.sql; do
  if [ ! -f "${sql_file}" ]; then
    echo "[mysql-init] no test sql files found"
    exit 0
  fi

  echo "[mysql-init] running ${sql_file}"
  mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" < "${sql_file}"
done

echo "[mysql-init] test sql files loaded"

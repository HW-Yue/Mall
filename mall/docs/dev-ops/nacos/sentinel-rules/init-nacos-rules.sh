#!/usr/bin/env bash
#
# 把当前目录下所有 *-rules.json 批量发布到 Nacos（Group = SENTINEL_GROUP）
# 用法：
#   NACOS_ADDR=100.86.250.112:8848 ./init-nacos-rules.sh
# 可选：
#   NAMESPACE=public NACOS_USER=nacos NACOS_PASS=nacos ./init-nacos-rules.sh
#
# Nacos 3.x：配置发布使用 Admin API（/v3/admin/cs/config）；旧版 /v1/cs/configs 在 3.2+ 默认不可用会 404。
# Admin API 鉴权（nacos.core.auth.admin.enabled）与 NACOS_AUTH_ENABLE 独立；若未关闭 Admin 鉴权且未带 Token 会 HTTP 403。
# 本地 compose 已设 NACOS_AUTH_ADMIN_ENABLE=false；生产开启 Admin 鉴权时请：NACOS_USER=nacos NACOS_PASS=*** ./init-nacos-rules.sh
set -euo pipefail

NACOS_ADDR="${NACOS_ADDR:-100.86.250.112:8848}"
NAMESPACE="${NAMESPACE:-public}"
GROUP="${GROUP:-SENTINEL_GROUP}"
NACOS_USER="${NACOS_USER:-}"
NACOS_PASS="${NACOS_PASS:-}"

BASE_URL="http://${NACOS_ADDR}/nacos/v3/admin/cs/config"
CURL_TOKEN=()

if [[ -n "${NACOS_USER}" && -n "${NACOS_PASS}" ]]; then
  echo "[info] 请求 Nacos accessToken ..."
  RAW=$(curl -s -X POST "http://${NACOS_ADDR}/nacos/v3/auth/user/login" \
    -d "username=${NACOS_USER}&password=${NACOS_PASS}")
  TOKEN=$(echo "${RAW}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
  if [[ -z "${TOKEN}" ]]; then
    RAW=$(curl -s -X POST "http://${NACOS_ADDR}/nacos/v1/auth/login" \
      -d "username=${NACOS_USER}&password=${NACOS_PASS}")
    TOKEN=$(echo "${RAW}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
  fi
  if [[ -z "${TOKEN}" ]]; then
    echo "[error] 获取 accessToken 失败，请检查账号密码"
    exit 1
  fi
  CURL_TOKEN=(-H "accessToken: ${TOKEN}")
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

shopt -s nullglob
FILES=( *-rules.json )
if (( ${#FILES[@]} == 0 )); then
  echo "[warn] 当前目录没有 *-rules.json"
  exit 0
fi

for f in "${FILES[@]}"; do
  dataId="${f}"     # 文件名即 DataId，与 application-dev.yml 中 datasource.xxx.dataId 对齐
  content=$(cat "${f}")
  # 依 data-type: json，直接把 JSON 作为 content 发布
  echo "[pub] ${dataId}  group=${GROUP}  ns=${NAMESPACE}"
  curl -s -o /dev/null -w "  -> HTTP %{http_code}\n" -X POST "${BASE_URL}" \
    "${CURL_TOKEN[@]}" \
    --data-urlencode "namespaceId=${NAMESPACE}" \
    --data-urlencode "groupName=${GROUP}" \
    --data-urlencode "dataId=${dataId}" \
    --data-urlencode "type=json" \
    --data-urlencode "content=${content}"
done

echo "[done] 已发布 ${#FILES[@]} 个规则文件到 Nacos ${NACOS_ADDR} group=${GROUP}"

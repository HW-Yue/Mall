#!/usr/bin/env bash
#
# 递归扫描当前目录及子目录下与「运行时配置」相关的 YAML，批量发布到 Nacos（Group = DEFAULT_GROUP）。
# 包含：DynamicTp（*-dtp-dev.yml）、可选 Hikari 覆盖（*-datasource-dev.yml）、
#       运行时参数（*-runtime-dev.yml，如 group-buy-service-runtime-dev.yml / pay-service-runtime-dev.yml）。
#
# 覆盖的 DataId（文件名即 DataId，与各服务 application-dev.yml 中 optional:nacos:... 一致）：
#   *-dtp-dev.yml           — spring.dynamic.tp（DynamicTp + Tomcat 适配）
#   *-datasource-dev.yml    — spring.datasource.hikari.*（可选，配合 HikariPoolDynamicRefresher）
#   *-runtime-dev.yml       — 日志 / Agent / Feign / 自定义线程池等运行时参数
#
# 用法：
#   NACOS_ADDR=100.86.250.112:8848 ./init-nacos-dtp.sh
# 可选：
#   NAMESPACE=public NACOS_USER=nacos NACOS_PASS=nacos ./init-nacos-dtp.sh
#
# Nacos 3.x：Admin API 鉴权与 NACOS_AUTH_ENABLE 独立；未关闭 Admin 鉴权且无 Token 会 403。见 docker-compose-environment.yml：NACOS_AUTH_ADMIN_ENABLE。
# 配置发布使用 Admin API：POST /nacos/v3/admin/cs/config（勿再用已废弃的 /v1/cs/configs，否则 404）。
set -euo pipefail

NACOS_ADDR="${NACOS_ADDR:-100.86.250.112:8848}"
NAMESPACE="${NAMESPACE:-public}"
GROUP="${GROUP:-DEFAULT_GROUP}"
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

mapfile -t FILES < <(
  find . -type f \( -name '*-dtp-dev.yml' -o -name '*-datasource-dev.yml' -o -name '*-runtime-dev.yml' \) | sort
)
if (( ${#FILES[@]} == 0 )); then
  echo "[warn] 当前目录及子目录没有 *-dtp-dev.yml、*-datasource-dev.yml 或 *-runtime-dev.yml"
  exit 0
fi

for f in "${FILES[@]}"; do
  [[ -f "${f}" ]] || continue
  dataId="$(basename "${f}")"
  content=$(cat "${f}")
  echo "[pub] ${dataId}  path=${f#./}  group=${GROUP}  ns=${NAMESPACE}  type=yaml"
  curl -s -o /dev/null -w "  -> HTTP %{http_code}\n" -X POST "${BASE_URL}" \
    "${CURL_TOKEN[@]}" \
    --data-urlencode "namespaceId=${NAMESPACE}" \
    --data-urlencode "groupName=${GROUP}" \
    --data-urlencode "dataId=${dataId}" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content=${content}"
done

echo "[done] 已发布 ${#FILES[@]} 个配置到 Nacos ${NACOS_ADDR} group=${GROUP}"

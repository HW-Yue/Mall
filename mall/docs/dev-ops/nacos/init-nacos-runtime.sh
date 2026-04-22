#!/usr/bin/env bash
#
# 统一入口：发布「运行时」相关的全部 Nacos 配置（YAML 到 DEFAULT_GROUP + Sentinel JSON 到 SENTINEL_GROUP）。
# 等价于依次执行 dtp-config/init-nacos-dtp.sh 与 sentinel-rules/init-nacos-rules.sh。
#
# 用法：
#   NACOS_ADDR=127.0.0.1:8848 ./init-nacos-runtime.sh
#   NACOS_ADDR=127.0.0.1:8848 NACOS_USER=nacos NACOS_PASS=nacos ./init-nacos-runtime.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== [1/2] YAML（DynamicTp / Hikari / *-runtime-dev）→ DEFAULT_GROUP ==="
"${SCRIPT_DIR}/dtp-config/init-nacos-dtp.sh"

echo "=== [2/2] Sentinel *-rules.json → SENTINEL_GROUP ==="
"${SCRIPT_DIR}/sentinel-rules/init-nacos-rules.sh"

echo "[done] init-nacos-runtime.sh：YAML + Sentinel 均已尝试发布（见上方 HTTP 状态）"

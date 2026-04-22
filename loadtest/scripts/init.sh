#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

python3 -m venv .venv
.venv/bin/pip install -r requirements.txt

echo "初始化完成：$ROOT_DIR/.venv 已创建，依赖已安装。"

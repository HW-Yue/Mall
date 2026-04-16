# k6 压测说明（默认启用 Dashboard）

本文档仅覆盖 `k6` 用法。  
如果你要看 Python/Locust，请看 `readme-locust.md`。

## 目录结构

- `k6/seckill-full-flow.js`：k6 主脚本（`goods` / `lock` / `full` 三种模式）
- `scripts/k6-goods.sh`：商品列表压测（默认开 dashboard）
- `scripts/k6-lock.sh`：锁单压测（默认开 dashboard）
- `scripts/k6-full.sh`：全链路压测（默认开 dashboard）
- `scripts/k6-100k.sh`：10 万级压测预设（默认开 dashboard）
- `scripts/k6-rps-10k-50k.sh`：查询接口 RPS 阶梯压测（1万 -> 5万，默认开 dashboard）

## 先安装 k6（Ubuntu / Debian）

```bash
sudo apt-get update
sudo apt-get install -y gnupg ca-certificates
curl -fsSL https://dl.k6.io/key.gpg | sudo gpg --dearmor -o /usr/share/keyrings/k6-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list > /dev/null
sudo apt-get update
sudo apt-get install -y k6
k6 version
```

## Dashboard（默认开启）

所有 `scripts/k6-*.sh` 都默认导出以下变量：

- `K6_WEB_DASHBOARD=true`
- `K6_WEB_DASHBOARD_PORT=5665`

运行后可在浏览器访问：`http://localhost:5665`

## 一键运行（推荐）

```bash
# 商品列表
./scripts/k6-goods.sh

# 锁单（默认 isTest=true，不走 MQ）
./scripts/k6-lock.sh

# 全链路（默认 isTest=false）
./scripts/k6-full.sh

# 查询接口：RPS 从 1万 逐步升到 5万
./scripts/k6-rps-10k-50k.sh
```

## 10 万压测

```bash
# 默认：10万并发 VU 爬升
./scripts/k6-100k.sh

# 改成 10万 RPS 冲击
EXECUTOR_MODE=constant-arrival-rate ./scripts/k6-100k.sh
```

## 查询接口 1万 -> 5万 RPS 阶梯压测

```bash
# 默认使用 ramping-arrival-rate，自动分阶段升压：
# 10k -> 20k -> 30k -> 40k -> 50k -> 回落到 0
./scripts/k6-rps-10k-50k.sh
```

默认阶段（可覆盖）：

- `STAGE_1_TARGET=10000`，`STAGE_1_DURATION=2m`
- `STAGE_2_TARGET=20000`，`STAGE_2_DURATION=2m`
- `STAGE_3_TARGET=30000`，`STAGE_3_DURATION=2m`
- `STAGE_4_TARGET=40000`，`STAGE_4_DURATION=2m`
- `STAGE_5_TARGET=50000`，`STAGE_5_DURATION=2m`
- `STAGE_6_TARGET=0`，`STAGE_6_DURATION=1m`

自定义示例（把每档改成 3 分钟）：

```bash
STAGE_1_DURATION=3m \
STAGE_2_DURATION=3m \
STAGE_3_DURATION=3m \
STAGE_4_DURATION=3m \
STAGE_5_DURATION=3m \
./scripts/k6-rps-10k-50k.sh
```

## 常用参数覆盖

```bash
# 换网关地址
BASE_URL=http://100.86.250.112:8090 ./scripts/k6-lock.sh

# 指定活动和商品
ACTIVITY_ID=200001 PRODUCT_ID=1001 ./scripts/k6-lock.sh

# 全链路下调轮询参数
POLL_RETRIES=6 POLL_INTERVAL_SEC=0.3 ./scripts/k6-full.sh
```

## 核心环境变量

- `MODE`：`goods` / `lock` / `full`
- `PRESET`：`default` / `100k` / `rps-10k-50k`
- `EXECUTOR_MODE`：`ramping-vus` / `constant-arrival-rate` / `ramping-arrival-rate`
- `BASE_URL`：网关地址（默认 `http://100.86.250.112:8090`）
- `LOCK_ONLY_IS_TEST`：`lock` 模式是否走测试分支（默认 `true`）
- `FULL_FLOW_IS_TEST`：`full` 模式是否走测试分支（默认 `false`）
- `RATE`、`TIME_UNIT`、`DURATION`：到达率模式参数
- `START_RATE`、`STAGE_n_TARGET`、`STAGE_n_DURATION`：阶梯 RPS 模式参数
- `PRE_ALLOCATED_VUS`、`MAX_VUS`：到达率模式 VU 池

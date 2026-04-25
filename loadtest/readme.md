# Nexus 统一压测目录

所有服务的压测脚本集中在这里，按服务分子目录。当前包含：

- `seckill` — 秒杀服务：商品列表 / 锁单 / 全链路 / Mock 支付宝
- `mall` — 商城服务：类目列表 / 商品分页查询

Python 压测基于 [Locust](https://docs.locust.io/)，另保留一份 k6 脚本用于需要极高 QPS 的秒杀场景。

## 目录结构

```
loadtest/
├── Makefile                 # 统一入口，make <service>-<scene>
├── requirements.txt         # Python 依赖（所有服务共用）
├── scripts/                 # shell 启动脚本，按服务分目录
│   ├── init.sh
│   ├── seckill/
│   └── mall/
├── locust/
│   ├── common.py            # 跨服务共享：网关地址、userId 生成器
│   ├── seckill/             # 秒杀专属 config + 脚本
│   └── mall/                # 商城专属 config + 脚本
└── k6/
    └── seckill-full-flow.js
```

## 首次初始化（所有服务共用一个虚拟环境）

```bash
./scripts/init.sh
# 或
make init
```

## 启动方式

### Locust

```bash
# 秒杀
make seckill-mock            # mock 支付宝，端口 7000
make seckill-goods           # 商品列表查询，Web UI :7321
make seckill-lock            # 锁单（isTest=true），Web UI :7321
make seckill-full            # 全链路（锁单+轮询+支付URL），Web UI :7321

# 商城
make mall-category           # 类目列表查询，Web UI :7322
make mall-goods              # 商品分页查询，Web UI :7322
```

### k6（仅秒杀场景保留）

```bash
make seckill-k6-goods
make seckill-k6-lock
make seckill-k6-full
make seckill-k6-100k         # 大压力 ramping-vus
make seckill-k6-rps-10k-50k  # 固定 RPS 渐增
```

## Locust Web UI 端口分配

| 服务 | 端口 |
|------|------|
| seckill | 7321 |
| mall    | 7322 |

两个端口错开是为了可以并行跑压测。

## 和 Prometheus 告警链路联动

- Sentinel 指标暴露：需要先打一次业务请求，`/actuator/prometheus` 才会出现 `sentinel_*`。本压测脚本即满足该前置条件。
- 资源名命名规范：Sentinel resource 是**纯 URI**（如 `/api/v1/mall/index/query_goods_page`），配 Nacos 流控规则时 `resource` 字段按此格式写。
- 触发告警最快路径：在 Nacos 里把某接口的 QPS 阈值调低（如 mall 的 `query_goods_page` 配 QPS=10），`make mall-goods` 启动后用 Locust Web UI 调到 100+ Users，约 1 分钟即可触发 `MallServiceQueryGoodsPageBlockHigh`，通过 Alertmanager 推到 ops-agent-spring-ai 的 `/api/v1/alert/receive`。

## 常见问题

- `.venv/bin/python` 不存在 → 先执行 `make init` 或 `./scripts/init.sh`。
- 端口占用 → 结束旧 Locust / k6 进程后重试。
- mock 支付宝：只在 `seckill-full` 场景需要，跑之前先 `make seckill-mock`，并确保 `pay` 服务的 `application-dev.yml` 中 `alipay.gateway-url` 指向 `http://100.86.250.112:7000`。

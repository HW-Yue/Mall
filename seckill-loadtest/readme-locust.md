# Locust 压测说明（Python）

本文档仅覆盖 `Locust`（Python）用法。  
如果你要看 k6，请看 `readme-k6.md`。

## 目录结构

- `locust/config.py`：公共配置（网关地址、接口路径、活动与商品 ID）
- `locust/seckill/goods_query.py`：商品列表查询压测
- `locust/seckill/lock_only.py`：锁单压测（`isTest=true`，不走 MQ）
- `locust/full_flow/seckill_to_pay.py`：全链路（锁单 -> 轮询建单 -> 获取支付链接）
- `locust/mock/alipay.py`：mock 支付宝

## 首次初始化（只需一次）

```bash
./scripts/init.sh
```

如果没有执行权限：

```bash
chmod +x scripts/*.sh
```

## 一键启动（保持你原习惯）

```bash
# 商品列表压测
./scripts/goods.sh

# 锁单压测（只锁单，不走 MQ）
./scripts/lock.sh

# 启动 mock 支付宝
./scripts/mock.sh

# 全链路压测
./scripts/full.sh
```

Locust Web UI: `http://localhost:7321`

## Make 快捷命令

- `make init`
- `make goods`
- `make lock`
- `make mock`
- `make full`

## 常见问题

- 报错 `.venv/bin/python` 不存在：先执行 `./scripts/init.sh`
- `7321` 端口占用：结束旧 Locust 进程后重试

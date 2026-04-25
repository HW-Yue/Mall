# 业务服务与 `labels.application` 对照

**用途**：写 SOP 占位符（如 `${application}`）、与 **Alertmanager `group_by`** 中的 `application` 对齐。

**来源**：`mall/docs/dev-ops/prometheus/alert_rules.yml` 各专项规则中的 `labels.application`。

| 规则意图 | `application` 标签值 | 备注 |
|----------|----------------------|------|
| order-service 专项 | `order-service` | 与 `app="order-service"` 同时使用 |
| seckill-service 专项 | `seckill-service` | |
| group-buy-service 专项 | `group-buy-service` | |
| pay / 支付宝创建订单 | `login-pay` | **`spring.application.name`** 为 login-pay（规则文件注释） |
| mall 商品列表 | `mall` | |
| 网关 | `gateway` | 指标里 `app="springcloud-gateway"`，标签 application 为 `gateway` |
| Exporter 全局 | `shared` | MySQL/Redis/RocketMQ 实例级 |

**注意**：Sentinel **全局**规则可能 **不**带 `application` 标签，仅有 `app` 与 `resource`；专项规则才显式加 `application`。

## 返回索引

[`README.md`](README.md) · [`sentinel.md`](sentinel.md)

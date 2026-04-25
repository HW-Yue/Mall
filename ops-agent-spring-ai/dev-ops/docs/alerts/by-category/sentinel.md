# category: `sentinel`

**规则组**：`sentinel-global`、`order-service-specific`、`seckill-service-specific`、`group-buy-service-specific`、`pay-service-specific`、`mall-service-specific`、`gateway-specific`  
**来源文件**：`mall/docs/dev-ops/prometheus/alert_rules.yml`

## 全局规则（所有业务服务通用）

| alertname | for | severity | expr（摘要） |
|-----------|-----|----------|--------------|
| SentinelBlockRateHigh | 1m | warning | `sentinel_block_qps > 50` |
| SentinelExceptionRateHigh | 2m | critical | `sentinel_exception_qps / clamp_min(sentinel_pass_qps, 1) > 0.05` |
| SentinelRtHigh | 2m | warning | `sentinel_rt > 1000` |
| SentinelPeakRtHigh | 2m | critical | `max_over_time(sentinel_rt[5m]) > 2000` |
| SentinelThreadCountHigh | 2m | warning | `sentinel_current_thread > 50` |

**标签**：`category: sentinel`；annotations 含 `{{ $labels.app }}`、`{{ $labels.resource }}`。

**说明（来自规则注释）**：`resource` 为 URI 字符串；`*_qps` 为 Gauge，**不宜**对 `rate()` 再聚合。

## order-service 专项

| alertname | for | severity | application 标签 | expr（摘要） |
|-----------|-----|----------|------------------|--------------|
| OrderServiceCreateOrderBlockHigh | 1m | critical | order-service | `sentinel_block_qps{app="order-service", resource="/api/v1/order/create_order"} > 20` |
| OrderServiceCreateOrderRtHigh | 2m | warning | order-service | `sentinel_rt{...create_order} > 500` |
| OrderServiceGetPayUrlRtHigh | 2m | warning | order-service | `sentinel_rt{...get_pay_url} > 300` |

## seckill-service 专项

| alertname | for | severity | application 标签 | expr（摘要） |
|-----------|-----|----------|------------------|--------------|
| SeckillServiceRtHigh | 1m | critical | seckill-service | `sentinel_rt{app="seckill-service"} > 200` |
| SeckillServiceBlockHigh | 1m | warning | seckill-service | `sentinel_block_qps{app="seckill-service"} > 100` |

## group-buy-service 专项

| alertname | for | severity | application 标签 | expr（摘要） |
|-----------|-----|----------|------------------|--------------|
| GroupBuyServiceCreatePayOrderBlockHigh | 1m | warning | group-buy-service | `sentinel_block_qps{app="group-buy-service", resource=~"/api/v1/group-buy/trade/.*"} > 30` |

## pay 服务专项（`app="login-pay"`）

| alertname | for | severity | application 标签 | expr（摘要） |
|-----------|-----|----------|------------------|--------------|
| PayServiceExceptionHigh | 1m | critical | login-pay | `sentinel_exception_qps{app="login-pay"}/clamp_min(sentinel_pass_qps{app="login-pay"},1) > 0.01` |
| PayServiceCreatePayOrderRtHigh | 2m | warning | login-pay | `sentinel_rt{app="login-pay", resource="/api/v1/alipay/create_pay_order"} > 500` |

## mall 专项

| alertname | for | severity | application 标签 | expr（摘要） |
|-----------|-----|----------|------------------|--------------|
| MallServiceQueryGoodsPageBlockHigh | 1m | warning | mall | `sentinel_block_qps{app="mall", resource="/api/v1/mall/index/query_goods_page"} > 50` |

## 网关专项

| alertname | for | severity | application 标签 | expr（摘要） |
|-----------|-----|----------|------------------|--------------|
| GatewayBlockRateHigh | 1m | critical | gateway | `sentinel_block_qps{app="springcloud-gateway"} > 200` |

## 返回索引

[`README.md`](README.md) · [`../sop-tool-mapping.md`](../sop-tool-mapping.md)

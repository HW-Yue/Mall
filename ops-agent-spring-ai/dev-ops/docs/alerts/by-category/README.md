# 按 category 分类的告警清单

**权威来源**：`dev-ops/prometheus/alert_rules.yml`  
下列 **`alert:` 名称** 与 **`labels.category`** 以该文件为准。

## 快速索引（alertname → 详解文件）

| category | 详解文件 | alertname 数量（约） |
|----------|----------|----------------------|
| `sentinel` | [`sentinel.md`](sentinel.md) | 全局 5 + 业务专项多条 |
| `dynamictp` | [`dynamictp.md`](dynamictp.md) | 4 |
| `http` | [`http.md`](http.md) | 1 |
| `hikari` | [`hikari.md`](hikari.md) | 3 |
| `system` | [`jvm-system.md`](jvm-system.md) | 3（含 `up`） |
| `mysql` | [`mysql.md`](mysql.md) | 4 |
| `redis` | [`redis.md`](redis.md) | 5 |
| `rocketmq` | [`rocketmq.md`](rocketmq.md) | 3 |

**业务应用标签**：部分规则额外设置 `labels.application`（如 `order-service`），详见 [`service-specific.md`](service-specific.md)。

## 全量 alertname（按 category）

- **sentinel**：GatewayBlockRateHigh, GroupBuyServiceCreatePayOrderBlockHigh, MallServiceQueryGoodsPageBlockHigh, OrderServiceCreateOrderBlockHigh, OrderServiceCreateOrderRtHigh, OrderServiceGetPayUrlRtHigh, PayServiceCreatePayOrderRtHigh, PayServiceExceptionHigh, SeckillServiceBlockHigh, SeckillServiceRtHigh, SentinelBlockRateHigh, SentinelExceptionRateHigh, SentinelPeakRtHigh, SentinelRtHigh, SentinelThreadCountHigh  
- **dynamictp**：OrderServiceThreadPoolBusy, ThreadPoolActiveRatioHigh, ThreadPoolAtMaxCapacity, ThreadPoolQueueUsageHigh, ThreadPoolRejectedTasks  
- **http**：Http5xxErrorRateHigh  
- **hikari**：HikariConnectionAcquireSlow, HikariConnectionsPending, HikariConnectionsSaturated  
- **system**：JvmGcPauseHigh, JvmHeapUsageHigh, ServiceDown  
- **mysql**：MySqlDown, MySqlInnodbRowLockWaitHigh, MySqlSlowQueriesHigh, MySqlTooManyConnections  
- **redis**：RedisBlockedClients, RedisConnectedClientsHigh, RedisDown, RedisKeyspaceHitRateLow, RedisMemoryHigh  
- **rocketmq**：RocketMqBrokerDown, RocketMqConsumerLagHigh, RocketMqDlqMessageAppeared  

## 相关

- SOP ↔ 工具：[`../sop-tool-mapping.md`](../sop-tool-mapping.md)
- Webhook：[`../formats/alertmanager-webhook-payload.md`](../formats/alertmanager-webhook-payload.md)

# category: `rocketmq`（rocketmq_exporter）

**规则组**：`rocketmq-exporter-global`  
**来源文件**：`mall/docs/dev-ops/prometheus/alert_rules.yml`  
**抓取**：`prometheus.yml` job `rocketmq-exporter` → `rocketmq-exporter:5557`，静态 labels **`application: shared`**。

| alertname | for | severity | application | expr（摘要） |
|-----------|-----|----------|-------------|--------------|
| RocketMqBrokerDown | 5m | critical | shared | `rocketmq_broker_tps == 0 and on(cluster,brokerName) (rocketmq_broker_tps offset 5m) > 0` |
| RocketMqConsumerLagHigh | 5m | warning | shared | `rocketmq_consumer_message_accumulation > 5000` |
| RocketMqDlqMessageAppeared | 5m | critical | shared | `increase(rocketmq_producer_offset{topic=~"%DLQ%.*"}[5m]) > 0` |

**标签**：`category: rocketmq`，`application: shared`；描述中含 **group/topic** 等 exporter 标签。

## 返回索引

[`README.md`](README.md)

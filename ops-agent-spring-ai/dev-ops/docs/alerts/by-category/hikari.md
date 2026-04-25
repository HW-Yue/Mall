# category: `hikari`

**规则组**：`hikari-global`  
**来源文件**：`mall/docs/dev-ops/prometheus/alert_rules.yml`

**说明**：`ops-agent-spring-ai` 在 hikari 类告警的 SOP 流程中按 `labels.pool` 反查 `application` 后查 Nacos dataId（见规则文件注释）。

| alertname | for | severity | expr（摘要） |
|-----------|-----|----------|--------------|
| HikariConnectionsSaturated | 2m | warning | `hikaricp_connections_active / clamp_min(hikaricp_connections_max, 1) > 0.85` |
| HikariConnectionsPending | 1m | warning | `hikaricp_connections_pending > 0` |
| HikariConnectionAcquireSlow | 2m | warning | `rate(hikaricp_connections_acquire_seconds_sum[1m]) / clamp_min(rate(hikaricp_connections_acquire_seconds_count[1m]), 1) > 0.1` |

**标签**：`category: hikari`；描述中含 **`application`**、**`pool`**。

## 返回索引

[`README.md`](README.md)

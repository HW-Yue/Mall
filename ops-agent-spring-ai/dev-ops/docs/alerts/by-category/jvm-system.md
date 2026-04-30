# category: `system`（JVM / 抓取可用性）

**规则组**：`jvm-system-global`  
**来源文件**：`dev-ops/prometheus/alert_rules.yml`

| alertname | for | severity | expr（摘要） |
|-----------|-----|----------|--------------|
| JvmHeapUsageHigh | 2m | warning | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85` |
| JvmGcPauseHigh | 2m | warning | `rate(jvm_gc_pause_seconds_sum[5m]) / clamp_min(rate(jvm_gc_pause_seconds_count[5m]), 1) > 0.5` |
| ServiceDown | 1m | critical | `up == 0` |

**标签**：`category: system`。

### ServiceDown 说明

- 表示 **Prometheus 无法成功 scrape 该 target**（进程 down、网络、错误注册地址等）。
- 与 **Exporter** 的 `mysql_up`/`redis_up` 等 **不同**；后者见 [`mysql.md`](mysql.md)、[`redis.md`](redis.md)。

## 返回索引

[`README.md`](README.md)

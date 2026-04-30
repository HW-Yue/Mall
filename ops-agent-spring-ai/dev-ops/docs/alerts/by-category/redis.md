# category: `redis`（redis_exporter）

**规则组**：`redis-exporter-global`  
**来源文件**：`dev-ops/prometheus/alert_rules.yml`  
**抓取**：`prometheus.yml` job `redis-exporter` → `redis-exporter:9121`，静态 labels **`application: shared`**。

| alertname | for | severity | application | expr（摘要） |
|-----------|-----|----------|-------------|--------------|
| RedisDown | 1m | critical | shared | `redis_up == 0` |
| RedisMemoryHigh | 5m | warning | shared | `redis_memory_used_bytes / clamp_min(redis_memory_max_bytes, 1) > 0.8` |
| RedisConnectedClientsHigh | 5m | warning | shared | `redis_connected_clients > 500` |
| RedisKeyspaceHitRateLow | 10m | warning | shared | `rate(redis_keyspace_hits_total[5m]) / clamp_min(rate(redis_keyspace_hits_total[5m]) + rate(redis_keyspace_misses_total[5m]), 0.001) < 0.8` |
| RedisBlockedClients | 2m | warning | shared | `redis_blocked_clients > 0` |

**标签**：`category: redis`，`application: shared`。

## 返回索引

[`README.md`](README.md)

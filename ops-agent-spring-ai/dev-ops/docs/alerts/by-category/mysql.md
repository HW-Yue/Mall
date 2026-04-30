# category: `mysql`（mysqld_exporter）

**规则组**：`mysql-exporter-global`  
**来源文件**：`dev-ops/prometheus/alert_rules.yml`  
**抓取**：`prometheus.yml` job `mysqld-exporter` → `mysqld-exporter:9104`，静态 labels **`application: shared`**。

| alertname | for | severity | application | expr（摘要） |
|-----------|-----|----------|-------------|--------------|
| MySqlDown | 1m | critical | shared | `mysql_up == 0` |
| MySqlTooManyConnections | 2m | warning | shared | `mysql_global_status_threads_connected / clamp_min(mysql_global_variables_max_connections, 1) > 0.8` |
| MySqlSlowQueriesHigh | 5m | warning | shared | `rate(mysql_global_status_slow_queries[5m]) / clamp_min(rate(mysql_global_status_questions[5m]), 0.001) > 0.01` |
| MySqlInnodbRowLockWaitHigh | 5m | warning | shared | `increase(mysql_global_status_innodb_row_lock_waits[5m]) > 10` |

**标签**：`category: mysql`，`application: shared`。

## 返回索引

[`README.md`](README.md)

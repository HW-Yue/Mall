# category: `http`

**规则组**：`http-layer-global`  
**来源文件**：`mall/docs/dev-ops/prometheus/alert_rules.yml`

| alertname | for | severity | expr（摘要） |
|-----------|-----|----------|--------------|
| Http5xxErrorRateHigh | 2m | critical | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m])) by (application) / clamp_min(sum(rate(http_server_requests_seconds_count[1m])) by (application), 0.001) > 0.05` |

**标签**：`category: http`；按 **`application`** 分组。

## 返回索引

[`README.md`](README.md)

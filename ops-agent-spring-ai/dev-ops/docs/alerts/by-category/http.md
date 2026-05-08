# category: `http`

**规则组**：`http-layer-global`  
**来源文件**：`dev-ops/prometheus/alert_rules.yml`

| alertname | for | severity | expr（摘要） |
|-----------|-----|----------|--------------|
| Http5xxErrorRateHigh | 2m | critical | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m])) by (application) / clamp_min(sum(rate(http_server_requests_seconds_count[1m])) by (application), 0.001) > 0.05` |

**标签**：`category: http`；按 **`application`** 分组。

**样例口径**：

- `application` 使用服务注册名，例如 `mall-service`、`order-service`
- HTTP 5xx 告警通常按应用聚合，不强依赖 `labels.resource`
- 如果前端或文档中要补充业务接口示例，使用服务内 URI，如 `/api/v1/mall/trade/create_normal_order`；不要写网关前缀 `/gw`

## 返回索引

[`README.md`](README.md)

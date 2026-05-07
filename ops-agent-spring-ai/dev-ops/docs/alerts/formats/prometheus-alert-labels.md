# Prometheus 告警：规则侧 labels 与 annotations

**权威来源**：`dev-ops/prometheus/alert_rules.yml` 每条 `alert` 下的 `labels` / `annotations`。

## 1. 通用标签（多数规则）

规则文件内显式设置的 `labels` 常见键：

| 键 | 含义 | 示例值 |
|----|------|--------|
| `severity` | 严重度 | `warning` / `critical` |
| `category` | 运维域/策略域 | `sentinel` / `dynamictp` / `dubbo` / `http` / `hikari` / `system` / `mysql` / `redis` / `rocketmq` |
| `application` | 业务应用名或 `shared` | `order-service` / `mall-service` / `shared` / `pay-service` 等 |

> Prometheus 还会自动附加 **`alertname`**（来自规则中的 `alert:` 字段）以及抓取目标上的标签（如 `instance`、`job` 等），具体以实际 firing 为准。

## 2. annotations（展示模板）

常见键：

| 键 | 用途 |
|----|------|
| `summary` | 短描述 |
| `description` | 长描述，常含 `{{ $labels.xxx }}`、`{{ $value }}` 等模板变量 |

模板变量来自 **Prometheus 告警评估上下文**（labels、value、查询结果等）。

## 3. Sentinel 专项说明（来自规则文件注释）

- Sentinel 资源名 **`resource`** 为 **URI 字符串**（如 `/api/v1/order/create_order`），**不含** HTTP Method 前缀。
- 部分规则使用 **`{app="xxx"}`** 选择服务（与 Micrometer 中 Sentinel binder 打的 `app` 标签一致）。

## 4. Dubbo 专项说明

- Dubbo 原始指标名中的点会转换为 Prometheus 下划线形式，例如 `dubbo.provider.requests.total` → `dubbo_provider_requests_total`。
- Dubbo 自带 `application.name` 标签会转换为 **`application_name`**；规则侧将 `labels.application` 设置为该值，便于 Alertmanager 分组。
- 方法级 RPC 指标需要先发生 Dubbo 调用，冷启动或无流量时可能没有对应 `interface` / `method` 时序。

## 5. 样例：评估后经 Alertmanager 进入 Webhook 的 `labels`（概念）

以下键值仅为说明结构；**真实 `alertname` 以 `alert_rules.yml` 为准**。

```json
{
  "alertname": "JVMMemoryHigh",
  "severity": "warning",
  "category": "system",
  "application": "order-service",
  "instance": "192.168.1.10:18081",
  "job": "jvm"
}
```

同一告警在 **HTTP POST 正文** 里会出现在 `alerts[].labels` 中，完整 JSON 见 [`alertmanager-webhook-payload.md`](alertmanager-webhook-payload.md) 第 5 节。

## 6. 相关文档

- 按告警名逐条列表：[`../by-category/README.md`](../by-category/README.md)
- Webhook 中如何出现这些字段：[`alertmanager-webhook-payload.md`](alertmanager-webhook-payload.md)

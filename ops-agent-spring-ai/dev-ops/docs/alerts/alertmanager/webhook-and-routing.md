# Alertmanager 路由与 Webhook

**权威配置**：`dev-ops/prometheus/alertmanager.yml`

## 1. 全局

- `resolve_timeout: 5m`

## 2. 路由 `route`

| 字段 | 值 | 说明 |
|------|-----|------|
| `group_by` | `['alertname', 'category', 'application']` | 分组键；Webhook 中多条 alert 可能合并为一组 |
| `group_wait` | `10s` | 组内首次等待 |
| `group_interval` | `30s` | 同组重复发送间隔 |
| `repeat_interval` | `12h` | 未恢复时重复通知间隔 |
| `receiver` | `ops-agent-spring-ai` | 默认接收器名称 |

## 3. 接收器 `receivers`

- **名称**：`ops-agent-spring-ai`
- **类型**：`webhook_configs`
- **URL**：`http://host.docker.internal:8096/api/v1/alert/receive`
- **send_resolved**：`true`（解决事件也会推送）
- **说明**：`8096` 对应仓库内 **`ops-agent-spring-ai`** 模块（见 `ops-agent-spring-ai/src/main/resources/application.yml` 的 `server.port`）

## 4. 抑制规则 `inhibit_rules`

- **规则**：`severity=critical` 作为 source 时，抑制同 `alertname` 且同 `application` 的 `severity=warning`。
- **目的**：减少 ops-agent-spring-ai 对同一问题的重复处理。

## 5. 相关文档

- Webhook JSON 结构：[`../formats/alertmanager-webhook-payload.md`](../formats/alertmanager-webhook-payload.md)

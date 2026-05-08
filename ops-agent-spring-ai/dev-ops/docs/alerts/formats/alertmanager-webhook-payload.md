# Alertmanager Webhook 负载格式（对接 Agent）

Alertmanager 向 `webhook_configs.url` 发送 **HTTP POST**，`Content-Type` 一般为 `application/json`。  
以下结构遵循 **Alertmanager v2 Webhook** 常见形态；**以线上实际抓包为准**。

> **和「规则里长什么样」的区别**：`labels` / `annotations` 在 **Prometheus 规则里怎么写** 见同目录 [`prometheus-alert-labels.md`](prometheus-alert-labels.md)（权威：`mall/.../alert_rules.yml`）。本文是 **AM 已组装好、POST 到 Agent 的 JSON 长什么样**。

## 1. 顶层字段（典型）

| 字段 | 类型 | 说明 |
|------|------|------|
| `receiver` | string | 如 `ops-agent-spring-ai` |
| `status` | string | `firing` / `resolved`（或兼容旧版仅顶层 status） |
| `alerts` | array | 告警列表（见下） |
| `groupLabels` | object | 分组标签子集 |
| `commonLabels` | object | 本组告警公共标签 |
| `commonAnnotations` | object | 本组公共 annotations |
| `externalURL` | string | Alertmanager 外部 URL（可选） |

> 不同 Alertmanager 版本字段可能略有差异；Agent 端应 **以 `alerts[]` 为主** 解析。

## 2. `alerts[]` 单条元素（典型）

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | string | `firing` / `resolved` |
| `labels` | object | 含 **`alertname`**、`severity`、`category`、`application` 及 `instance`、`job` 等 |
| `annotations` | object | `summary`、`description` |
| `startsAt` | string | RFC3339 时间 |
| `endsAt` | string | resolved 时有效 |
| `generatorURL` | string | Prometheus 告警链接（可选） |

## 3. 本项目 Agent 入参映射（参考实现）

**ops-agent-spring-ai** 的 `AlertReceiveController` 使用自定义 DTO 接收 body（见 `ops-agent-spring-ai` 内 `AlertmanagerPayload`）。  
对接时需保证：**至少能把 `alerts[].labels.alertname`、severity、application、category 及 annotations 传到后端**。

建议在 SOP 匹配中使用：

- **`labels.alertname`**：主键匹配 SOP 规则里的 `match-alertname`（默认每条规则一个文件：`src/main/resources/sop/rules/*.yml`，由 `ops-ai.sop.rules-dir` 扫描；仍可在 `application.yml` 的 `ops-ai.sop.rules` 写内联规则，且排在文件规则之前）
- **`labels.category`**：辅助过滤或文档化
- **`labels.application`**：占位符 `${application}` 等（见 `AlertPlaceholderResolver`）

## 4. `send_resolved: true` 的影响

解决事件会再次 POST；Agent 应区分 **`status=resolved`**，避免对已恢复告警重复执行自动变更类动作。

## 5. 完整样例（与本项目 DTO 对齐）

Java 端 `AlertmanagerPayload` 只强依赖两个字段：**顶层 `alerts` 数组**、以及**每条**里的 `labels`（`annotations` 等可选但建议保留）。未列出的键会被忽略（`@JsonIgnoreProperties(ignoreUnknown = true)`）。

### 5.1 仅含本项目解析字段的「最小可运行」体

POST `Content-Type: application/json` → `http://<host>:8096/api/v1/alert/receive`

```json
{
  "status": "firing",
  "alerts": [
    {
      "status": "firing",
      "labels": {
        "alertname": "JVMMemoryHigh",
        "severity": "warning",
        "category": "system",
        "application": "order-service",
        "instance": "192.168.1.10:18081",
        "job": "jvm"
      },
      "annotations": {
        "summary": "order-service JVM 堆占用偏高",
        "description": "heap 使用率超过 85% 持续 5m，instance=192.168.1.10:18081"
      },
      "startsAt": "2026-04-25T10:00:00.000Z",
      "endsAt": "0001-01-01T00:00:00.000Z"
    }
  ]
}
```

说明：

- 顶层 **`status`**：部分 AM 版本会带；`AlertEvent` 会优先用**每条** `alerts[].status`，否则回退到顶层 `status`。
- **`endsAt` 的零时间**：`firing` 时常见此占位，表示尚未结束（以线上 Alertmanager 实际输出为准）。

### 5.2 更接近现网：带 `groupLabels` / `commonLabels` 的整包

真实 Alertmanager 还会附带 `receiver`、`groupLabels`、`commonLabels`、`commonAnnotations`、`externalURL` 等；**本服务当前实现不读这些字段**，但转发链路上普遍存在，可用来对照抓包或 Mock。

```json
{
  "receiver": "ops-agent-spring-ai",
  "status": "firing",
  "alerts": [
    {
      "status": "firing",
      "labels": {
        "alertname": "Http5xxRateHigh",
        "severity": "critical",
        "category": "http",
        "application": "mall",
        "instance": "192.168.1.20:8095",
        "job": "spring-boot"
      },
      "annotations": {
        "summary": "mall 服务 5xx 比例过高",
        "description": "5xx/总请求 > 0.5% 持续 5m"
      },
      "startsAt": "2026-04-25T10:05:00.000Z",
      "endsAt": "0001-01-01T00:00:00.000Z",
      "generatorURL": "http://prometheus:9090/graph?g0.expr=..."
    }
  ],
  "groupLabels": {
    "alertname": "Http5xxRateHigh",
    "category": "http",
    "application": "mall"
  },
  "commonLabels": {
    "alertname": "Http5xxRateHigh",
    "severity": "critical",
    "category": "http",
    "application": "mall",
    "instance": "192.168.1.20:8095",
    "job": "spring-boot"
  },
  "commonAnnotations": {
    "summary": "mall 服务 5xx 比例过高"
  },
  "externalURL": "http://alertmanager:9093"
}
```

（若 `alertname` 为演示用，请以 `dev-ops/prometheus/alert_rules.yml` 中真实名称为准。）

### 5.3 恢复事件（`send_resolved: true`）

解决后 Alertmanager 会再 POST 一次，单条 `status` 为 `resolved`，`endsAt` 为有效时间：

```json
{
  "status": "resolved",
  "alerts": [
    {
      "status": "resolved",
      "labels": {
        "alertname": "JVMMemoryHigh",
        "severity": "warning",
        "category": "system",
        "application": "order-service",
        "instance": "192.168.1.10:18081"
      },
      "annotations": {
        "summary": "order-service JVM 堆占用偏高"
      },
      "startsAt": "2026-04-25T10:00:00.000Z",
      "endsAt": "2026-04-25T10:12:00.000Z"
    }
  ]
}
```

编写 SOP 或自动化时，**应对 `resolved` 分支**做幂等/跳过，避免对已恢复问题重复写配置。

## 6. 相关文档

- 规则里有哪些 `alertname`：[`../by-category/README.md`](../by-category/README.md)
- Alertmanager 配置：[`../alertmanager/webhook-and-routing.md`](../alertmanager/webhook-and-routing.md)
- 规则侧 `labels` / `annotations` 键说明：[`prometheus-alert-labels.md`](prometheus-alert-labels.md)

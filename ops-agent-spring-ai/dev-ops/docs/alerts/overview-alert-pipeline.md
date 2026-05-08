# 告警流水线总览

## 1. 组件与职责

| 组件 | 配置位置 | 职责 |
|------|----------|------|
| **业务服务**（mall 各微服务） | 各服务 Spring Boot | 暴露 `/actuator/prometheus`；Micrometer 指标（HTTP、JVM、Hikari、DTP、Sentinel 等） |
| **Nacos** | `dev-ops/...` | 提供 **Prometheus HTTP SD** 端点，返回需抓取的目标列表 |
| **Prometheus** | `dev-ops/prometheus/prometheus.yml` | 按 `scrape_configs` 拉取指标；加载 `alert_rules.yml` **计算告警**；将告警送 Alertmanager |
| **Exporters** | 同目录 compose（如 `docker-compose-exporters.yml`） | `mysqld-exporter`、`redis-exporter`、`rocketmq-exporter` 提供中间件指标 |
| **Alertmanager** | `dev-ops/prometheus/alertmanager.yml` | 分组、抑制、去重；**Webhook** 转发到消费方 |
| **Agent（消费方）** | `ops-agent-spring-ai` | HTTP 接收 webhook，匹配 SOP，执行工具或子 Agent |

## 2. 数据流（简图）

```
微服务 actuator ──┐
Nacos SD 列表 ────┼──► Prometheus scrape ──► TSDB
Exporters ────────┘              │
                                 ▼
                        rule_files: alert_rules.yml
                        (for: 持续时间, expr: PromQL)
                                 │
                                 ▼
                        firing alerts ──► Alertmanager
                                 │
                                 ▼
                        webhook POST ──► Agent（ops-agent-spring-ai 默认 :8096，见 `ops-agent-spring-ai/src/main/resources/application.yml`）
```

> **注意**：Alertmanager webhook 默认指向 **`http://host.docker.internal:8096/api/v1/alert/receive`**（与 `dev-ops/prometheus/alertmanager.yml` 一致）。

## 3. 与「预警」相关的代码路径（商城）

- **规则定义**：仅 YAML，不在 Java 内硬编码 alertname。
- **指标绑定**：Sentinel / DTP 等由 common 组件注册到 Micrometer（详见 `alert_rules.yml` 顶部注释）。

## 4. 延伸阅读

- 抓取与标签：[`prometheus/scrape-and-labels.md`](prometheus/scrape-and-labels.md)
- Webhook 字段：[`formats/alertmanager-webhook-payload.md`](formats/alertmanager-webhook-payload.md)
- 逐条告警：[`by-category/README.md`](by-category/README.md)

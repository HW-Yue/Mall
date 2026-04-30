# 开发文档（DevOps / 告警 / Agent）

本文档树为 **ops-agent-spring-ai** 与 **商城 Prometheus 告警** 对齐的「单一事实入口」。细则按主题拆文件，便于检索与 AI 引用。

## 必读索引

| 文档 | 内容 |
|------|------|
| [`alerts/README.md`](alerts/README.md) | 告警体系总览：谁评估、谁发送、标签约定 |
| [`alerts/overview-alert-pipeline.md`](alerts/overview-alert-pipeline.md) | 从指标 → 规则 → Alertmanager → Webhook 的完整链路 |
| [`alerts/prometheus/scrape-and-labels.md`](alerts/prometheus/scrape-and-labels.md) | 抓取任务、Nacos SD、`application`/`category` 标签来源 |
| [`alerts/alertmanager/webhook-and-routing.md`](alerts/alertmanager/webhook-and-routing.md) | Alertmanager 分组、接收器、当前 webhook URL |
| [`alerts/by-category/README.md`](alerts/by-category/README.md) | **按 category 分目录**的每条告警：表达式、标签、说明 |
| [`alerts/formats/alertmanager-webhook-payload.md`](alerts/formats/alertmanager-webhook-payload.md) | 常见 Webhook JSON 结构（字段级） |
| [`alerts/formats/prometheus-alert-labels.md`](alerts/formats/prometheus-alert-labels.md) | Prometheus 告警标签与模板变量 |
| [`alerts/sop-tool-mapping.md`](alerts/sop-tool-mapping.md) | 告警类别 → `ops-agent-spring-ai` skill 工具建议路径 |

## 仓库内「权威配置」路径（请勿与本文档脱节）

| 资源 | 路径 |
|------|------|
| 告警规则 | `dev-ops/prometheus/alert_rules.yml` |
| Prometheus | `dev-ops/prometheus/prometheus.yml` |
| Alertmanager | `dev-ops/prometheus/alertmanager.yml` |
| Spring AI Agent 示例配置 | `ops-agent-spring-ai/src/main/resources/application.yml` |

## 前端资源

见 [`../frontend/README.md`](../frontend/README.md)。

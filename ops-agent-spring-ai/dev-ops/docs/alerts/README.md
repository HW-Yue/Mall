# 告警文档索引

## 本目录做什么

说明 **商城 DevOps 栈**里告警的 **来源**、**格式**、**标签字段**，以及与 **Agent / SOP** 的对应方式。细节拆到子文件，主索引见 [`../README.md`](../README.md)。

## 文件导航

| 文件 | 说明 |
|------|------|
| [`overview-alert-pipeline.md`](overview-alert-pipeline.md) | 指标 → 规则评估 → Alertmanager → Webhook |
| [`prometheus/scrape-and-labels.md`](prometheus/scrape-and-labels.md) | Job、Nacos SD、Exporter、标签 |
| [`alertmanager/webhook-and-routing.md`](alertmanager/webhook-and-routing.md) | `route`、`receiver`、当前 webhook 目标 |
| [`formats/prometheus-alert-labels.md`](formats/prometheus-alert-labels.md) | 规则里 `labels`/`annotations` 字段 |
| [`formats/alertmanager-webhook-payload.md`](formats/alertmanager-webhook-payload.md) | POST body 结构（对接 Agent 入参） |
| [`by-category/README.md`](by-category/README.md) | 按 `category` 分组的告警清单（逐条） |
| [`sop-tool-mapping.md`](sop-tool-mapping.md) | 排查时推荐的 skill / 工具顺序 |

## 权威规则文件

所有 `alertname` 以 **`mall/docs/dev-ops/prometheus/alert_rules.yml`** 为准；本文档中的表格从该文件摘录，变更时请同步更新。

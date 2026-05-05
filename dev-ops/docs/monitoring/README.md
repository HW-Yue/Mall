# 监控文档

`dev-ops/docs/monitoring/` 维护商城 DevOps 监控栈的正式说明。这里聚合 Prometheus、Alertmanager、Sentinel、DynamicTP、ELK、SkyWalking 和启动顺序；更细的告警语义仍以 `ops-agent-spring-ai/dev-ops/docs/alerts/` 为准。

## 目录

| 主题 | 内容 | 详情 |
|---|---|---|
| 指标暴露 | 业务服务如何暴露 Micrometer、Sentinel、DTP、JVM/HTTP 指标 | [metric-exposure](./metric-exposure.md) |
| Prometheus / Alertmanager | 抓取、规则评估、告警分组、Webhook 转发 | [prometheus-alertmanager](./prometheus-alertmanager.md) |
| Exporters | MySQL / Redis / RocketMQ exporter 的职责、标签和端口 | [exporters](./exporters.md) |
| Sentinel | Nacos 规则组织、Dashboard、资源命名和标签约定 | [sentinel](./sentinel.md) |
| DynamicTP | 线程池运行时配置与指标采集约定 | [dynamic-tp](./dynamic-tp.md) |
| ELK | Elasticsearch、Logstash、Kibana 的开发环境接入 | [elk](./elk.md) |
| SkyWalking | OAP / UI / Java Agent 联调与和 ELK 的关系 | [skywalking](./skywalking.md) |
| 启动顺序 | 本仓库监控栈推荐启动顺序 | [startup-order](./startup-order.md) |

## 关键文件

| 用途 | 文件 |
|---|---|
| Prometheus 抓取配置 | `dev-ops/prometheus/prometheus.yml` |
| Prometheus 告警规则 | `dev-ops/prometheus/alert_rules.yml` |
| Alertmanager 路由 | `dev-ops/prometheus/alertmanager.yml` |
| Grafana / Prometheus / Alertmanager compose | `dev-ops/docker-compose-grafana.yml` |
| Exporters compose | `dev-ops/docker-compose-exporters.yml` |
| ELK compose | `dev-ops/docker-compose-elk.yml` |
| SkyWalking compose | `dev-ops/docker-compose-skywalking.yml` |
| Nacos 运行时配置入口 | `dev-ops/nacos/README.md` |
| Sentinel 规则入口 | `dev-ops/nacos/sentinel-rules/README.md` |
| SkyWalking 开发接入 | `dev-ops/SKYWALKING.md` |

## 维护规则

- 监控正式说明统一维护在 `dev-ops/docs/monitoring/`。
- 配置事实优先以 `dev-ops/` 下 compose、Prometheus、Nacos 文档为准。
- 告警分类、标签字段、Alertmanager 到 `ops-agent-spring-ai` 的 SOP 对接，优先引用 `ops-agent-spring-ai/dev-ops/docs/alerts/`，不要在本目录重复抄整套告警清单。

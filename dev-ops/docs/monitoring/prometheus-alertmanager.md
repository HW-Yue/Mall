# Prometheus 与 Alertmanager

- Prometheus 负责抓取业务服务与 exporter 指标，并评估 `alert_rules.yml`。
- Alertmanager 负责按 `alertname`、`category`、`application` 分组告警，再转发到 `ops-agent-spring-ai` webhook。

## Prometheus

- 主配置：`dev-ops/prometheus/prometheus.yml`
- 规则文件：`dev-ops/prometheus/alert_rules.yml`
- Compose：`dev-ops/docker-compose-grafana.yml`
- 业务抓取方式：通过 Nacos HTTP SD 拉取服务实例，默认 `metrics_path=/actuator/prometheus`
- Dubbo RPC 告警：复用业务服务 `/actuator/prometheus` 中的 Dubbo Micrometer 指标，规则分类为 `category=dubbo`
- 特殊处理：若目标地址注册成 `127.0.0.1:port`，通过 relabel 替换为 `host.docker.internal:${port}`，避免容器内 Prometheus 抓不到宿主机服务

## Alertmanager

- 配置：`dev-ops/prometheus/alertmanager.yml`
- 默认接收器：`ops-agent-spring-ai`
- Webhook URL：`http://host.docker.internal:8096/api/v1/alert/receive`
- 分组键：`alertname`、`category`、`application`
- 抑制规则：同服务下 `critical` 抑制同名 `warning`

## 相关入口

- 告警规则明细与标签语义：`ops-agent-spring-ai/dev-ops/docs/alerts/`
- 告警总链路：`ops-agent-spring-ai/README.md`

## 事实来源

- `dev-ops/prometheus/prometheus.yml`
- `dev-ops/prometheus/alertmanager.yml`
- `dev-ops/docker-compose-grafana.yml`
- `ops-agent-spring-ai/dev-ops/docs/alerts/README.md`

# 指标暴露

- 目标：统一说明业务服务如何暴露 Prometheus 可抓取指标。
- 入口：`management.endpoints.web.exposure.include` 需要包含 `health,prometheus`。

## 指标来源

- JVM / HTTP：Spring Boot Actuator 自动暴露。
- Sentinel：`Dependencies/common-log-starter/.../sentinel/SentinelMetricsBinder.java` 注册 Gauge。
- DynamicTP：各服务 `dtp-dev.yml` 里开启 `collector-types: [micrometer, internal_logging]`。

## 关键约束

- Sentinel 指标需要服务先有真实请求流量，首次请求前 Dashboard 和 Prometheus 中可能还没有相关时序。
- Prometheus 默认通过 `/actuator/prometheus` 抓取 Spring Boot 服务。
- Exporter 指标与业务服务指标分开采集，Exporter 统一打 `application=shared`。

## 事实来源

- `AGENTS.md` / `CLAUDE.md` 现有监控段落
- `dev-ops/prometheus/prometheus.yml`
- `Dependencies/common-log-starter/.../sentinel/SentinelMetricsBinder.java`
- `dev-ops/nacos/dtp-config/`

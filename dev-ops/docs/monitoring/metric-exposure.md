# 指标暴露

- 目标：统一说明业务服务如何暴露 Prometheus 可抓取指标。
- 入口：`management.endpoints.web.exposure.include` 需要包含 `health,prometheus`。

## 指标来源

- JVM / HTTP：Spring Boot Actuator 自动暴露。
- Sentinel：`Dependencies/common-log-starter/.../sentinel/SentinelMetricsBinder.java` 注册 Gauge。
- DynamicTP：各服务 `dtp-dev.yml` 里开启 `collector-types: [micrometer, internal_logging]`。
- Dubbo：各 Dubbo 应用在 `*-app/src/main/resources/application.yml` 开启 `dubbo.metrics.protocol=prometheus`、`dubbo.metrics.enable-rpc=true`、`dubbo.metrics.use-global-registry=true`，RPC / 线程池 / 注册中心 / 元数据指标会进入 Micrometer 全局注册表，并随 `/actuator/prometheus` 输出。

## 关键约束

- Sentinel 指标需要服务先有真实请求流量，首次请求前 Dashboard 和 Prometheus 中可能还没有相关时序。
- Dubbo RPC 指标同样需要先发生 Dubbo 调用，首次调用前只会看到应用、线程池、注册中心等基础指标。
- Prometheus 默认通过 `/actuator/prometheus` 抓取 Spring Boot 服务。
- Dubbo 预警规则统一归类为 `category=dubbo`，由 `ops-agent-spring-ai` 通过 `metrics_ops` + 日志 / Nacos / 下游依赖工具排查。
- Exporter 指标与业务服务指标分开采集，Exporter 统一打 `application=shared`。

## 事实来源

- `AGENTS.md` / `CLAUDE.md` 现有监控段落
- `dev-ops/prometheus/prometheus.yml`
- `Dependencies/common-log-starter/.../sentinel/SentinelMetricsBinder.java`
- `mall|order-service|group-buy-service|seckill-service|pay/*-app/src/main/resources/application.yml`
- `dev-ops/nacos/dtp-config/`

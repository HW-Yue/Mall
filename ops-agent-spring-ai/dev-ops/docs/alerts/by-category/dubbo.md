# category: `dubbo`

**规则组**：`dubbo-global`  
**来源文件**：`dev-ops/prometheus/alert_rules.yml`

**抓取**：业务服务通过 Nacos HTTP SD 暴露 `/actuator/prometheus`；Dubbo 指标来自 Dubbo 3.3.2 metrics，并随 Micrometer 一起输出。

## 告警清单

| alertname | for | severity | 表达式摘要 |
|---|---:|---|---|
| DubboProviderErrorRateHigh | 2m | critical | provider 失败率 > 5% |
| DubboConsumerErrorRateHigh | 2m | critical | consumer 失败率 > 5% |
| DubboProviderRtP99High | 2m | warning | provider P99 RT > 1000ms |
| DubboConsumerRtP99High | 2m | warning | consumer P99 RT > 1000ms |
| DubboProviderNoTraffic | 5m | warning | 当前 5m 无 provider 请求，但 10m 前仍有流量 |
| DubboThreadPoolActiveHigh | 2m | warning | Dubbo 线程池 active/max > 80% |
| DubboThreadPoolRejected | 1m | critical | Dubbo 线程池拒绝数 1m 内增加 |
| DubboRegistryFailure | 2m | warning | 注册/订阅失败数 5m 内增加 |
| DubboMetadataFailure | 2m | warning | 元数据 push/subscribe/store 失败数 5m 内增加 |

## 标签

**规则标签**：`category: dubbo`，`application: {{ $labels.application_name }}`。  
**指标标签**：常见有 `application_name`、`interface`、`method`、`group`、`version`、`thread_pool_name`。

## 排查入口

优先使用 `metrics_ops.business_metrics` 查询 Dubbo PromQL，再按证据进入日志、Nacos、Docker 或下游依赖排查。

[`README.md`](README.md) · [`../sop-tool-mapping.md`](../sop-tool-mapping.md)

# 告警域 → ops-agent-spring-ai 工具映射（SOP 编写参考）

**目标**：写 `sop-markdown`（正文用**纯文本**即可，配置键名历史原因未改）或 deterministic `steps` 时，快速选对 **skill**（域）与典型 **tool**。

**现成 playbook**：`ops-agent-spring-ai/src/main/resources/sop/rules/*.yml` 已按 `by-category/README.md` 中 **`alertname` + `category`** 拆文件，可与上表对照增删。

**七域 skill id**：`docker_ops`、`mysql_inspect`、`rocketmq_inspect`、`metrics_ops`、`elasticsearch_ops`、`redis_inspect`、`nacos_config`（与 `SubAgent.domainId()` 一致）。

## 1. 按 `category`（Prometheus `labels.category`）

| category | 典型现象 | 建议排查顺序（工具思路） |
|----------|----------|---------------------------|
| `sentinel` | 限流、RT 高、异常率高 | **metrics_ops**（`sentinel_metrics` 等 + 自定义 `promql`）→ **elasticsearch_ops**（日志/Trace 索引）→ **nacos_config**（Sentinel 规则 dataId，**写需审批**） |
| `dynamictp` | 线程池忙、队列满、拒绝任务 | **metrics_ops**（`dynamictp_metrics` / `jvm_metrics`）→ **nacos_config**（DTP yml）→ 必要时 **mysql_inspect** / **redis_inspect**（排除下游慢） |
| `http` | 5xx 比例高 | **metrics_ops** → **elasticsearch_ops**（`es_search`/`es_aggregation`）→ **docker_ops**（若仅个别实例） |
| `hikari` | 连接池满、pending、获取慢 | **metrics_ops**（`jvm_metrics`/`business_metrics` 辅助看资源）+ **hikaricp** 指标用 PromQL → **mysql_inspect** → **nacos_config**（数据源 yml） |
| `system` | JVM 堆、GC、`up==0` | **metrics_ops** → **docker_ops**（容器/资源）→ **elasticsearch_ops**（日志）；**ServiceDown** 时核对 **Prometheus targets** 与 **Nacos 注册 IP** |
| `mysql` | 实例、连接、慢查询、行锁（Exporter） | **mysql_inspect** → **metrics_ops**（mysqld_exporter 指标）→ **nacos_config**（`shared-mysql-tuning.yml` 等，视权限） |
| `redis` | 内存、连接、命中率、阻塞 | **redis_inspect** → **metrics_ops**（redis_exporter） |
| `rocketmq` | Broker、堆积、DLQ | **rocketmq_inspect** → **metrics_ops** → **elasticsearch_ops**（业务失败日志） |

## 2. 相关文档

- 逐条告警名：[`by-category/README.md`](by-category/README.md)
- 流水线：[`overview-alert-pipeline.md`](overview-alert-pipeline.md)

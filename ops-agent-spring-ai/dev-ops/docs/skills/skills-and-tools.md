# Skill & Function Tool 清单

> 本文档是 `ops-agent-spring-ai` 的「运维工具事实来源」。维护者、SOP 编写者、AI 在引用工具名和参数时，应以本文档和源码（`skill/**/*SkillRegistry.java`、`agent/sub/*ISubAgent.java`）为准。

## 总览

`ops-agent-spring-ai` 拥有 **8 个运维 Skill 域**，每个域对应：

1. 一个 `OpsSkillRegistry`（定义了该域的具体 Function Tool）
2. 一个 `*ISubAgent`（子 ReAct Agent，封装成父 Agent 可调用的单入口工具）

两层调用关系如下：

- **父 ReAct Agent** 看到的是 **8 个 `*_skill`**（如 `catalog_skill`、`docker_skill`、`mysql_skill`），通过 `task` 参数传入自然语言任务。
- **子 Agent** 收到任务后，在自己的 ReAct 循环中调用该域的 **具体 Function Tool**（如 `docker_logs`、`mysql_processlist`）。

```
父 Agent (ParentReactAgent)
    │
    ├── catalog_skill ─────┐
    ├── docker_skill ───────┐
    ├── mysql_skill ────────┤
    ├── redis_skill ────────┤
    ├── nacos_skill ────────┤───> SubAgent (ISubAgent)
    ├── prometheus_skill ───┤      │
    ├── elasticsearch_skill─┤      └── 调用该域内部 Function Tool
    └── rocketmq_skill ─────┘          (通过 MasterRegistry.execute)
```

## 父 Agent 工具（Sub Agent 入口）

父 Agent 暴露给 LLM 的 8 个工具，由 `AgentToolRegistry` 组装，来源为 `ISubAgent` 接口。

| Parent Tool Name | 对应 Skill ID | 父工具描述 |
|---|---|---|
| `catalog_skill` | `catalog_ops` | Catalog Skill：可先列出当前服务名或 Topic，再查询 service/application/compose service/container/configEntries 以及 topic/table/pool 静态归属。 |
| `docker_skill` | `docker_ops` | Docker Skill：日志、stats、inspect、受控 exec。传入 task 为自然语言任务说明。 |
| `mysql_skill` | `mysql_inspect` | MySQL Skill：只读诊断会话、状态、锁、慢查询、SELECT 执行计划。传入 task 为自然语言任务说明。 |
| `redis_skill` | `redis_inspect` | Redis Skill：只读 INFO、慢日志、客户端、内存、GET/SCAN。传入 task 为自然语言任务说明。 |
| `nacos_skill` | `nacos_config` | Nacos Skill：配置读/写（写可能审批）、服务实例与服务列表。读配置前必须先拿到 Catalog 返回的明确 dataId/group。 |
| `prometheus_skill` | `metrics_ops` | Prometheus Skill：Sentinel/DynamicTP/JVM/业务 PromQL。传入 task 为自然语言任务说明。 |
| `elasticsearch_skill` | `elasticsearch_ops` | Elasticsearch Skill：按服务检索错误日志、索引、搜索、计数、聚合（应用日志 nexus-* 与 SkyWalking sw_*；args 可带 cluster=skywalking）。默认先按服务查错误样本。 |
| `rocketmq_skill` | `rocketmq_inspect` | RocketMQ Skill：Topic 路由、消费统计、死信线索。传入 task 为自然语言任务说明。 |

## 子 Skill 内部 Function Tool

### 1. `catalog_ops`

> 底层实现：`CatalogToolkit`，基于 `src/main/resources/ops-catalog/catalog.json` 做只读静态知识库查询。

| 工具名 | 入参 | 描述 |
|---|---|---|
| `catalog_resolve_service` | `query` (string, 可选) 或 `service/application/resource/topic/consumerGroup/table/database/pool` | 根据自由文本或结构化线索归因服务。 |
| `catalog_list_services` | 无 | 列出当前已知标准服务名清单。 |
| `catalog_list_topics` | 无 | 列出当前已知 Topic 名清单。 |
| `catalog_describe_service` | `service` (string, 必填) | 返回某个服务对应的 application、compose service、container、configEntries/configDataIds、topic、database、pool 等静态拓扑。 |
| `catalog_lookup_resource_owner` | `kind` (string, 必填), `value` (string, 必填) | 按 resource/topic/consumerGroup/table/database/pool 反查归属。 |

**设计原则**：问题很模糊时，先用 `catalog_list_services` 或 `catalog_list_topics` 缩小范围，再用 `catalog_describe_service` 补全容器名、配置入口、Topic、数据库名等静态线索。

---

### 2. `docker_ops` — 详见 [`docker.md`](docker.md)

> 底层实现：`DockerToolkit`，通过 Docker Engine API（默认 `unix:///var/run/docker.sock`）连接。

| 工具名 | 入参 | 描述 |
|---|---|---|
| `docker_logs` | `container` (string, 必填), `tail` (number, 可选, 默认 100, 最大 5000) | 拉取容器 stdout/stderr 日志，最长 120KB 截断。 |
| `docker_stats` | `container` (string, 必填) | 获取容器资源统计（CPU、内存、IO 等）。 |
| `docker_inspect` | `container` (string, 必填) | 获取容器元数据。容器不存在（not found）是「服务未部署」的关键证据。 |
| `docker_exec` | `container` (string, 必填), `command` (string, 必填) | 在容器内执行 `sh -c <command>`，60 秒超时。 |

**设计原则**：容器不存在时，直接报告「Docker 未发现该服务容器」，不再继续查 logs/stats。

---

### 3. `mysql_inspect` — 详见 [`mysql.md`](mysql.md)

> 底层实现：`MysqlToolkit`，通过 JDBC 连接 MySQL 执行只读查询。

| 工具名 | 入参 | 描述 |
|---|---|---|
| `mysql_processlist` | 无 | `SHOW PROCESSLIST`，查看当前连接与会话。 |
| `mysql_status` | `pattern` (string, 可选) | `SHOW GLOBAL STATUS LIKE <pattern>`，查看全局状态变量。 |
| `mysql_locks` | 无 | 查询 `performance_schema.metadata_locks` 样例，排查元数据锁。 |
| `mysql_slow_query` | 无 | 查询 `mysql.slow_log` 最近行（需开启 slow_log 表）。 |
| `mysql_explain_sql` | `sql` (string, 必填) | 对单条 `SELECT` 或 `WITH ... SELECT` 执行 `EXPLAIN FORMAT=JSON`。 |

---

### 4. `redis_inspect` — 详见 [`redis.md`](redis.md)

> 底层实现：`RedisToolkit`，通过 Redisson 连接 Redis。

| 工具名 | 入参 | 描述 |
|---|---|---|
| `redis_info` | `section` (string, 可选) | 执行 `INFO [section]`，查看 Redis 服务器信息。 |
| `redis_slowlog` | `count` (number, 可选, 默认 32) | 执行 `SLOWLOG GET <count>`，查看慢查询日志。 |
| `redis_client_list` | 无 | 执行 `CLIENT LIST`，查看客户端连接详情。 |
| `redis_memory` | 无 | 执行 `INFO memory`，查看内存使用详情。 |
| `redis_get` | `key` (string, 可选) 或 `scanPattern` + `scanCount` (number, 可选, 默认 50) | 直接 `GET` 或受控 `SCAN` 采样，避免全量遍历。 |

---

### 5. `nacos_config` — 详见 [`nacos.md`](nacos.md)

> 底层实现：`NacosToolkit`，通过 Nacos Java SDK 连接配置中心与服务发现。

| 工具名 | 入参 | 描述 | 需审批 |
|---|---|---|---|
| `nacos_get_config` | `dataId` (string, 必填), `group` (string, 可选, 默认 `DEFAULT_GROUP`) | 读取 Nacos 配置内容。`dataId/group` 必须来自 Catalog 返回的配置入口，不支持 `*` 或一次传多个值。 | 否 |
| `nacos_publish_config` | `dataId` (string, 必填), `group` (string, 可选), `content` (string, 必填) | 发布/修改 Nacos 配置。**高危写操作，需人工审批**。 | **是** |
| `nacos_list_instances` | `serviceName` (string, 必填), `group` (string, 可选) | 查询服务健康实例列表。空列表是「服务未注册或无健康实例」的关键证据。 | 否 |
| `nacos_list_services` / `nacos_get_services` | `pageNo` (number, 可选, 默认 1), `pageSize` (number, 可选, 默认 100) | 分页列出注册的服务名。两个名称是别名，行为一致。 | 否 |

**设计原则**：排查服务错误率/不可用时，先用 `nacos_list_instances` 确认服务存在；空实例列表直接报告，不继续查无关配置。读配置前先从 Catalog 拿明确 `dataId/group`。配置修改必须走 `nacos_publish_config` + 审批。

---

### 6. `metrics_ops` — 详见 [`prometheus.md`](prometheus.md)

> 底层实现：`PrometheusToolkit`，通过 HTTP 调用 Prometheus `/api/v1/query`（Instant Query）。

| 工具名 | 入参 | 描述 |
|---|---|---|
| `sentinel_metrics` | `promql` (string, 可选, 默认 `up`) | Sentinel 限流熔断相关 PromQL 查询。 |
| `dynamictp_metrics` | `promql` (string, 可选, 默认 `process_threads`) | DynamicTP 线程池相关 PromQL 查询。 |
| `jvm_metrics` | `promql` (string, 可选, 默认 `jvm_memory_used_bytes`) | JVM 进程指标 PromQL 查询。 |
| `business_metrics` | `promql` (string, 可选, 默认 `up`) | 业务自定义指标 PromQL 查询。 |

**设计原则**：确认服务是否被 Prometheus 采集时，优先查 `up{application="<app>"}`。若结果为空，直接报告「Prometheus 未发现该服务」，不再猜业务指标名。

---

### 7. `elasticsearch_ops` — 详见 [`elasticsearch.md`](elasticsearch.md)

> 底层实现：`ElasticsearchToolkit`，通过 Elasticsearch REST Client 连接。支持两个集群：
> - `logs`（默认）：应用日志，索引 `nexus-*`
> - `skywalking` / `sw`：SkyWalking 存储，索引 `sw_segment-*`、`sw_metrics-*`

| 工具名 | 入参 | 描述 |
|---|---|---|
| `es_indices` | `cluster` (string, 可选, 默认 `logs`, 可选 `skywalking`/`sw`) | 列出集群索引。 |
| `es_search_service_errors` | `service`/`application` (至少一个), `lookback` (string, 可选, 默认 `1h`), `size` (number, 可选), `keywords` (array/string, 可选), `index` (string, 可选), `cluster` (string, 可选) | 按服务检索错误日志摘要，默认入口。 |
| `es_search` | `index` (string, 必填), `query` (string, 可选), `cluster` (string, 可选) | DSL 搜索，query 为 JSON 字符串。 |
| `es_count` | `index` (string, 必填), `query` (string, 可选), `cluster` (string, 可选) | 指定索引文档计数。 |
| `es_aggregation` | `index` (string, 必填), `body` (string, 必填), `cluster` (string, 可选) | terms / date_histogram 等聚合查询。 |

**设计原则**：优先用 `es_search_service_errors` 按服务名和时间窗口查错误样本，只有高层入口不足以回答问题时再手写 DSL。`es_search` 允许 bare query clause，工具会自动补 `query` 包装。

---

### 8. `rocketmq_inspect` — 详见 [`rocketmq.md`](rocketmq.md)

> 底层实现：`RocketMqToolkit`，通过 RocketMQ Admin 工具类连接 NameServer。

| 工具名 | 入参 | 描述 |
|---|---|---|
| `mq_topic_stats` | `topic` (string, 可选, 空则列出所有 topic) | 查看 Topic 路由或列表。 |
| `mq_consumer_status` | `consumerGroup` (string, 必填), `topic` (string, 可选) | 查看消费者组的消费进度、堆积情况。 |
| `mq_dead_letter` | `topic` (string, 可选), `consumerGroup` (string, 可选) | 返回 DLQ（死信队列）排查提示与路由信息。 |

---

## 写操作审批说明

当前仅 **1 个工具**需要人工审批：

| 工具名 | 所在 Skill | 审批触发点 |
|---|---|---|
| `nacos_publish_config` | `nacos_config` | 发布/修改 Nacos 配置时，由 `ToolApprovalRuleFilter` 拦截，生成审批单，挂起执行，等待人工通过 `ApprovalController` 审批。 |

其他所有工具均为**只读操作**，直接执行。

## 相关文档

| 文档 | 内容 |
|------|------|
| [`alerts/sop-tool-mapping.md`](../alerts/sop-tool-mapping.md) | 告警 category → Skill 工具建议排查路径 |
| [`alerts/by-category/README.md`](../alerts/by-category/README.md) | 按 Prometheus category 的告警条目 |
| [`../ops-agent/architecture.md`](../ops-agent/architecture.md) | 项目架构与流程图 |
| [`../ops-agent/api.md`](../ops-agent/api.md) | HTTP API 概览 |

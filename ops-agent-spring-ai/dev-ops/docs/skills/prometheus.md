# metrics_ops — Prometheus 指标 Skill

- **Skill ID**: `metrics_ops`
- **父 Agent 工具名**: `prometheus_skill`
- **父工具描述**: Prometheus Skill：Sentinel/DynamicTP/JVM/业务 PromQL。传入 task 为自然语言任务说明。
- **底层实现**: `PrometheusToolkit` (`skill/prometheus/PrometheusToolkit.java`)
- **Skill 注册类**: `MetricsSkillRegistry` (`skill/prometheus/MetricsSkillRegistry.java`)
- **子 Agent**: `MetricsISubAgent` (`agent/sub/MetricsISubAgent.java`)

## Function Tool 清单

### `sentinel_metrics`

Sentinel 限流熔断相关 PromQL 查询。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `promql` | string | 否 | `up` | 自定义 PromQL 表达式，覆盖默认值 |

**行为**: 调用 Prometheus HTTP API `/api/v1/query`（Instant Query）。默认查询 `up`，可传入任意 PromQL 覆盖。

**典型覆盖 PromQL**:
- `up{application="mall"}` — 确认服务是否存活
- `sentinel_pass_requests_total{application="mall"}` — Sentinel 通过请求数
- `sentinel_block_requests_total{application="mall"}` — Sentinel 限流阻断数
- `sentinel_rt_seconds_count{application="mall"}` — Sentinel RT 统计

---

### `dynamictp_metrics`

DynamicTP 线程池相关 PromQL 查询。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `promql` | string | 否 | `process_threads` | 自定义 PromQL 表达式，覆盖默认值 |

**行为**: 调用 Prometheus HTTP API `/api/v1/query`（Instant Query）。默认查询 `process_threads`，可传入任意 PromQL 覆盖。

**典型覆盖 PromQL**:
- `dtp_thread_pool_active_count{application="order-service"}` — 线程池活跃线程数
- `dtp_thread_pool_queue_size{application="order-service"}` — 线程池队列大小
- `dtp_thread_pool_rejected_count{application="order-service"}` — 线程池拒绝任务数

---

### `jvm_metrics`

JVM 进程指标 PromQL 查询。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `promql` | string | 否 | `jvm_memory_used_bytes` | 自定义 PromQL 表达式，覆盖默认值 |

**行为**: 调用 Prometheus HTTP API `/api/v1/query`（Instant Query）。默认查询 `jvm_memory_used_bytes`，可传入任意 PromQL 覆盖。

**典型覆盖 PromQL**:
- `jvm_memory_used_bytes{application="mall"}` — JVM 内存使用
- `jvm_gc_pause_seconds_count{application="mall"}` — GC 次数
- `jvm_gc_pause_seconds_sum{application="mall"}` — GC 耗时
- `process_cpu_usage{application="mall"}` — CPU 使用率

---

### `business_metrics`

业务自定义指标 PromQL 查询。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `promql` | string | 否 | `up` | 自定义 PromQL 表达式，覆盖默认值 |

**行为**: 调用 Prometheus HTTP API `/api/v1/query`（Instant Query）。默认查询 `up`，可传入任意 PromQL 覆盖。此工具为通用指标查询入口，无特定默认场景。

**典型覆盖 PromQL**:
- `http_server_requests_seconds_count{application="mall",status=~"5.."}` — 5xx 错误数
- `dubbo_consumer_requests_total{application="order-service"}` — Dubbo 消费端请求数
- `dubbo_provider_requests_total{application="pay"}` — Dubbo 服务端请求数

## 设计原则

**服务存在性优先**（由子 Agent System Prompt 保证）：

1. 确认服务是否被 Prometheus 采集时，优先查 `up{application="<app>"}`、`up{app="<app>"}`、`up{job="<app>"}`。
2. 如果这些查询 result 为空，直接报告「Prometheus 未发现该服务时序/标签可能不一致」，不要继续猜业务指标名。
3. 若要查 5xx，再基于已有标签查 `http_server_requests_seconds_count` / `*_requests_total` 等；没有基础 `up` 时先返回不存在证据。

## 返回值格式

所有工具均返回 Prometheus Instant Query 的 JSON 结果，由 `PrometheusToolkit` 解析后包装为：

```json
{"status":"ok","key":"query","value":"{...Prometheus 原始 result...}"}
```

若 PromQL 语法错误或查询超时，返回：
```json
{"status":"error","value":"query 失败: ..."}
```

## 审批标记

全部工具均为 **只读操作**（HTTP GET），无需审批。

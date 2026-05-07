# elasticsearch_ops — Elasticsearch 诊断 Skill

- **Skill ID**: `elasticsearch_ops`
- **父 Agent 工具名**: `elasticsearch_skill`
- **父工具描述**: Elasticsearch Skill：索引、搜索、计数、聚合（应用日志 nexus-* 与 SkyWalking sw_*；args 可带 cluster=skywalking）。传入 task 为自然语言任务说明。
- **底层实现**: `ElasticsearchToolkit` (`skill/elasticsearch/ElasticsearchToolkit.java`)
- **Skill 注册类**: `ElasticsearchSkillRegistry` (`skill/elasticsearch/ElasticsearchSkillRegistry.java`)
- **子 Agent**: `ElasticsearchISubAgent` (`agent/sub/ElasticsearchISubAgent.java`)

## 集群切换

此 Skill 支持连接 **两个独立的 ES 集群**，通过 `cluster` 参数切换：

| cluster 值 | 含义 | 典型索引 |
|---|---|---|
| `logs`（默认） | 应用日志 ELK 集群 | `nexus-*` |
| `skywalking` / `sw` | SkyWalking OAP 存储集群 | `sw_segment-*`, `sw_metrics-*` |

**说明**: `skywalking` 和 `sw` 是同一集群的两种写法，底层实现会自动归一化为同一连接。

## Function Tool 清单

### `es_indices`

列出集群索引。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `cluster` | string | 否 | `logs` | `logs`（应用日志）或 `skywalking`/`sw`（SkyWalking 存储） |

**行为**: 通过 Elasticsearch REST Client 执行 `GET /_cat/indices?format=json` 或等效查询。返回索引列表，包含 `index`, `health`, `status`, `docs.count`, `store.size` 等字段。

---

### `es_search`

DSL 搜索。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `index` | string | 是 | — | 目标索引名，如 `nexus-2026.05.07` |
| `query` | string | 否 | `""` | Elasticsearch Query DSL JSON 字符串 |
| `cluster` | string | 否 | `logs` | 集群选择 |

**行为**: 若 `query` 为空，执行 `match_all` 查询；否则解析 `query` 为 JSON 执行搜索。返回搜索结果命中文档（默认分页由 `ElasticsearchToolkit` 控制）。

**query 参数示例**:
```json
{"bool":{"must":[{"match":{"serviceName":"order-service"}},{"range":{"@timestamp":{"gte":"now-1h"}}}]}}
```

---

### `es_count`

文档计数。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `index` | string | 是 | — | 目标索引名 |
| `query` | string | 否 | `""` | Elasticsearch Query DSL JSON 字符串 |
| `cluster` | string | 否 | `logs` | 集群选择 |

**行为**: 若 `query` 为空，返回索引总文档数；否则返回匹配 query 的文档计数。

---

### `es_aggregation`

聚合查询（terms / date_histogram / stats 等）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `index` | string | 是 | — | 目标索引名 |
| `body` | string | 是 | — | 完整的聚合请求体 JSON 字符串 |
| `cluster` | string | 否 | `logs` | 集群选择 |

**行为**: 将 `body` 解析为 JSON 后发送聚合请求。返回聚合结果桶（buckets）和指标值。

**body 参数示例**（按状态码统计 HTTP 请求数）:
```json
{
  "size": 0,
  "aggs": {
    "status_codes": {
      "terms": {
        "field": "status_code",
        "size": 10
      }
    }
  }
}
```

**body 参数示例**（按小时统计日志量）:
```json
{
  "size": 0,
  "aggs": {
    "by_hour": {
      "date_histogram": {
        "field": "@timestamp",
        "calendar_interval": "1h"
      }
    }
  }
}
```

## 审批标记

全部工具均为 **只读操作**，无需审批。

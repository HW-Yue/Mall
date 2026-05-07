# redis_inspect — Redis 诊断 Skill

- **Skill ID**: `redis_inspect`
- **父 Agent 工具名**: `redis_skill`
- **父工具描述**: Redis Skill：只读 INFO、慢日志、客户端、内存、GET/SCAN。传入 task 为自然语言任务说明。
- **底层实现**: `RedisToolkit` (`skill/redis/RedisToolkit.java`)
- **Skill 注册类**: `RedisSkillRegistry` (`skill/redis/RedisSkillRegistry.java`)
- **子 Agent**: `RedisISubAgent` (`agent/sub/RedisISubAgent.java`)

## Function Tool 清单

### `redis_info`

查看 Redis 服务器信息。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `section` | string | 否 | `""` | INFO 子章节，如 `memory`, `clients`, `persistence`, `stats`, `replication`, `cpu`, `commandstats`, `cluster`, `keyspace` |

**行为**: 执行 `INFO [section]`。空字符串时返回全部信息。返回原始 INFO 文本格式。

---

### `redis_slowlog`

查看慢查询日志。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `count` | number | 否 | `32` | 返回最近 N 条慢查询记录 |

**行为**: 执行 `SLOWLOG GET <count>`。返回每条记录的 `id`, `timestamp`, `duration`（微秒）, `command` 数组。

---

### `redis_client_list`

查看客户端连接详情。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| — | — | — | — | 无参数 |

**行为**: 执行 `CLIENT LIST`。返回每个客户端的 `id`, `addr`, `fd`, `name`, `age`, `idle`, `flags`, `db`, `sub`, `psub`, `multi`, `qbuf`, `qbuf-free`, `obl`, `oll`, `omem`, `events`, `cmd`, `user`。

---

### `redis_memory`

查看内存使用详情。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| — | — | — | — | 无参数 |

**行为**: 执行 `INFO memory` 的快捷方式。返回 `used_memory`, `used_memory_rss`, `used_memory_peak`, `used_memory_lua`, `mem_fragmentation_ratio`, `maxmemory`, `maxmemory_policy` 等内存指标。

---

### `redis_get`

读取指定 key 或受控 SCAN 采样。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `key` | string | 否 | `""` | 直接执行 `GET <key>` |
| `scanPattern` | string | 否 | `""` | SCAN 匹配模式，如 `order:*` |
| `scanCount` | number | 否 | `50` | SCAN 的 count 参数，控制单次迭代采样量 |

**行为**（互斥逻辑，由子 Agent 根据任务决定传哪个参数）：

1. 若传入 `key`，执行 `GET <key>`，返回字符串值。
2. 若传入 `scanPattern`，执行 `SCAN 0 MATCH <pattern> COUNT <scanCount>`，返回匹配到的 key 列表（受控采样，避免全量遍历）。
3. 若 `key` 为空且 `scanPattern` 也为空，行为由 `RedisToolkit` 实现决定（通常返回空）。

**安全设计**: 不暴露 `KEYS *` 等全量扫描命令，SCAN 采样量由 `scanCount` 上限控制。

## 审批标记

全部工具均为 **只读操作**，无需审批。

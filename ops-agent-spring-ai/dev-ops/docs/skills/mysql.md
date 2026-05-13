# mysql_inspect — MySQL 诊断 Skill

- **Skill ID**: `mysql_inspect`
- **父 Agent 工具名**: `mysql_skill`
- **父工具描述**: MySQL Skill：只读诊断会话、状态、锁、慢查询、SELECT 执行计划。传入 task 为自然语言任务说明。
- **底层实现**: `MysqlToolkit` (`skill/mysql/MysqlToolkit.java`)
- **Skill 注册类**: `MysqlSkillRegistry` (`skill/mysql/MysqlSkillRegistry.java`)
- **子 Agent**: `MysqlISubAgent` (`agent/sub/MysqlISubAgent.java`)

## Function Tool 清单

### `mysql_processlist`

查看当前 MySQL 连接与会话。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| — | — | — | — | 无参数 |

**行为**: 执行 `SHOW PROCESSLIST`。返回结果中包含每个连接的 `Id`, `User`, `Host`, `db`, `Command`, `Time`, `State`, `Info`。

---

### `mysql_status`

查看 MySQL 全局状态变量。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `pattern` | string | 否 | `""` | 传给 `SHOW GLOBAL STATUS LIKE '<pattern>'`。空字符串表示查询全部。 |

**行为**: 若 `pattern` 为空，执行 `SHOW GLOBAL STATUS`；否则执行 `SHOW GLOBAL STATUS LIKE '<pattern>'`。用于快速定位 Threads_connected、Questions、Slow_queries 等关键指标。

---

### `mysql_locks`

查询元数据锁样例。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| — | — | — | — | 无参数 |

**行为**: 查询 `performance_schema.metadata_locks`，返回当前元数据锁持有和等待情况的样例数据。用于排查 DDL 阻塞、表锁等待等问题。

---

### `mysql_slow_query`

查看慢查询表样例。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| — | — | — | — | 无参数 |

**行为**: 查询 `mysql.slow_log` 表最近行。前提是 MySQL 已开启 `slow_log` 表（非文件模式）。返回字段包括 `start_time`, `query_time`, `lock_time`, `rows_sent`, `rows_examined`, `sql_text` 等。

**前置条件**: `log_output = TABLE` 且 `slow_query_log = ON`，否则可能返回空。

---

### `mysql_explain_sql`

查看 `SELECT` 语句的执行计划（JSON）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `sql` | string | 是 | — | 仅允许单条 `SELECT` 或 `WITH ... SELECT` 语句 |

**行为**: 对输入 SQL 执行 `EXPLAIN FORMAT=JSON <sql>`，返回 JSON 形式的执行计划，便于 Agent 或前端做结构化消费。

**限制**:
- 仅允许 `SELECT` 或 `WITH ... SELECT`
- 不允许多语句
- 不支持 `EXPLAIN ANALYZE`

## 审批标记

全部工具均为 **只读操作**，无需审批。

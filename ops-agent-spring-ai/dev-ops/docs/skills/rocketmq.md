# rocketmq_inspect — RocketMQ 诊断 Skill

- **Skill ID**: `rocketmq_inspect`
- **父 Agent 工具名**: `rocketmq_skill`
- **父工具描述**: RocketMQ Skill：Topic 路由、消费统计、死信线索。传入 task 为自然语言任务说明。
- **底层实现**: `RocketMqToolkit` (`skill/rocketmq/RocketMqToolkit.java`)
- **Skill 注册类**: `RocketMqSkillRegistry` (`skill/rocketmq/RocketMqSkillRegistry.java`)
- **子 Agent**: `RocketMqISubAgent` (`agent/sub/RocketMqISubAgent.java`)

## Function Tool 清单

### `mq_topic_stats`

查看 Topic 路由或 Topic 列表。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `topic` | string | 否 | `""` | Topic 名；为空时返回所有 Topic 列表 |

**行为**: 若 `topic` 为空，通过 RocketMQ Admin 查询所有 Topic 列表；若指定了 Topic 名，查询该 Topic 的路由信息（Broker 分布、队列数、读写权限等）。

---

### `mq_consumer_status`

查看消费者组的消费进度与堆积情况。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `consumerGroup` | string | 是 | — | 消费者组名 |
| `topic` | string | 否 | `""` | 可选的 Topic 过滤 |

**行为**: 通过 RocketMQ Admin 查询指定消费者组的消费状态。返回每个队列的 `brokerName`, `queueId`, `brokerOffset`, `consumerOffset`, `diff`（堆积量）等信息。`diff` 大于 0 表示存在消息堆积。

---

### `mq_dead_letter`

DLQ（死信队列）排查提示与路由。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `topic` | string | 否 | `""` | 关联的原始 Topic |
| `consumerGroup` | string | 否 | `""` | 关联的消费者组 |

**行为**: 返回死信队列的排查提示信息，包括：
1. 死信 Topic 命名规则：`%DLQ%<consumerGroup>`
2. 死信产生原因（消费失败重试超限）
3. 当前 DLQ 消息堆积量估算
4. 建议处理方案（人工排查后重投或清理）

此工具不直接消费 DLQ 消息，仅提供排查线索和路由信息。

## 审批标记

全部工具均为 **只读操作**，无需审批。

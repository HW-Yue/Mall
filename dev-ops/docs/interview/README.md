# Nexus 面试讲稿集

按岗位方向准备的 4 套讲稿，**同一项目，不同火力**。

## 索引

| 文件 | 适用方向 | 主线 | ops-agent 占比 |
|------|---------|------|---------------|
| [mid-level.md](./mid-level.md) | Java 后端 3–5 年（中级） | 交易链路 + MQ + DDD 工程化 | 一句话带过 |
| [senior.md](./senior.md) | Java 后端 5–8 年（资深） | 架构权衡 + 动态化 + 可观测 | 作为差异化亮点 |
| [ai-engineer.md](./ai-engineer.md) | AI 应用 / Agent 工程方向 | ops-agent（SOP→ReAct→8 域 Skill→审批） | 主线 |
| [architect.md](./architect.md) | 架构师 / 技术专家 | 跨服务边界 + 闭环治理 + 演进路径 | 三位一体之一 |

## 通用素材（4 个版本共享，背下来）

### 项目一句话定位

> Nexus 是一个 DDD 六边形架构的电商交易系统，包含 6 个业务微服务 + 1 个 AI 运维 Agent，覆盖普通商城、拼团、秒杀三类完整交易链路，并在监控告警侧用 Spring AI Alibaba 做了 SOP 驱动的 ReAct Agent 闭环。

### 规模数据（来自代码事实，可直接引用）

- **7 个微服务**：springcloud-gateway / mall / order-service / pay / group-buy-service / seckill-service / ops-agent-spring-ai
- **DDD 六层**：api / app / domain / trigger / infrastructure / types
- **29 个 MQ 消费者**，3 条 `pay-success-*` 按 marketType 拆分的 Topic
- **5 个业务库** + Grafana 库；MySQL 业务库自动重放种子 SQL
- **11 个 Nacos DataId**，覆盖 Sentinel / DynamicTp / Hikari / RocketMQ 消费者 / 运行时属性
- **双 Spring Boot 版本**：业务侧 3.2.12 / Java 21；ops-agent 3.4.5 / Java 21
- **ops-agent 8 个 Skill 域**：docker_ops / mysql_inspect / rocketmq_inspect / metrics_ops（Prometheus）/ elasticsearch_ops / redis_inspect / nacos_config / catalog

### 数据口径声明（**所有讲稿必须遵守**）

| 数据类型 | 允许的说法 | 禁止的说法 |
|---------|-----------|-----------|
| QPS / TPS | "压测环境单机 xxx" / "设计目标 xxx" | "线上 xxx" / "生产 xxx" |
| 故障案例 | "压测中观察到" / "设计上规避了" | "线上事故" / "P0 故障" |
| 用户量 | 不提 | "百万用户" / "DAU xxx" |
| ops-agent 诊断时长 | "对照人工排查基线，从 ~15min 降到 ~3min（设计目标 + 演示环境实测）" | 不能说真实运维降本 |

**核心原则**：宁可少说一个数字，也不要让面试官追问"线上数据怎么来的"时露馅。

### 通用追问防御（4 个版本都可能被问到）

1. **"这是几个人做的？"** → 个人项目，我负责整体架构与全部代码；目的是练手 + 作品集，所以刻意把多种典型场景（高并发、AI、动态化、可观测）都做一遍。
2. **"压测怎么压的？"** → 本地 docker compose 起一套环境，用 JMeter / wrk 打单机，主要看链路通畅性、降级行为、热更新生效，不追求绝对 TPS 数字。
3. **"为什么这么多服务自己做？"** → 故意把每条交易链路拆成独立服务而不是单体，是为了演示"服务边界怎么划""跨服务一致性怎么处理"——这是单体项目练不到的。
4. **"AI Agent 是套壳吗？"** → 不是只调一次大模型。具体看 [ai-engineer.md](./ai-engineer.md)，包含 ReAct 多轮、子 Agent 委派、Wrench 规则链、审批流。

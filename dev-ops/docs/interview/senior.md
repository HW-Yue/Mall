# 资深版讲稿（Java 后端 5–8 年）

**主线**：架构权衡 + 动态化 + 可观测
**目标**：每个亮点都要能讲出"我考虑过哪几种方案，最后为什么选这个"

> 数据口径：本项目压测过、无生产流量。所有数字标注压测/设计目标，避免编造生产事故。

---

## 一、1 分钟项目总览（自我介绍用）

> Nexus 是我个人主导设计的一个**电商交易 + AI 运维**完整体系。业务侧 6 个微服务覆盖普通商城、拼团、秒杀三类交易链路，DDD 六边形分层，技术栈 Spring Boot 3.2 + Spring Cloud Alibaba + RocketMQ 5 + Redisson + 支付宝沙箱。
>
> 我设计时刻意把几个有挑战的场景全做一遍：**支付标识 outTradeNo 在订单和支付服务内部收口**、**普通/拼团/秒杀三链路异步落库 + 兜底重试**、**Sentinel / Hikari / DynamicTp / RocketMQ 消费线程池全部接入 Nacos 动态刷新**、**Prometheus + ELK + SkyWalking + Alertmanager 全栈可观测**。
>
> 最后还做了一个差异化亮点——基于 Spring AI Alibaba 的 **运维 Agent（ops-agent-spring-ai）**，把 Alertmanager 告警接进来，按 SOP 规则匹配后用 ReAct 编排 8 个领域 Skill（Docker / MySQL / RocketMQ / Prometheus / ES / Redis / Nacos / Catalog）做自动诊断，写操作走内存审批队列。

---

## 二、核心亮点（STAR 深挖）

### 亮点 1：outTradeNo 收口与营销服务解耦的边界设计

**S（背景）**
项目有三种交易类型：普通商城、拼团、秒杀。三者业务规则迥异，但都要落地到统一的订单和支付流程。早期的草稿里 `outTradeNo`（支付平台单号）一度暴露给营销服务和前端，后果是**任何一个营销服务出 bug 都可能写错支付单号**，对账成本极高。

**T（任务）**
在所有模块之间画一条干净的边界，确保支付标识的"真相源"只有一个，跨服务通信只用 `orderId`。

**A（行动）**
1. **硬约束写入 CLAUDE.md 顶部**，作为整个仓库的第一原则：`outTradeNo` 只允许出现在 `order-service` / `pay-service` 的库表、内部 DTO、内部 MQ 和两者之间的调用中。营销服务的本地表、MQ、HTTP 出入参一律不能有这个字段。
2. **退款链路重新设计为双跳**：
   - 营销服务 `/refund` → HTTP 调 `order-service.refund_execute`
   - `order-service` 持有 `outTradeNo`，发事务消息 `pay-refund-{type}` 给 `pay-service`
   - pay 完成退款后回写 `pay-refund-{type}-result` 给 `order-service`
   - `order-service` 再发只带 `orderId` 的 `order-refund-{type}` 给营销服务做本地补偿
3. **关单链路同样收口**：`order-close-{type}` 只给 pay 和 order，营销侧消费的是单独的 `order-close-{type}-market` Topic。

**权衡过的方案**：
- **方案 A（已否决）**：双向直连——营销服务直接监听 `pay-refund-result-*`。简单，但等于把 outTradeNo 泄露给营销服务，违反收口原则。
- **方案 B（已否决）**：Saga 编排——需要引入 Seata，所有服务都得改 SQL 加 undo_log，复杂度收益不匹配。
- **方案 C（采用）**：双跳 + 事务消息——`order-service` 作为状态中枢，单向流转。

**R（结果 / 设计指标）**
- 营销服务代码里**零** `outTradeNo` 字段；
- 新增一种交易类型只需要加一个 marketType + 一个 pay-success Topic + 一个 market Topic，订单和支付逻辑零改动；
- 对账只需要 order 库和 pay 库两方比对。

---

### 亮点 2：MQ 消息分类原则（事务消息 vs 普通消息）

**S（背景）**
项目里有 29 个 MQ 消费者，**不可能全部用事务消息**——事务消息有性能开销和回查复杂度，但又不能全用普通消息，关键节点要保证消息和本地表的原子性。

**T（任务）**
建立一套清晰的"哪些用事务消息、哪些用普通消息"的分类原则，并能讲出每一个分类的理由。

**A（行动）**

| 场景 | 类型 | 理由 |
|------|------|------|
| `pay-refund-{normal/group-buy/seckill}` | **事务消息** | 退款必须保证"扣支付单状态"和"发消息通知 order"在同一事务里，否则会出现资金状态和业务状态不一致 |
| `order-ship-task` | **事务消息** | 发货任务和订单状态扭转必须原子 |
| `normal-order-create` / `group-buy-order-create` / `seckill-order-create` | **普通消息** | 下单时**还没有持久状态可保护**（订单本来就是要异步落库的），用普通消息 + Redis 存在标记 + 6×50ms 兜底就足够 |
| `pay-success-{type}` | **普通消息** | pay 那边已经落库了，发消息失败可以靠定时任务对账补偿，没必要付出事务消息的代价 |
| `order-paid-{type}` / `order-refund-{type}` | **普通消息** | 下游消费者本身就要做幂等（按 orderId 唯一索引），重复或丢失都能兜住 |

**关键洞察**：事务消息保护的是 **"本地状态变更" + "发消息"** 的原子性。如果本地变更已经在事务里完成（pay 写完了支付单），那么发消息丢失最坏情况只是延迟，靠对账兜底；只有"边变更边发"的场景才真正需要事务消息。

**R（结果 / 设计指标）**
- 9 个事务消息 / 20 个普通消息，分类清晰可解释；
- 压测中刻意 kill 消费者验证：事务消息节点零丢失，普通消息节点最坏 5 分钟内被定时对账任务补偿。

---

### 亮点 3：动态化配置矩阵（5 类配置全热更新）

**S（背景）**
压测时经常需要现场调参——连接池小了、线程池打满了、限流阈值不对——如果每次都重启服务，调参成本高且会影响压测连续性。

**T（任务）**
让所有"可调"参数都从 Nacos 动态刷新，**5 秒内生效，无需重启**。

**A（行动）**
| 类型 | 实现方式 | 难点 |
|------|---------|------|
| **Sentinel 规则** | 各服务直连 Nacos JSON 规则源，**不走 Spring 主配置树**，避免规则更新触发整个 `@RefreshScope` 重建 | 规则源要解耦，否则会污染主配置 |
| **Hikari 连接池** | `HikariPoolDynamicRefresher` 监听 `EnvironmentChangeEvent`，**只刷新可热改参数**（`connectionTimeout` / `idleTimeout` / `maximumPoolSize`），`jdbcUrl`/`username` 这类必须重启的字段在代码里显式 reject | Hikari 不是原生支持热更，需要拿到 `HikariConfigMXBean` 改 |
| **DynamicTp 线程池** | DynamicTp `1.1.9.1-3.x` 接管 Dubbo、RocketMQ 消费、Tomcat 线程池 | DynamicTp BOM 锁住了 Dubbo 版本到 3.0.7，不能擅自升级，否则 starter 与核心冲突 |
| **RocketMQ 消费线程池** | 按 `consumerGroup` 维度动态更新并发度 | 消费中变更要在拉新批之间无缝切换 |
| **运行时业务属性** | `@RefreshScope` + Nacos | 注意 `@RefreshScope` Bean 不能被 `@Resource` 早期注入，否则不会刷新 |

**踩过的坑**：
- `infrastructure` 模块的 `@RefreshScope` 不生效——因为 Spring Cloud Alibaba BOM 没 manage `spring-cloud-context`，需要手动声明 `4.1.4`。
- Dubbo 版本被 DynamicTp BOM 锁到 3.0.7，最初想升 3.2 出现 starter 跟 core 不兼容报错，回退。

**R（结果 / 设计指标）**
- 5 类配置、11 个 Nacos DataId 全动态；
- 压测中流量突增的标准应对：Nacos 改 Sentinel flow 规则 → 5 秒内全节点生效 → 限流命中、链路保持稳定。

---

### 亮点 4：可观测体系 + AI 运维 Agent 闭环

**S（背景）**
微服务多了之后，"出问题怎么定位"是最大的工程痛点。光有监控不够，光有日志也不够，必须把告警 → 排查 → 修复整条链路打通。

**T（任务）**
搭一套可观测全栈，并且在告警侧引入 AI Agent 做自动诊断（这是项目最有差异化的点）。

**A（行动）**
1. **指标层**：Micrometer + Prometheus，每个服务暴露业务指标（订单创建/支付成功/退款率）和系统指标；Grafana 面板按服务分组。
2. **日志层**：Logback → Filebeat → Elasticsearch → Kibana。
3. **链路层**：SkyWalking agent，traceId 贯穿网关到下游。
4. **告警层**：Prometheus 计算告警 → Alertmanager → **webhook 推给 ops-agent-spring-ai**。
5. **AI 闭环**：ops-agent 收到告警后——
   - `OpsRouteService` 用 **硬匹配（YAML SOP 规则）+ AI 兜底匹配** 二段匹配
   - 命中规则 → `ParentReactAgent` 开始 ReAct 循环（思考 → 选工具 → 执行 → 观察 → 再思考）
   - 通过 `AgentToolRegistry` 委派给 8 个领域子 Agent（每个子 Agent 自己也是一个 ReAct 循环，单一技术域）
   - 工具调用统一过 `MasterRegistry` → Wrench 规则链（SkillResolve / Whitelist / Approval / RunCancel / TraceStart / Execute / ResultRecord）
   - 写操作（比如改 Nacos 配置）**强制走内存审批队列**，运维同学在 Web 控制台批准后才执行

**R（结果 / 设计指标）**
- 故障演练对照基线：人工排查典型问题 ~15min，ops-agent 自动产出诊断报告 ~3min（演示环境，对照设计目标）；
- 100% 写操作可审计，零绕过路径；
- 8 个 Skill 域覆盖了运维场景的主要面，新增域只需实现 `OpsSkillRegistry` 接口 + 注册 SOP 规则。

---

## 三、可能被追问的题（30 秒回答骨架）

### Q1: outTradeNo 收口的代价是什么？营销服务真不需要支付单号？
A: 代价是退款链路多一跳（双跳设计），延迟多 ~50–100ms，但换来对账模型大幅简化。营销服务只关心"这笔订单退了没"，不关心支付单号；真要做支付侧深度排查，order_id 一查就能在 order_service 拿到 outTradeNo。

### Q2: 为什么不用 Outbox 模式而用事务消息？
A: 评估过。Outbox 需要单独的 outbox 表 + 轮询任务 + 删除策略，自己维护。RocketMQ 事务消息把这套机制内建了，回查机制完善，运维侧也有现成工具。对单体或小项目 Outbox 更轻量，但我们已经强依赖 RocketMQ，就直接用 native 事务消息。

### Q3: Hikari 哪些参数不能热改？
A: `jdbcUrl` / `username` / `password` / `driverClassName` 必须重建连接池。`maximumPoolSize` / `minimumIdle` / `connectionTimeout` / `idleTimeout` / `maxLifetime` 可以通过 `HikariConfigMXBean` 热改，但 `maximumPoolSize` 缩小不会立即关闭已建连接，会等连接归还时回收。

### Q4: Sentinel 规则更新原子性怎么保证？
A: 规则源是整份 JSON 替换，Sentinel 内部用 `volatile` 引用切换，不存在"半新半旧"的窗口。但跨节点严格一致是不可能的，Nacos 推送有先后，需要业务能容忍 1-2 秒内不同节点限流不一致。

### Q5: ReAct 比 Plan-and-Execute 好在哪？为什么不用 LangGraph？
A: 运维场景的特点是"看了第一个工具结果才知道下一步问什么"，Plan-and-Execute 提前规划全部步骤不适用。LangGraph 是 Python 生态，我整个项目是 Java，引入跨语言成本太高。Spring AI Alibaba 的 ChatClient + Tool Calling 已经能满足。

### Q6: AI Agent 的 token 成本怎么控制？
A: ① 子 Agent 只看自己领域的 prompt fragment，不传全局上下文；② 工具结果在子 Agent 里压缩成中文摘要再返回 Parent；③ Wrench 规则链里的 `ToolResultRecordRuleFilter` 把每次工具结果归档，Parent 不重复读；④ 一轮 ReAct 上限轮数硬截断，超时返回部分结论。

### Q7: 配置中心挂了怎么办？
A: Nacos 客户端有本地快照，启动时即使 Nacos 不可用也能用本地缓存启动；运行中 Nacos 不可达不会让动态配置回滚，只是无法接收新更新。压测时刻意演练过 kill nacos 容器，所有服务保持当前配置正常运行。

### Q8: 这套架构如果流量翻 10 倍呢？
A: 三处瓶颈：① 订单 DB 单库——拆分按 userId hash 分库分表，order_id 雪花算法已经带分片位；② Redisson 单点——上集群模式；③ pay-service 单实例对接支付宝——支付宝有商户号限流，需要按 marketType 拆多商户号；④ ops-agent 单实例——Web 控制台改成无状态 + Redis 共享 run state。

---

## 四、自评 / 复盘金句

- "做这个项目最大的收获是：**先把边界画清楚，再写代码。** outTradeNo 收口这件事如果一开始没定死，后面想收回来要改 N 个服务。"
- "如果重做一次，我会更早引入 ArchUnit 把边界做成架构守护测试——靠文档和 code review 总有漏的时候。"
- "AI 运维 Agent 这块我刻意没用任何 Python 库，全部用 Spring 生态实现——目的不是炫技，是想验证'Java 生态做 Agent 是否可行'，结论是可行，但 prompt 调试、上下文管理工具链确实比 Python 弱。"

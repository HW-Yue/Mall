# 架构师版讲稿（架构师 / 技术专家）

**主线**：跨服务边界 + 闭环治理 + 演进路径
**目标**：每个亮点都要能讲"我为什么这么划边界、未来怎么演进"

> 数据口径：压测过、无生产流量。讲数字时强调"设计目标 / 演进推演"，不编造生产指标。

---

## 一、1 分钟项目总览（自我介绍用）

> Nexus 是我个人主导设计的电商交易 + AI 运维一体化项目，我把它当成一次**完整的架构练习**——三种典型电商场景（普通商城、拼团、秒杀）+ 一个差异化的 AI 运维 Agent，用来验证"在 Java 微服务体系下，业务、可观测、动态化、AI 治理如何形成闭环"。
>
> 整体结构是 6 个业务微服务 + 1 个 AI 运维 Agent，DDD 六边形分层。架构上几个关键决策：
>
> 1. **支付标识 outTradeNo 在 order 和 pay 内部收口**，营销服务只感知 orderId，跨服务一致性靠 MQ 异步 + 兜底查询，不引入分布式事务框架；
> 2. **5 类配置全部接 Nacos 动态刷新**（Sentinel / Hikari / DynamicTp / RocketMQ 消费者 / 业务属性），形成"压测中改参数无需重启"的运维基线；
> 3. **可观测三位一体**：指标（Prometheus）+ 日志（ELK）+ 链路（SkyWalking），再加 Alertmanager 把告警喂给 ops-agent 做 AI 自动诊断；
> 4. **AI 运维 Agent**：SOP 硬匹配 + AI 兜底，Parent ReAct + 子 Agent 委派，写操作走审批队列。

---

## 二、核心亮点（STAR + 演进路径）

### 亮点 1：交易链路的边界设计（outTradeNo 收口 / 营销服务解耦）

**S（背景）**
项目有三类下单（普通 / 拼团 / 秒杀），共用支付和履约。最容易腐烂的边界是"营销服务持有支付单号"——一旦多份持有，对账成本陡升，任何营销 bug 都能写穿支付状态。

**T（任务）**
画一条干净、可强制执行的边界，让支付标识只有一个真相源。

**A（行动）**
1. **硬约束写入仓库根 `CLAUDE.md`**：`outTradeNo` 只允许在 `order-service` / `pay-service` 之间存在；营销服务的库表、MQ、HTTP 出入参一律不允许。
2. **退款双跳协议**：
   ```
   营销 /refund  →  order-service.refund_execute
                 →  pay-refund-{type}（事务消息）→ pay
                 →  pay-refund-{type}-result（回执）→ order
                 →  order-refund-{type}（只带 orderId）→ 营销
   ```
3. **关单同协议**：`order-close-{type}` 给 pay/order；营销侧消费独立的 `order-close-{type}-market`。
4. **支付成功按 marketType 拆三个 Topic**（`pay-success-normal` / `-group-buy` / `-seckill`），避免单一 Topic 消费端长 if-else。

**权衡过的方案**
| 方案 | 拒绝理由 |
|------|---------|
| 营销直接监听 `pay-*` Topic | outTradeNo 泄露给营销服务，违反收口 |
| 引入 Seata Saga | 所有服务改 SQL 加 undo_log，复杂度与收益不匹配 |
| 单一 `pay-success` Topic | 消费端要靠 marketType 字段分支，业务耦合 |

**R（设计指标）**
- 营销服务**零** `outTradeNo` 字段；
- 新增交易类型仅需加 1 个 marketType + 1 对 Topic，订单/支付代码零改动；
- 对账只需 order 库 vs pay 库两方比对。

**演进路径**
- **10× 流量**：order_service DB 按 userId 分库分表，订单号雪花算法已预留分片位；MQ 消费者并发度通过 Nacos 动态加大。
- **100× 流量**：① 异地多活——按 user 维度分单元，单元内闭环，跨单元只走对账；② 支付侧引入"支付路由"按商户号分流支付宝/微信/银联，pay-service 升级为路由层。
- **新支付通道**：pay-service 内部抽 `PaymentChannel` 接口，新增通道实现 + Nacos 路由配置，order-service 完全无感。

---

### 亮点 2：异步落库 + 兜底查询代替分布式事务

**S（背景）**
下单峰值 DB 写压力大，要异步化；但用户拿到 orderId 后立即就要发起支付，订单还没落库就查不到，体验崩。

**T（任务）**
在不引入分布式事务的前提下，保证"用户感知一致性"。

**A（行动）**
1. **写路径**：生成 orderId/outTradeNo → Redis 写存在标记 `order:exists:{userId}:{orderId}` → 发普通 MQ → 立即返回。
2. **读路径（`get_pay_url`）**：
   - 命中存在标记 → 6×50ms DB 重试兜底（最坏 300ms 内 MQ 消费完成）
   - 未命中标记 → 直接 `ORDER_NOT_FOUND`，杜绝恶意流量打 DB
3. **秒杀更进一步**：先发 token（Lua 扣 Redis 库存），异步建单，前端轮询拿 orderId——下单接口完全不碰 DB。
4. **幂等保证**：订单表 `uniq_order_id`、消费者侧的状态机 + Redis 短期消费记录三层防御。

**关键设计哲学**
- **强一致是手段，用户感知一致是目的**。下单到支付之间用户是阻塞等待的，给一个 300ms 兜底窗口就够了；非要做强一致就得引 Seata，付出整个仓库的复杂度。
- **失败 fail-fast**：未命中存在标记的查询直接挂掉，把恶意流量挡在 DB 外。这是"安全 vs 体验"的明确权衡——存在标记 TTL 设短了（比如 24h），晚于这个时间的合法查询会变 fail-fast，要靠定时对账兜底。

**R（设计指标）**
- 下单接口 RT < 50ms（压测）；
- 极端情况兜底 < 300ms 自愈；
- DB 写并发可控（被 MQ 削峰）。

**演进路径**
- **10× 流量**：兜底窗口可能撑不住 → MQ 消费者扩容 + Redis 存在标记升级为带订单基本信息的 hash，部分查询直接走缓存不打 DB。
- **100× 流量**：考虑 CDC + 物化视图——订单写入 binlog → Flink → Redis 持久化视图，查询路径完全脱离主 DB。

---

### 亮点 3：动态化配置矩阵（运维基线建设）

**S（背景）**
微服务多了之后，"出问题怎么快速止血"是工程价值的核心。重启大法 vs 配置热改，是工程成熟度的分水岭。

**T（任务）**
让所有"可调"参数无需重启即可生效。

**A（行动）**

| 类型 | 实现 | 关键难点 |
|------|------|---------|
| **Sentinel 规则** | 直连 Nacos JSON 规则源，**绕开 Spring 主配置树**，避免污染 `@RefreshScope` 重建 | 规则推送跨节点最终一致，业务要容忍 1-2s 不一致 |
| **Hikari 连接池** | `HikariPoolDynamicRefresher` 监听 `EnvironmentChangeEvent`，**白名单热改参数**（pool size / timeout 类） | jdbcUrl/credentials 类必须重启，要在代码里显式 reject 防误改 |
| **DynamicTp** | 接管 Dubbo / RocketMQ 消费 / Tomcat 线程池 | DynamicTp BOM 锁住 Dubbo 3.0.7，不能升级；`infrastructure` 模块需要手动声明 `spring-cloud-context` |
| **RocketMQ 消费者** | 按 consumerGroup 维度动态并发度 | 变更要在拉新批之间无缝切换 |
| **运行时属性** | `@RefreshScope` + Nacos | 早期注入 Bean 不会刷新，需要走 prototype + lookup |

**演进路径**
- **金丝雀发布配置**：当前所有节点接收同一份配置。下一步引入 Nacos 灰度发布或客户端按 tag 过滤，先在少量节点验证再全量。
- **配置变更审计 + 回滚**：当前 Nacos 自带版本但没和 ops-agent 联动。可以让 ops-agent 监听 Nacos 变更事件，把"谁在什么时候改了什么"记入审计流，并支持一键回滚。
- **配置合规校验**：变更前跑 schema 校验（比如 Hikari maxPoolSize 不能超过 DB 实例上限），防止误改。

---

### 亮点 4：闭环治理 = 可观测 + 动态化 + AI 运维 Agent

**S（背景）**
监控告警常见的痛点：仪表板很多但"看了不知道怎么办"，告警变成噪音被忽略。要把告警 → 排查 → 修复整条链路打通。

**T（任务）**
让可观测体系不止于"看"，能直接产出诊断建议甚至自动修复动作。

**A（行动）**
```
业务 ───→ 指标 (Prometheus) ───→ 告警 (Alertmanager)
       └→ 日志 (ELK)                       │
       └→ 链路 (SkyWalking)                ▼
                                  ops-agent-spring-ai
                                  ├── SOP 硬匹配 + AI 兜底匹配
                                  ├── Parent ReAct
                                  ├── 8 域子 Agent（Docker / MySQL / RocketMQ
                                  │   / Prometheus / ES / Redis / Nacos / Catalog）
                                  ├── Wrench 规则链统一工具执行
                                  └── 写操作 → 内存审批队列 → 运维 Web 控制台
                                            ↓ 审批通过
                                          调用 nacos_publish_config
                                          → 业务侧 5s 内热更新生效
```

**关键设计**
- **可观测是 Agent 的眼睛**：业务必须把 Prometheus 指标、ELK 日志、SkyWalking trace 都建好，Agent 才有东西可看；
- **动态化是 Agent 的手**：业务必须把 Sentinel / Hikari / DynamicTp 都接 Nacos，Agent 才能"建议改限流阈值"后真的有效；
- **审批是 Agent 的刹车**：写操作 100% 走审批，杜绝幻觉直接落地；
- **三者闭环**：观察 → 诊断 → 改配置 → 再观察是否生效，整套自动化但留人在环。

**R（设计指标）**
- 故障演练对照基线：人工 ~15min，Agent 自动 ~3min（演示环境对照设计目标）；
- 100% 写操作可审计；
- 8 个 Skill 域覆盖运维主要面，新增 Skill ~200 行代码。

**演进路径**
- **多 Agent 协作**：当前是单 Parent + 8 个固定子 Agent。下一步引入"协调 Agent"层处理跨域问题（比如 MQ 积压同时伴随 DB 慢查询）。
- **从被动诊断到主动巡检**：当前是告警驱动。可加定时主动巡检（每天扫一遍可疑指标），把"潜在问题"前置发现。
- **生产化**：审批队列改持久化（DB or Outbox + TTL），对接工单系统（Jira / ServiceNow），把 Agent 决策纳入正规变更管理流程。

---

### 亮点 5：DDD 六边形 + 测试隔离（架构守护原则）

**S（背景）**
微服务很容易"看起来分层但实际耦合"——Controller 直接调 Mapper、domain 依赖 infrastructure 的具体类。一旦腐烂代价极高。

**T（任务）**
建立可强制执行的分层规则，让架构腐烂代价显性化。

**A（行动）**
1. **六层包结构** 全仓统一（api / app / domain / trigger / infrastructure / types）；
2. **依赖方向**：`trigger → app → domain ← infrastructure`，domain 通过 port 接口反向依赖；
3. **测试隔离硬规则**（写入 `CLAUDE.md`）：
   - 测试代码只能在 `*-app/src/test/` 和 `application-test-mock.yml`；
   - 严禁在 domain/trigger/infrastructure 写 `if(test)`、测试桩 Controller、测试专用分支；
   - 每个服务可独立跑单测，**不依赖其他服务启动**；
4. **全链路脚本**：`dev-ops/app/group-buy-full-flow-test.sh` 验证跨服务关键路径。

**演进路径**
- **ArchUnit 守护**：把"营销服务不能引用 outTradeNo""domain 不能依赖 infrastructure 具体类"做成编译期单测，靠 CI 强制；
- **接口契约测试**：用 Pact 或类似工具，让 `*-api` 模块的 DTO 变更能在编译期发现下游不兼容。

---

## 三、可能被追问的题（30 秒回答骨架）

### Q1: 为什么不直接用单体？这个规模真的需要拆 6 个服务？
A: 业务规模上确实不需要——这是个个人作品，刻意拆是为了**练边界设计**。单体项目永远练不到"跨服务一致性""服务边界划分""配置中心"这些场景。坦白说生产里如果业务复杂度跟我这个项目相当，我会推荐单体或者只拆 2-3 个服务。

### Q2: outTradeNo 收口的最大代价？
A: 退款链路多一跳（双跳设计），延迟 +50-100ms。但换来对账模型从 N 方变两方比对，长期收益压倒短期开销。如果出现"退款延迟敏感"的场景（比如金融），可以考虑给特定 marketType 开直连白名单，但要明确记录这是边界破例。

### Q3: 没有真实流量，你怎么验证架构有效？
A: 三个维度：① **本地压测看降级行为**——刻意 kill consumer / 改限流规则，验证系统按设计降级；② **故障演练**——构造慢查询、MQ 积压、Redis 阻塞，验证 ops-agent 诊断输出是否符合预期；③ **同构推理**——选取业界已知成功模式（比如阿里 Sentinel + Nacos 体系）做参照，论证我的设计在 10× / 100× 流量下的演进路径是否合理。

### Q4: AI 运维 Agent 是炫技还是真有价值？
A: 三个层面：① **个人项目层面**，它是差异化亮点，让我能讲"Java 生态做 Agent 是否可行"这个有意思的问题；② **架构层面**，它把"可观测 + 动态化"闭环了——光有 Prometheus + Nacos 还得人去看去改，Agent 让闭环走完整；③ **生产化层面**，当前形态肯定不能直接上生产，需要补审计/合规/工单对接，但**模式本身**（SOP + ReAct + 审批）是真的有价值的——AIOps 是确定趋势。

### Q5: 如果重做一次，最大的改动会是什么？
A: 三件事：① **更早引入 ArchUnit**，把架构约束做成编译期守护，不靠 code review；② **更早搭 Agent 评估集**，prompt 迭代需要回归用例支撑；③ **业务侧加幂等性 Filter**，现在幂等性逻辑分散在多个 Service 里，应抽到一个统一的 IdempotencyAspect。

### Q6: 你说"压测过没真实流量"，那压测有意义吗？
A: 我用压测验证的不是 TPS 数字，而是三件事：① **链路通畅性**——三类下单 + 支付 + 退款全链路在并发下能跑通；② **降级行为**——Sentinel 限流 / Hikari 满 / MQ 积压时系统按设计降级而不是雪崩；③ **热更新真的生效**——改 Nacos 配置后压测曲线肉眼可见变化。这些是架构验证，不是性能验证。

### Q7: 如果 ops-agent 写错配置导致生产事故怎么办？
A: 当前设计已经把这个风险压到很低：写操作 100% 走审批，运维同学在 Web 控制台**人工审批**才会真执行。Agent 只是个"建议者 + 执行器"。真上生产还要加：① 配置变更前的 schema 校验；② 一键回滚；③ 工单系统对接，把每次 Agent 决策纳入正规变更管理。Agent 不能取代变更管理，只能加速。

### Q8: 你认为这个项目最不成熟的地方是哪？
A: 三处：① **没有真实流量验证**——压测 ≠ 生产，很多边角问题只有生产才暴露；② **Agent 评估集薄弱**——目前主要靠人肉看 case，迭代效率低；③ **测试覆盖率不均**——业务核心路径覆盖好，Agent 的 prompt 行为测试缺失。如果继续做，这三块是首要补的。

---

## 四、自评 / 复盘金句

- "做这个项目最重要的收获是：**架构师的核心工作是画边界**。outTradeNo 这个边界画对了，整个仓库的耦合度天然就降了一档；画错了，再多设计模式也救不回来。"
- "我刻意把 AI Agent 和业务系统放在同一个仓里，是想表达一个观点——**AI 时代的业务系统设计要把"Agent 友好性"作为新的非功能需求**。可观测埋点是给 Agent 的眼睛，动态配置中心是给 Agent 的手，受控写操作是给 Agent 的刹车。"
- "我对'用 Java 做 Agent'这件事的结论是：**可行，但要付出造轮子的代价**。Spring AI Alibaba 已经做了基础抽象，但 prompt 调试工具链、上下文管理、评估框架都得自己搭。如果团队完全没历史包袱，Python + LangGraph 会更快；如果团队是 Java，自己搭也完全 OK。"
- "如果只让我留一句话给候选人——**别炫技，画清楚边界，剩下的就是细节**。"

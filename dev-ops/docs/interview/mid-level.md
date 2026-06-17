# 中级版讲稿（Java 后端 3–5 年）

**主线**：交易链路 + MQ + DDD 工程化
**目标**：稳，少踩雷，每个亮点都要能讲到 MySQL/Redis/MQ 的具体实现

> 数据口径：本项目压测过、无生产流量。所有"性能数字"都标注压测环境或设计目标，不能讲成线上数据。

---

## 一、1 分钟项目总览（自我介绍用）

> 我做了一个叫 Nexus 的电商交易项目，覆盖**普通商城下单、拼团、秒杀三条完整交易链路**和支付/退款全流程，一共 6 个业务微服务 + 1 个网关，整体按 DDD 六边形分层（api / app / domain / trigger / infrastructure / types）。
>
> 技术栈是 Spring Boot 3.2 + Spring Cloud Alibaba（Nacos 注册/配置）+ RocketMQ 5 + MySQL + Redisson，订单和支付服务对接支付宝沙箱。
>
> 我重点投入的是 **三类下单链路的统一收口** 和 **MQ 消息分类设计**——支付标识 `outTradeNo` 只在 order 和 pay 内部存在，营销服务（拼团/秒杀）只感知 `orderId`，这样三种交易类型可以共用同一套订单和支付流程。另外我也用 Spring AI Alibaba 探索了一个运维 Agent，做监控告警的自动诊断。

---

## 二、核心亮点（STAR 深挖）

### 亮点 1：三类交易链路的统一收口

**S（背景）**
普通商城下单、拼团、秒杀三种业务，下单入口、库存模型、营销规则全都不一样，但**支付和履约的流程是一样的**。如果每条链路都自己接支付，会出现支付单号多份维护、退款/对账逻辑重复实现的问题。

**T（任务）**
设计一套订单和支付的统一收口方案：营销服务只关心业务规则，订单和支付逻辑只在 `order-service` 和 `pay-service` 内部维护。

**A（行动）**
1. **建立硬约束**：`orderId`（`OD{snowflake}`）和 `outTradeNo`（`OT{snowflake}`）都由 `order-service` 生成；`outTradeNo` 只允许出现在 `order-service` ↔ `pay-service` 之间，营销服务和前端**完全感知不到**。
2. **三条入口共用同一个 `order-service.create_order`**：mall 调 `create_order_normal_from_mall`，group-buy 和 seckill 调 `create_order`，参数里带 `marketType`。
3. **支付成功事件按 marketType 拆 Topic**：`pay-success-normal` / `pay-success-group-buy` / `pay-success-seckill`，避免消费端用一个大 if-else 区分业务。
4. **退款链路走"双跳"**：营销服务发起退款 → HTTP 调 `order-service.refund_execute` → `order-service` 发 `pay-refund-{type}` 给 pay → pay 回写 `pay-refund-{type}-result` → `order-service` 再发 `order-refund-{type}` 给营销服务做本地补偿。这样营销服务只监听一个事件，不用同时监听 pay 和 order 两个上游。

**R（结果 / 设计指标）**
- 三条链路共用一套订单+支付代码，营销服务**零** outTradeNo 字段；
- 新增一种交易类型（比如团购）时，只需新加一个 marketType 和一个 pay-success Topic，订单和支付代码不用动；
- 压测环境单机能稳定跑通三类下单 + 退款全链路。

---

### 亮点 2：高并发场景下"先返回订单号、再异步落库"的兜底设计

**S（背景）**
秒杀和拼团高峰瞬间会产生大量订单写入，如果下单接口要等 `t_order` 落库完才返回，DB 直接会被打死。

**T（任务）**
让下单接口在毫秒级返回 `orderId`，订单数据异步写入；同时保证用户**立即**就能用这个 `orderId` 去发起支付。

**A（行动）**
1. **下单接口同步**：生成 `orderId` + `outTradeNo` → 在 Redis 写存在标记 `order:exists:{userId}:{orderId}` → 发 RocketMQ 消息（`normal-order-create` / `group-buy-order-create` / `seckill-order-create`） → 立即返回 `orderId`。
2. **消费者异步落库**：MQ 消费者把订单写入 `t_order`，**唯一索引 + 幂等键防止重复消费**。
3. **`get_pay_url` 兜底**：用户拿 `orderId` 来发起支付时——
   - 命中 Redis 存在标记 → 表示订单确实在落库中，做 **6 × 50ms** 的 DB 重试等 MQ 消费完成，最坏 300ms 兜底。
   - 未命中标记 → 直接 `ORDER_NOT_FOUND`，快速失败，不让恶意请求拖垮 DB。
4. **秒杀更进一步**：先返回 `seckillToken`（Lua 脚本扣 Redis 可售库存得到），前端拿着 token 轮询 `query_seckill_order` 拿 `orderId`，再走支付。这样下单接口本身完全不碰 DB。

**R（结果 / 设计指标）**
- 下单接口 RT 在压测环境稳定 < 50ms；
- DB 写入压力被 MQ 削峰，写入并发可控；
- "用户拿不到 orderId" 这种坏体验在 300ms 内自愈，无需告知用户重试。

---

### 亮点 3：DDD 六边形分层的工程化落地

**S（背景）**
微服务多了以后，最怕代码长成"Controller 直接调 Mapper、业务逻辑散落各处"，长期维护很痛苦。

**T（任务）**
让每个服务的代码结构清晰、可单测、可替换外部依赖。

**A（行动）**
统一 6 层包结构：
- **`*-api`**：纯接口契约（DTO、Request/Response），其他服务可以只依赖这个 jar
- **`*-app`**：Spring Boot 启动类 + yml
- **`*-domain`**：核心业务（Service / Entity / VO），**不依赖任何 Spring 框架外的具体技术**
- **`*-trigger`**：HTTP Controller、MQ Listener、定时任务（adapter in）
- **`*-infrastructure`**：DAO、MQ Producer、HTTP/RPC Client（adapter out，**实现 domain 定义的 port 接口**）
- **`*-types`**：常量、枚举、异常

依赖方向严格：`trigger → app → domain ← infrastructure`，**domain 不依赖 infrastructure**，只通过 port 接口反向调用。

**R（结果）**
- 单测可以在 `*-app/src/test/` 里跑，**不需要起其他微服务**；
- 替换底层（比如 MQ 从 RocketMQ 换 Kafka）只动 infrastructure；
- 提交前最低要求是跑受影响服务的单测 + 全链路脚本 `dev-ops/app/group-buy-full-flow-test.sh`。

---

### 亮点 4：Hikari / DynamicTp 配置热更新（一句话亮点，被追问时展开）

> 我把 Sentinel 限流规则、Hikari 连接池参数、DynamicTp 线程池、RocketMQ 消费者并发度都接到 Nacos 上做动态刷新——压测时如果发现某条链路 RT 飙高，可以在不重启的情况下直接改连接池/线程池参数生效，5 秒左右。

---

## 三、可能被追问的题（30 秒回答骨架）

### Q1: MQ 消息重复消费怎么处理？
A: 三层防御。① 数据库唯一索引（`uniq_out_trade_no` / `uniq_order_id`）；② 业务幂等键（订单状态机，已支付的订单不能再次更新成支付中）；③ Redis 短期消费记录（`mq:consumed:{messageId}` TTL 1 小时）。RocketMQ 至少一次语义下，幂等性必须落到业务层。

### Q2: 为什么不用 Seata / Saga 做分布式事务？
A: 三类下单链路里其实**没有真正的强一致需求**——下单到支付之间用户是阻塞等待的，可以用最终一致 + 兜底查询解决；退款用的是事务消息（RocketMQ 半消息）保证本地表和发消息原子性。引 Seata 会让所有服务都得改 SQL 加 undo_log，复杂度跟收益不匹配。

### Q3: Redisson 分布式锁选 `lock` 还是 `tryLock`？
A: 库存扣减用 `tryLock(0, leaseTime, SECONDS)` + 重试，避免线程在锁上等导致 Tomcat 线程被吃光。秒杀场景不用锁，用 Redis Lua 脚本原子扣库存。

### Q4: `@Transactional` 在哪些场景会失效？
A: 自调用、private 方法、异常被吞、传播级别错误、非 public 方法、AOP 代理被绕过（this.xxx 调用）。我们项目里订单状态扭转用的是显式的事务模板 + 事务消息组合，不依赖 `@Transactional` 跨 MQ。

### Q5: MySQL 索引怎么设计的？
A: 订单表主键 `id` + `orderId` 唯一索引 + `outTradeNo` 唯一索引 + `(userId, status)` 联合索引覆盖用户订单列表查询。所有查询走索引，禁用全表扫描（开了 `slow_query_log` + 阈值 100ms）。

### Q6: Redis 缓存击穿/穿透/雪崩？
A: 商品详情用 Redisson 互斥锁 + 双重检查防击穿；不存在的商品 ID 写空对象 5min 防穿透；过期时间随机化 + 多级缓存防雪崩。秒杀活动开始前预热 Redis。

### Q7: 网关怎么做的？
A: Spring Cloud Gateway + Nacos 服务发现，按 `/gw/api/v1/{service}/**` 路由分发，StripPrefix=1 去掉网关前缀。前端请求统一从 `dev-ops/nginx/html/js/api-config.js` 配置入口，避免散落。

### Q8: 你们怎么压测的？什么工具？
A: 本地 docker compose 起完整环境，JMeter 打主要接口（下单/支付/查询），观察 Prometheus 指标和 Sentinel 监控面板。主要看链路通畅性、降级行为是否符合预期、热更新参数后是否真的生效，**不追求绝对 TPS 数字**——单机压测不能代表真实生产。

---

## 四、自评 / 复盘金句

- "如果重做一次，我会把 `outTradeNo` 收口的约束做成 **架构守护测试**（ArchUnit），编译期就阻止其他模块引用，比靠文档约束更可靠。"
- "项目里我最满意的是 **三类下单链路收敛到一套订单+支付代码**，最不满意的是 **MQ 重复消费的幂等性现在分散在多个地方**，应该抽到一个统一的 IdempotencyFilter 里。"
- "做这个项目最大的收获是：**先把边界画清楚，再写代码**。outTradeNo 这个事情如果一开始没定死边界，后面想收回来代价会非常大。"

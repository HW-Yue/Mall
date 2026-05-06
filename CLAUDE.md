> 注意：如果修改本文件内容，必须同步更新 `CLAUDE.md`；如果修改 `CLAUDE.md`，也必须同步更新本文件。

## Repository Overview

Multi-module Java enterprise microservices mono-repo，DDD 架构。

## 文档与图表输出目录

所有架构图、流程图、技术图、MQ 路由图等可视化产物，统一归档到 `dev-ops/docs/` 下，不要散落到其他目录。

约定：
- 生成的 SVG / PNG / Mermaid / Markdown 说明文档，统一放在 `dev-ops/docs/`
- 文档按主题分层归档：
  - `dev-ops/docs/api/`：接口文档
  - `dev-ops/docs/config/`：配置中心、运行时配置、动态更新文档
  - `dev-ops/docs/testing/`：测试方案与测试策略
  - `dev-ops/docs/diagrams/`：流程图、MQ 图、交易链路图
- 不要把接口清单、流程图、测试说明再散落到根目录 `docs/` 或服务子目录，正式文档都以 `dev-ops/docs/` 为入口

**Services:**
- `springcloud-gateway` — API Gateway（:8090），StripPrefix=1 去掉路径前缀 `/gw`
- `mall` — 商城（Spring Boot 3.2, Java 21），普通商品展示与下单入口、后台配置
- `order-service` — 统一订单服务
- `group-buy-service` — 拼团服务
- `seckill-service` — 秒杀服务
- `pay` — 支付服务（Spring Boot 2.7.12, Java 8），对接支付宝
- `ops-agent-spring-ai` — 运维 Agent（Spring AI Alibaba，:2322），SOP 驱动 ReAct + 7 域 skill 工具（Docker / MySQL / RocketMQ / Prometheus / Elasticsearch / Redis / Nacos）；详见 `ops-agent-spring-ai/README.md`

## DDD Module Layout

```
*-api/           # 接口契约：DTO、Request/Response，定义 Controller 实现的接口
*-app/           # Spring Boot 启动类、yml 配置
*-domain/        # 核心业务 Service、Entity、ValueObject
*-trigger/       # HTTP Controller（实现 api 接口）、MQ 消费者、定时任务
*-infrastructure/# 数据库、发 MQ、外部 HTTP/RPC 调用
*-types/         # 常量、枚举、异常
```

依赖规则：`trigger` → `app` → `domain` ← `infrastructure`，均依赖 `types` 和 `api`。

## 微服务职责

| 服务 | 职责 |
|------|------|
| `mall` | 普通商品展示、普通品下单（Redis防刷→锁库→order-service异步落单）、后台CRUD；不展示拼团/秒杀商品 |
| `group-buy-service` | 独立维护拼团商品，拼团下单，内部调 order-service |
| `seckill-service` | 独立维护秒杀商品，秒杀下单，内部调 order-service |
| `order-service` | 统一订单创建、支付URL获取、退款执行、订单查询 |
| `pay` | 对接支付宝，按 marketType 发布 `pay-success-*` 三个 Topic |
| `ops-agent-spring-ai` | 接收 Alertmanager webhook，按 SOP 规则用 ReAct 调度 7 域 skill 工具诊断；Nacos 写操作走内存审批队列 |

**商品数据冗余**：拼团/秒杀商品各自独立存储，不依赖 mall 服务，后台配置时同步写入各服务。

**前端展示名称规则**：`goodsName = spuName + " " + skuSpecJson`，在接口层拼接，DTO 暴露 `goodsName`；`goodsId` 统一用 `skuId` 替代。

## ⚠️ 接口变更三位一体原则（MANDATORY）

修改接口路径/参数/返回值必须同步修改以下三处：

1. **后端 Controller**（trigger 层）`@RequestMapping`
2. **网关路由**：`springcloud-gateway/app/src/main/resources/application-dev.yml`（同时改 prod/test）
3. **前端配置**：`dev-ops/nginx/html/js/api-config.js`（`AppApiPaths` 对象）

**网关路由表（StripPrefix=1，去掉 `/gw`）：**
| 前端路径前缀 | 转发服务 |
|------------|--------|
| `/gw/api/v1/mall/**` | `lb://mall-service` |
| `/gw/api/v1/pay/**` | `lb://pay-service` |
| `/gw/api/v1/order/**` | `lb://order-service` |
| `/gw/api/v1/group-buy/**` | `lb://group-buy-service` |
| `/gw/api/v1/seckill/**` | `lb://seckill-service` |

**变更检查清单：**
- [ ] 改 `@RequestMapping` 路径 → 同步改 `api-config.js`
- [ ] 新增接口 → `api-config.js` 加路径，确认网关路由覆盖该服务
- [ ] 新增服务 → 网关 yml 加路由，`api-config.js` 注册路径
- [ ] 改 DTO 字段名 → 检查前端 JS 字段引用（`mall.js`/`payment.js` 等）

## 接口文档

接口清单不再内嵌在本文件，统一维护在：

- 总览：`dev-ops/docs/api/README.md`
- 服务分文档：`dev-ops/docs/api/services/`
- 详细接口文档：`dev-ops/docs/api/details/`

事实来源：

- `*-trigger/src/main/java/**/http/**`
- `*-api/src/main/java/**`
- `dev-ops/nginx/html/js/api-config.js`
- `springcloud-gateway/app/src/main/resources/application-*.yml`

接口改动时，除了改代码，还必须同步更新：

- `api-config.js`
- gateway 路由
- 对应服务接口文档
- `dev-ops/docs/api/README.md` 中的总览说明
- 需要时同步更新详细接口文档中的参数表和样例

## 业务流程

> **当前实现要点：**
> - `order-service` 不是所有 MQ 都走事务消息。当前 `pay-refund-*`、`order-ship-task` 走事务消息；`order-paid-*`、`normal-order-create` 走普通消息。
> - 普通单是 mall 锁库后，由 `order-service` 投递 `normal-order-create` 异步落库。
> - 秒杀下单先返回 `seckillToken`，前端轮询拿到 `orderId` 后再走支付流程。

### 普通下单
前端 → mall `create_normal_order`（防刷→锁可售库存） → Feign order-service `create_order_normal_from_mall` → order-service 发布 `normal-order-create` → order-service 消费后异步落库 `t_order` → 前端拿 `orderId` / `outTradeNo` → order-service `get_pay_url`（必要时短暂重试等待订单落库）→ Feign pay `create_pay_order` → 支付宝

支付后：支付宝回调 pay → pay 更新支付单状态 → `pay-success-normal` → order-service 更新订单状态 → 发布 `order-paid-normal` 与事务消息 `order-ship-task`

### 拼团下单
前端 → group-buy-service `create_pay_order`（校验活动、占 `lock_count`、必要时创建 team）→ Feign order-service `create_order` → 返回 `orderId` / `teamId` / `outTradeNo` → order-service `get_pay_url` → Feign pay → 支付宝

新开团时：group-buy-service 还会发送 `group-buy-timeout-refund` 定时消息，用于队伍超时后的关单 / 退款补偿。

支付后：pay → `pay-success-group-buy` → order-service 更新订单状态 → 发布 `order-paid-group_buy` → group-buy-service 结算拼团订单并更新组队状态 → 成团后由 group-buy-service 再发 `group-buy-success-notify` 给 order-service 推进后续处理

### 秒杀下单
前端 → seckill-service `create_pay_order`（校验活动、Lua 扣 Redis 可售库存、生成 `seckillToken` / `outTradeNo`）→ seckill-service 发布 `seckill-order-create` → order-service 异步建单 → 前端轮询 order-service `query_seckill_order` 拿到 `orderId` → order-service `get_pay_url` → Feign pay → 支付宝

支付后：pay → `pay-success-seckill` → order-service 更新订单状态 → 发布 `order-paid-seckill` 与事务消息 `order-ship-task` → seckill-service 扣 Redis 真实库存并发布 `seckill-stock-deduct` 异步回写 MySQL 库存

退款：

- 普通单：前端 → order-service `refund` → order-service 发布事务消息 `pay-refund-normal`
- 拼团 / 秒杀：前端 → 各营销服务 `refund` → HTTP 调 order-service `refund_execute` → order-service 发布事务消息 `pay-refund-group-buy` / `pay-refund-seckill`
- pay 完成退款后，再回写 `pay-refund-*-result` 给 order-service；营销服务侧分别消费 `pay-refund-group-buy` / `pay-refund-seckill` 做本地状态与库存补偿

## MQ 文档

MQ topic、生产者、消费者、消费者组、参数说明统一维护在：

- 总览：`dev-ops/docs/mq/README.md`
- Topic 明细：`dev-ops/docs/mq/topics/`

事实来源：

- 各服务 `application-{profile}.yml`
- `@RocketMQMessageListener`
- `*-infrastructure` 下 MQ producer / publisher

## 关键文件路径

### SQL（`dev-ops/mysql/sql/`）
`mall_db.sql` / `order_service.sql` / `group_buy_service.sql` / `seckill_service.sql` / `grafana.sql`

> 新增表/字段必须同步更新对应 SQL 文件。
> 如果改的是业务库 SQL，还必须同步检查并更新 `dev-ops/mysql/sql/test/*.sql` 对应测试 SQL。

其余网关 / 前端 / 各服务关键入口，统一维护在：

- 总览：`dev-ops/docs/code-map/README.md`
- 网关与前端：`dev-ops/docs/code-map/gateway-and-frontend.md`
- `order-service`：`dev-ops/docs/code-map/order-service.md`
- `group-buy-service`：`dev-ops/docs/code-map/group-buy-service.md`
- `seckill-service`：`dev-ops/docs/code-map/seckill-service.md`
- `mall`：`dev-ops/docs/code-map/mall.md`
- `pay`：`dev-ops/docs/code-map/pay.md`

## 技术配置

### 版本
| 服务 | Java | Spring Boot |
|------|------|-------------|
| mall | 21 | 3.2.12 |
| order-service | 21 | 3.2.12 |
| group-buy-service | 21 | 3.2.12 |
| seckill-service | 21 | 3.2.12 |
| pay | 21 | 3.2.12 |
| springcloud-gateway | 21 | 3.2.12 |
| ops-agent-spring-ai | 21 | 3.4.5 |

### 当前主技术栈

- ORM：MyBatis Spring Boot Starter `3.0.5`
- Cache：Redisson Spring Boot Starter `3.26.0`
- MQ：RocketMQ Spring Boot Starter `2.3.1` + RocketMQ Client `5.3.0`
- 注册发现 / 配置：Spring Cloud Alibaba `2023.0.1.0`
- 服务间调用：OpenFeign `4.1.2`
- 动态线程池：DynamicTp `1.1.9.1-3.x`

补充：

- `ops-agent-spring-ai` 独立使用 Spring Boot `3.4.5`、Spring Cloud `2024.0.1`、Spring Cloud Alibaba `2023.0.3.3`
- `ops-agent-spring-ai` 的 RocketMQ Client 版本为 `5.3.1`

### Redis（Redisson）
统一用 `redisson-spring-boot-starter:3.26.0`，配置键 `spring.data.redis.*` + `spring.redis.redisson.config`（YAML 片段）。不使用 Lettuce / 自研 `redis.sdk.config`。

各服务 `client-name` 全局唯一：`mall-redisson` / `order-redisson` / `seckill-redisson` / `groupbuy-redisson` / `pay-redisson`

### Hikari 连接池命名（全局唯一）
`Mall_HikariCP` / `Order_HikariCP` / `Seckill_HikariCP` / `GroupBuy_HikariCP` / `Pay_HikariCP`

### Spring Profiles & 启动类

Profiles: `dev` / `test` / `prod`，配置在各 app 模块 `src/main/resources/application-{profile}.yml`，本地默认 `dev`。

| 服务 | 启动类 | 模块 |
|------|--------|------|
| mall | `com.yue.MallApplication` | `mall-app` |
| order-service | `com.yue.order.OrderServiceApplication` | `order-service-app` |
| group-buy-service | `com.yue.groupbuy.GroupBuyServiceApplication` | `group-buy-service-app` |
| seckill-service | `com.yue.seckill.SeckillServiceApplication` | `seckill-service-app` |
| pay | `cn.bugstack.PayApplication` | `pay-app` |
| gateway | `cn.bugstack.gateway.SpringcloudGatewayApplication` | `app` |

### Build & Run

```bash
mvn clean package -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn clean package -pl <module-name> -am
```

```bash
docker-compose -f dev-ops/docker-compose-environment.yml up -d
docker compose -f docker-apps/docker-compose-apps.yml up -d
```

**Docker 环境约定：**
- 所有基础环境都通过根目录 `dev-ops/` 下的 Docker Compose 文件构建和启动，不再从子模块目录找环境配置。
- MySQL、Redis、Nacos、RocketMQ、Sentinel、ELK、Prometheus/Grafana 等端口、容器名、网络名、挂载目录均以 `dev-ops/` 内的 compose 与配置文件为准。
- MySQL 初始化 SQL 统一放在 `dev-ops/mysql/sql/`；只有 `dev-ops/mysql/data/` 为空时官方镜像才会执行 `/docker-entrypoint-initdb.d`，测试库 SQL 由 `zz-init-test-sql.sh` 递归加载 `test/*.sql`。
- 业务应用镜像和容器入口在 `docker-apps/`；需要确认应用依赖的环境地址时，先查 `dev-ops/`，再查各服务 `application-{profile}.yml` 和 `docker-apps/docker-compose-apps.yml`。
- 涉及订单、拼团、支付、网关、Nacos、MySQL 初始化或 Docker 启停的重大变更，提交前必须先完成一次完整全链路测试，流程见 `dev-ops/full-flow-test/README.md`，脚本入口为 `bash dev-ops/app/group-buy-full-flow-test.sh`。

## 监控文档

Prometheus、Alertmanager、Sentinel、DynamicTP、ELK、SkyWalking、启动顺序统一维护在：

- 总览：`dev-ops/docs/monitoring/README.md`
- 分主题文档：`dev-ops/docs/monitoring/`

相关事实来源：

- `dev-ops/prometheus/`
- `dev-ops/docker-compose-*.yml`
- `dev-ops/nacos/README.md`
- `dev-ops/nacos/sentinel-rules/README.md`
- `dev-ops/SKYWALKING.md`
- 告警 → SOP → ReAct 流水线：`ops-agent-spring-ai/README.md`

## 配置文档

配置中心接入、运行时配置拆分、动态更新边界统一维护在：

- 总览：`dev-ops/docs/config/README.md`
- 配置更新总览：`dev-ops/docs/config/dynamic-refresh.md`
- Sentinel 规则动态更新：`dev-ops/docs/config/sentinel-rules.md`
- Hikari 连接池动态更新：`dev-ops/docs/config/hikari-refresh.md`
- DynamicTp / Tomcat 动态更新：`dev-ops/docs/config/dynamic-tp-refresh.md`
- RocketMQ 消费线程池动态更新：`dev-ops/docs/config/rocketmq-consumer-refresh.md`
- 运行时属性热更新：`dev-ops/docs/config/runtime-properties-refresh.md`

相关事实来源：

- 各服务 `application-{profile}.yml`
- 各服务 `src/main/resources/nacos/*.yml`
- 各服务 `HikariPoolDynamicRefresher`
- `@RefreshScope` 配置类与运行时配置装配类

## 服务单测约定

目标：

- 每个服务单独执行自己的测试
- 不依赖其他服务启动
- 不为了测试修改业务对外接口
- 不在生产代码里加 `if (test)`、mock 分支、测试专用 controller

测试相关改动只允许放在这些位置：

- `*-app/src/test/java`
- `*-app/src/test/resources`
- `*-app/src/main/resources/application-test-mock.yml`
- `*-app/pom.xml`

不要把测试替身、测试 profile 判断、测试桩逻辑写进 `domain`、`trigger`、`infrastructure` 的生产源码里，除非是修复明显空实现或把硬编码外部依赖改成正常 Spring 注入。

### 测试文档

改代码后的测试补齐规则、单测隔离、执行方式、提交前最低要求统一维护在：

- 总览：`dev-ops/docs/testing/README.md`
- 改代码后怎么改测试：`dev-ops/docs/testing/change-driven-test-rules.md`
- 单测隔离与测试基座：`dev-ops/docs/testing/test-isolation-and-fixtures.md`
- 单测执行方式：`dev-ops/docs/testing/test-execution.md`
- 提交前最低测试要求：`dev-ops/docs/testing/minimum-test-requirements.md`

详细策略与单测计划：

- `dev-ops/docs/testing/service-standalone-test-strategy.md`
- `dev-ops/docs/testing/service-unit-test-plan/README.md`
- 集成 / 全链路测试：`dev-ops/full-flow-test/README.md`

额外要求：

- 只改单服务内部代码：至少跑受影响服务单测
- 改跨服务链路代码：必须在单测之外补跑集成 / 全链路测试
- 全链路脚本入口：`bash dev-ops/app/group-buy-full-flow-test.sh`

> 注意：如果修改本文件内容，必须同步更新 `CLAUDE.md`；如果修改 `CLAUDE.md`，也必须同步更新本文件。

## Repository Overview

Multi-module Java enterprise microservices mono-repo，DDD 架构。

## 文档与图表输出目录

所有架构图、流程图、技术图、MQ 路由图等可视化产物，统一归档到 `dev-ops/docs/` 下，不要散落到其他目录。

约定：
- 生成的 SVG / PNG / Mermaid / Markdown 说明文档，统一放在 `dev-ops/docs/`
- 文档按主题分层归档：
  - `dev-ops/docs/api/`：接口文档
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

> **核心原则**：order-service 发 MQ 必须用 RocketMQ **事务消息**（发半消息 → 本地事务更新订单状态 → 提交半消息）。

### 普通下单
前端 → mall `create_normal_order`（防刷→锁库）→ Feign order-service `create_order_normal_from_mall`（MQ 异步落单）→ 前端拿 `orderId` → order-service `get_pay_url` → Feign pay → 支付宝

支付后：支付宝回调 pay → `pay-success-normal` → order-service 事务消息更新订单状态

### 拼团下单
前端 → group-buy-service `create_pay_order`（校验+占库存）→ Feign order-service `create_order` → 返回 `orderId` → 同普通支付流程

支付后：pay → `pay-success-group-buy` → order-service 事务消息 → `order-paid-group_buy` → group-buy-service 更新组队状态 → 成团后 → group-buy-service 调 order-service 结算

### 秒杀下单
前端 → seckill-service `create_pay_order`（校验+扣库存）→ Feign order-service `create_order` → 返回 `orderId` → 同普通支付流程

支付后：pay → `pay-success-seckill` → order-service 事务消息 → `order-paid-seckill` → seckill-service 更新订单状态

退款：前端 → 各营销服务 `refund` → HTTP 调 order-service `refund_execute`

## MQ Topic 规划

| Topic | 生产者 | 消费者 | 消息类型 |
|-------|--------|--------|--------|
| `pay-success-normal` | pay | order-service | 普通 |
| `pay-success-group-buy` | pay | order-service | 普通 |
| `pay-success-seckill` | pay | order-service | 普通 |
| `order-paid-group_buy` | order-service | group-buy-service | **事务消息** |
| `order-paid-seckill` | order-service | seckill-service | **事务消息** |
| `order-close-group-buy` | pay / group-buy-service（团级超时自发） | pay / order-service / group-buy-service（回退 `team_order.lock_count`） | 普通 |
| `pay-refund-seckill` | order-service | pay（调支付宝退款）/ seckill-service（恢复 Redis available+real & MySQL `stock_count`） | **事务消息** |

> 表中只列了核心几条，全量 topic 见各服务 `application-{profile}.yml` 的 `app.rocketmq.topic`。

## 关键文件路径

### SQL（`dev-ops/mysql/sql/`）
`mall_db.sql` / `order_service.sql` / `group_buy_service.sql` / `seckill_service.sql` / `grafana.sql`

> 新增表/字段必须同步更新对应 SQL 文件。

### 网关 & 前端
- 网关路由：`springcloud-gateway/app/src/main/resources/application-dev.yml`
- 前端接口：`dev-ops/nginx/html/js/api-config.js`（`AppApiPaths`）
- 前端逻辑：`dev-ops/nginx/html/js/mall.js`（下单）、`payment.js`（支付）、`order-list.js`（订单列表）

### order-service
- Controller：`order-service/order-service-trigger/src/main/java/com/yue/order/trigger/http/OrderController.java`
- MQ Listeners：`order-service-trigger/.../listener/PaySuccess{,GroupBuy,Seckill}Listener.java`
- MQ Producer：`order-service-infrastructure/.../event/OrderPaidMqProducer.java`

### group-buy-service
- `GroupBuyMarketController.java` / `GroupBuyTradeController.java`（trigger/http）
- `OrderServicePort.java`（infrastructure/adapter/port）；Feign 超时来自 `app.agent.feign.order-service.*`
- `OrderPaidGroupBuyListener.java`（trigger/listener）

### seckill-service
- `SeckillMarketController.java` / `SeckillTradeController.java`（trigger/http）
- `SeckillMarketServiceImpl.java` / `SeckillTradeServiceImpl.java`（domain）
- `OrderServicePort.java`（infrastructure/adapter/port）
- `OrderPaidSeckillListener.java`（trigger/listener）

### mall
- `IndexController.java` / `OrderTradeController.java`（trigger/http）
- `BackendConfigController.java`（trigger/http/admin）

### pay
- `AliPayController.java`（trigger/http）
- `PaySuccessRocketMqPort.java`（infrastructure/adapter/port）

## 技术配置

### 版本
| 服务 | Java | Spring Boot |
|------|------|-------------|
| mall | 8 | 2.7.12 |
| pay | 8 | 2.7.12 |

ORM: MyBatis，Cache: Redisson，MQ: RocketMQ

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

## 监控（Prometheus / Sentinel / DynamicTP / ELK）

> 告警 → SOP → ReAct → 7 域 skill 诊断流水线见 `ops-agent-spring-ai/README.md`

**指标暴露**：各服务开启 `management.endpoints.web.exposure.include: health,prometheus`
- Sentinel：`SentinelMetricsBinder`（`common-log-starter`）注册 Gauge，**首次请求后才有数据**
- DynamicTP：各服务 `dtp-dev.yml` 配置 `collector-types: [micrometer, internal_logging]`
- JVM/HTTP：Spring Boot Actuator 自动暴露

**关键文件：**
| 用途 | 文件 |
|------|------|
| Prometheus 主配置 | `dev-ops/prometheus/prometheus.yml` |
| 告警规则（36条） | `dev-ops/prometheus/alert_rules.yml` |
| Alertmanager 配置 | `dev-ops/prometheus/alertmanager.yml` |
| Grafana compose | `dev-ops/docker-compose-grafana.yml` |
| exporter compose（mysqld/redis/rocketmq） | `dev-ops/docker-compose-exporters.yml` |
| Sentinel 规则 | `dev-ops/nacos/sentinel-rules/`（按 `flow/`、`degrade/`、`param-flow/`、`system/`、`authority/`、`gateway/` 分目录） |
| DTP 配置 | `dev-ops/nacos/dtp-config/`（按 `dynamic-tp/`、`datasource/`、`runtime/`、`shared/` 分目录） |
| Sentinel 指标绑定 | `Dependencies/common-log-starter/.../sentinel/SentinelMetricsBinder.java` |

**Sentinel 规则 Nacos 格式：**
- dataId: `{app}-flow-rules.json`，groupId: `SENTINEL_GROUP`
- `resource` 为纯 URI，不带 HTTP Method 前缀（如 `/api/v1/order/create_order`）
- `grade`: `0`=并发线程数，`1`=QPS；`controlBehavior`: `0`=快速失败，`1`=Warm Up，`2`=匀速排队

**告警标签规范：**
- Sentinel 告警用 `{app="xxx"}`，DTP/JVM/Hikari/HTTP 用 `{application="xxx"}`

**启动顺序：**
1. `docker-compose-environment.yml`（建 `nexus-devops` 网络，启 MySQL / Redis / Nacos 等）
2. `docker-compose-rocketmq.yml`
3. `docker-compose-elk.yml`（ES 9.2.0 + Logstash :4560 + Kibana :5601，无鉴权）
4. `docker-compose-mcp.yml`（ES MCP :8085 / Prometheus MCP :8001，均走 SSE）
5. `docker-compose-exporters.yml`（mysqld-exporter :9104 / redis-exporter :9121 / rocketmq-exporter :5557）
6. `docker-compose-grafana.yml`（Prometheus + Alertmanager + Grafana）

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

### 改代码后怎么改测试

改 `domain service / state machine / rule chain`：

- 同步修改或新增 `*DomainServiceTest`、`*StateMachineTest`、`*Rule*Test`、`*Calculator*Test`
- 不启 Spring，只用 Mockito / stub / fake object
- 覆盖成功路径、失败补偿路径、幂等、重复消息、非法状态迁移

改 `controller`：

- 同步修改或新增 `*ControllerTest`
- 优先纯 Mockito controller 测试
- 断言参数校验、返回码映射、service 调用、异常映射
- 如果 controller 组装了跨服务请求 DTO，必须断言 DTO 字段

改 `listener / job`：

- 同步修改或新增 `*ListenerTest`、`*JobTest`
- listener 直接调用 `onMessage(...)`
- job 直接调用 job 方法
- 覆盖正常消息、缺字段 / 非法消息、重复消息 / 幂等、下游异常

改 `MQ producer / transaction listener`：

- 同步修改或新增 `*InfrastructureTest`、`*MqProducerTest`、`*TransactionListenerTest`
- 不连真实 RocketMQ
- 只 mock `RocketMQTemplate`
- 断言 `topic`、`payload`、`headers`
- 事务消息断言本地事务返回值：`COMMIT / ROLLBACK / UNKNOWN`

改 `Feign port / 外部 adapter`：

- 同步修改或新增 `*PortTest`、`*InfrastructureTest`
- 不走真实 Nacos / 服务发现 / HTTP
- mock 下游 client
- 断言请求 DTO 组装、空响应、失败码映射、异常映射、失败补偿

改 `repository / DAO / MyBatis mapper`：

- 同步修改或新增 `*RepositoryTest`、`*InfrastructureTest`
- 允许连本服务测试库
- 不跨服务读写别的服务库
- 至少覆盖新增 SQL 的读写主路径和关键状态分支

改 `profile / 配置装配 / app 启动相关`：

- 必须同步检查 `application-test-mock.yml`
- 必须同步检查 `maven-surefire-plugin`
- 必须同步检查 `mockito-extensions/org.mockito.plugins.MockMaker`
- 必须同步检查本服务测试基类和 test config

不要让测试去连接真实：

- Nacos
- RocketMQ broker
- Sentinel dashboard
- Logstash
- 其他微服务

### 单测隔离设计

每个服务都按这套结构维护：

- `src/main/resources/application-test-mock.yml`
- `src/test/java/.../config/RocketMqMockTestConfig.java`
- `src/test/java/.../config/Abstract*ComponentTest.java`
- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`

规则：

- `test-mock` profile 只给测试使用
- Feign 一律在测试类里 `@MockBean` 或 Mockito mock
- MQ 一律 mock，不连真实 broker
- repository 组件测试只连本服务测试库

现有测试基座：

- `pay/pay-app/src/test/java/cn/bugstack/test/config/AbstractPayComponentTest.java`
- `order-service/order-service-app/src/test/java/com/yue/order/test/config/AbstractOrderServiceComponentTest.java`
- `group-buy-service/group-buy-service-app/src/test/java/com/yue/groupbuy/test/config/AbstractGroupBuyComponentTest.java`
- `seckill-service/seckill-service-app/src/test/java/com/yue/seckill/test/config/AbstractSeckillComponentTest.java`
- `mall/mall-app/src/test/java/com/yue/test/config/AbstractMallComponentTest.java`

MQ mock 配置：

- `pay/pay-app/src/test/java/cn/bugstack/test/config/RocketMqMockTestConfig.java`
- `order-service/order-service-app/src/test/java/com/yue/order/test/config/RocketMqMockTestConfig.java`
- `group-buy-service/group-buy-service-app/src/test/java/com/yue/groupbuy/test/config/RocketMqMockTestConfig.java`
- `seckill-service/seckill-service-app/src/test/java/com/yue/seckill/test/config/RocketMqMockTestConfig.java`
- `mall/mall-app/src/test/java/com/yue/test/config/RocketMqMockTestConfig.java`

### 每个服务执行 mvn 单测的方式

统一从各自 `app` 模块执行，`surefire` 已默认注入 `spring.profiles.active=test-mock`：

```bash
mvn -pl pay/pay-app -am test -DskipTests=false
mvn -pl order-service/order-service-app -am test -DskipTests=false
mvn -pl group-buy-service/group-buy-service-app -am test -DskipTests=false
mvn -pl seckill-service/seckill-service-app -am test -DskipTests=false
mvn -pl mall/mall-app -am test -DskipTests=false
```

只跑单个测试类：

```bash
mvn -pl order-service/order-service-app -am test -DskipTests=false -Dtest=OrderDomainServiceTest
mvn -pl seckill-service/seckill-service-app -am test -DskipTests=false -Dtest=SeckillTradeServiceImplTest
```

只跑某个测试方法：

```bash
mvn -pl pay/pay-app -am test -DskipTests=false -Dtest=AliPayControllerTest#payNotifyHandlesClosedOrderByRefunding
```

### 提交前最低要求

如果改动只在单一服务内，至少跑该服务 `app` 模块测试。

如果改动涉及交易主链路，至少跑受影响的服务：

- 改 `mall -> order-service`：跑 `mall-app`、`order-service-app`
- 改 `group-buy-service -> order-service -> pay`：跑 `group-buy-service-app`、`order-service-app`、`pay-app`
- 改 `seckill-service -> order-service -> pay`：跑 `seckill-service-app`、`order-service-app`、`pay-app`
- 改公共 MQ topic 路由或退款/关单链路：跑 `pay-app`、`order-service-app` 和对应营销服务

详细说明见 `dev-ops/docs/testing/service-standalone-test-strategy.md`

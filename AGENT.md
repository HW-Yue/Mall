# CLAUDE.md

> 注意：如果修改本文件内容，必须同步更新 `CLAUDE.md`；如果修改 `CLAUDE.md`，也必须同步更新本文件。

## Repository Overview

Multi-module Java enterprise microservices mono-repo，DDD 架构。

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
| `/gw/api/v1/mall/**` | `lb://mall` |
| `/gw/api/v1/login-pay/**` | `lb://login-pay` |
| `/gw/api/v1/order/**` | `lb://order-service` |
| `/gw/api/v1/group-buy/**` | `lb://group-buy-service` |
| `/gw/api/v1/seckill/**` | `lb://seckill-service` |

**变更检查清单：**
- [ ] 改 `@RequestMapping` 路径 → 同步改 `api-config.js`
- [ ] 新增接口 → `api-config.js` 加路径，确认网关路由覆盖该服务
- [ ] 新增服务 → 网关 yml 加路由，`api-config.js` 注册路径
- [ ] 改 DTO 字段名 → 检查前端 JS 字段引用（`mall.js`/`payment.js` 等）

## 接口清单

### mall
| 接口路径 | 方法 | 说明 |
|----------|------|------|
| `/api/v1/mall/index/query_category_type_list` | GET | 分类列表 |
| `/api/v1/mall/index/query_goods_page` | POST | 商品分页 |
| `/api/v1/mall/index/query_sku_detail` | POST | SKU 详情 |
| `/api/v1/mall/index/query_activity_goods` | GET | 活动商品入口 |
| `/api/v1/sku/lock_stock` | POST | 锁库存（order-service Feign 调用） |
| `/api/v1/sku/unlock_stock` | POST | 解锁库存 |
| `/api/v1/mall/trade/create_normal_order` | POST | 普通下单入口（防刷→锁库→异步落单） |
| `/api/v1/gbm/config/activity_type*` | CRUD | 活动类型后台管理 |
| `/api/v1/gbm/config/category*` | CRUD | 分类后台管理 |
| `/api/v1/gbm/config/sku*` | CRUD | SKU 后台管理 |
| `/api/v1/gbm/dcc/update_config` | GET | 动态配置更新 |

### order-service
| 接口路径 | 方法 | 说明 |
|----------|------|------|
| `/api/v1/order/create_order` | POST | 创建订单（普通/拼团/秒杀通用） |
| `/api/v1/order/create_order_normal_from_mall` | POST | 普通品已锁库后落单（服务间调用，可选 `X-Internal-Token`） |
| `/api/v1/order/get_pay_url` | POST | 获取支付 URL |
| `/api/v1/order/query_seckill_order` | POST | 秒杀建单结果轮询 |
| `/api/v1/order/refund` | POST | 普通订单退款 |
| `/api/v1/order/refund_execute` | POST | 营销订单退款执行（拼团/秒杀调用） |
| `/api/v1/order/query_user_order_list` | POST | 用户订单列表（游标分页，`lastId`） |

### group-buy-service
| 接口路径 | 方法 | 说明 |
|----------|------|------|
| `/api/v1/group-buy/market/query_goods_list` | GET | 拼团商品列表 |
| `/api/v1/group-buy/market/query_group_buy_market_config` | POST | 拼团市场配置（含进行中的团） |
| `/api/v1/group-buy/market/trial` | POST | 拼团试算 |
| `/api/v1/group-buy/market/query_orders` | POST | 进行中参团记录 |
| `/api/v1/group-buy/market/team_statistics` | POST | 活动维度拼团统计 |
| `/api/v1/group-buy/trade/create_pay_order` | POST | 拼团下单 |
| `/api/v1/group-buy/trade/refund` | POST | 拼团退款 |

### seckill-service
| 接口路径 | 方法 | 说明 |
|----------|------|------|
| `/api/v1/seckill/market/query_goods_list` | GET | 秒杀商品列表 |
| `/api/v1/seckill/trade/create_pay_order` | POST | 秒杀下单 |
| `/api/v1/seckill/trade/refund` | POST | 秒杀退款 |

### pay
| 接口路径 | 方法 | 说明 |
|----------|------|------|
| `/api/v1/alipay/create_pay_order` | POST | 创建支付订单 |
| `/api/v1/alipay/alipay_notify_url` | POST | 支付宝回调 |
| `/api/v1/alipay/active_pay_notify` | POST | 主动查询支付状态（测试） |
| `/api/v1/login-pay/login/check_login` | GET | 登录状态 |
| `/api/v1/login-pay/login/register` | POST | 注册 |
| `/api/v1/login-pay/login/weixin_qrcode_ticket` | GET | 微信登录二维码 |

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

RabbitMQ 用于：mall 内部退款通知（`RefundSuccessTopicListener`，`group_buy_market_exchange`）

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

ORM: MyBatis，Cache: Redisson，MQ: RocketMQ（主）+ RabbitMQ（mall 内部退款）

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
| gateway | `cn.bugstack.xfg.dev.tech.SpringcloudGatewayApplication` | `app` |

### Build & Run

```bash
mvn clean package -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn clean package -pl <module-name> -am
```

```bash
docker-compose -f docs/dev-ops/docker-compose-environment.yml up -d
docker compose -f docker-apps/docker-compose-apps.yml up -d
```

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
| Sentinel 规则 | `dev-ops/nacos/sentinel-rules/*-flow-rules.json` |
| DTP 配置 | `dev-ops/nacos/dtp-config/*-dtp-dev.yml` |
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

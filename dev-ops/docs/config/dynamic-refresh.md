# 配置更新总览

本文档从服务维度说明本仓库当前支持哪些配置更新。各类配置的实现原理，拆分见本目录下的专题文档。

## 总体设计

当前实现基于 Spring Cloud Alibaba Nacos Config：

- 各服务在 `application-dev.yml` / `application-test.yml` 中通过 `spring.config.import` 引入 Nacos DataId。
- 需要动态刷新的配置使用 `optional:nacos:...&refreshEnabled=true`。
- Hikari 连接池不是天然全量热更，仓库里通过 `HikariPoolDynamicRefresher` 监听 `EnvironmentChangeEvent`，把部分新值应用到运行中的连接池。
- `group-buy-service` 额外提供 `@RefreshScope` 的 `AgentRuntimeProperties`，承接 `app.agent.*` 运行时开关。
- Sentinel 规则不走 Spring Boot 主配置树，而是各服务通过 `spring.cloud.sentinel.datasource.*.nacos` 直连 Nacos JSON 规则源。

## 服务支持矩阵

| 服务 | Sentinel 规则动态更新 | DynamicTp / Tomcat | Hikari 连接池 | RocketMQ 消费线程池 | 运行时属性热更新 |
|---|---|---|---|---|---|
| `springcloud-gateway` | 支持网关 `gw-flow`；规则源为 Nacos `SENTINEL_GROUP` | 不涉及 | 不涉及 | 不涉及 | 不涉及 |
| `mall` | 支持 `flow` / `degrade` / `param-flow` / `system` / `authority` | 支持 | 支持部分字段 | 当前无 MQ 消费者 | 当前未单独拆 runtime 热更新 |
| `order-service` | 支持 `flow` / `degrade` / `param-flow` / `system` / `authority` | 支持 | 支持部分字段 | 支持，按 consumerGroup 动态更新 | 当前未单独拆 runtime 热更新 |
| `group-buy-service` | 支持 `flow` / `degrade` / `param-flow` / `system` / `authority` | 支持 | 支持部分字段 | 支持，按 consumerGroup 动态更新 | 支持 `logging.level.*`、`app.agent.*`，Feign 超时需重启实例 |
| `seckill-service` | 支持 `flow` / `degrade` / `param-flow` / `system` / `authority` | 支持 | 支持部分字段 | 支持，按 consumerGroup 动态更新 | 当前未单独拆 runtime 热更新 |
| `pay` | 支持 `flow` / `degrade` / `param-flow` / `system` / `authority` | 支持 | 支持部分字段 | 支持，按 consumerGroup 动态更新 | 有 `pay-service-runtime-dev.yml` 入口，当前以非线程池运行时配置承载为主 |

说明：

- 上表中的“支持部分字段”是指运行中只会应用代码里显式处理过的 Hikari 参数，不代表所有 `spring.datasource.hikari.*` 都能立即生效。
- Sentinel 规则属于动态配置的一部分，但规则组织与发布口径仍以 `dev-ops/docs/monitoring/sentinel.md` 和 `dev-ops/nacos/sentinel-rules/README.md` 为准。

## DataId 规划

当前项目主要按“线程池 / 数据源 / 业务运行时 / Sentinel 规则”拆分：

1. `*-dtp-dev.yml`
   用于 `spring.dynamic.tp.*` 和 `tomcat-tp`。
2. `*-datasource-dev.yml`
   用于 `spring.datasource.hikari.*`。
3. `*-runtime-dev.yml`
   用于日志级别、业务开关、部分运行时参数。
4. `*-mq-dev.yml`
   用于 RocketMQ 消费线程池配置。
5. `*-flow-rules.json`、`*-degrade-rules.json`、`*-param-flow-rules.json`、`*-system-rules.json`、`*-authority-rules.json`
   用于业务服务 Sentinel 规则。
6. `springcloud-gateway-gw-flow-rules.json`、`springcloud-gateway-gw-api-rules.json`
   用于网关 Sentinel Gateway 规则。

这样拆分的目的：

- 避免把线程池、数据库、功能开关混在一份配置里。
- 降低误操作范围，便于按故障类型定向调整。
- 让“能热更”和“不能完全热更”的配置边界更清晰。

## 各类配置入口

- Sentinel 规则动态更新：[sentinel-rules.md](./sentinel-rules.md)
- Hikari 连接池动态更新：[hikari-refresh.md](./hikari-refresh.md)
- DynamicTp / Tomcat 动态更新：[dynamic-tp-refresh.md](./dynamic-tp-refresh.md)
- RocketMQ 消费线程池动态更新：[rocketmq-consumer-refresh.md](./rocketmq-consumer-refresh.md)
- 运行时属性热更新：[runtime-properties-refresh.md](./runtime-properties-refresh.md)

## 当前热更新边界摘要

### 1. DynamicTp / Tomcat

`mall`、`order-service`、`group-buy-service`、`seckill-service`、`pay` 都引入了 `classpath:nacos/*-dtp-dev.yml`，并叠加同名 Nacos DataId：

```yaml
spring:
  config:
    import:
      - classpath:nacos/xxx-service-dtp-dev.yml
      - optional:nacos:xxx-service-dtp-dev.yml?group=DEFAULT_GROUP&refreshEnabled=true
```

这类配置用于运行时调整线程池和 Tomcat 工作线程，适合压测、削峰、流量放大场景。

### 2. Hikari 数据源

`mall`、`order-service`、`group-buy-service`、`seckill-service`、`pay` 都有 `HikariPoolDynamicRefresher`，监听 `EnvironmentChangeEvent` 后把部分参数应用到运行中的 `HikariDataSource`。

当前代码里实际支持的热更新字段：

| 服务 | 支持字段 |
|---|---|
| `mall` | `maximum-pool-size`、`minimum-idle` |
| `order-service` | `maximum-pool-size`、`minimum-idle` |
| `group-buy-service` | `maximum-pool-size`、`minimum-idle`、`connection-timeout` |
| `seckill-service` | `maximum-pool-size`、`minimum-idle` |
| `pay` | `maximum-pool-size`、`minimum-idle`、`connection-timeout` |

结论：

- 并不是所有 `spring.datasource.hikari.*` 字段都会自动作用到已运行连接池。
- 如果改的是未被 refresher 显式处理的字段，通常需要重启或滚动发布实例。

### 3. Sentinel 规则

`mall`、`order-service`、`group-buy-service`、`seckill-service`、`pay` 都声明了 5 类 Sentinel Nacos 数据源：

- `flow`
- `degrade`
- `param-flow`
- `system`
- `authority`

`springcloud-gateway` 单独声明了 Gateway 规则源：

- `gw-flow`
- 可选的 `gw-api`

这一类配置的特点是：

- 规则内容在 Nacos JSON 中维护，不依赖 Spring `EnvironmentChangeEvent`
- 服务启动后会持续监听对应 DataId
- 修改后通常不需要重启应用实例

### 4. RocketMQ 消费线程池

`order-service`、`group-buy-service`、`seckill-service`、`pay` 现在都拆出了 `*-mq-dev.yml`：

- `order-service-mq-dev.yml`
- `group-buy-service-mq-dev.yml`
- `seckill-service-mq-dev.yml`
- `pay-service-mq-dev.yml`

这一类配置的特点是：

- 按 `consumerGroup` 维度配置 `consumeThreadNumber` 和 `consumeThreadMax`
- Nacos 刷新后，项目内的 refresher 会找到对应 `DefaultRocketMQListenerContainer`
- 再把新值应用到运行中的 `DefaultMQPushConsumer` 和消费线程池

### 5. 业务运行时配置

`group-buy-service` 额外支持 `group-buy-service-runtime-dev.yml`：

- `logging.level.*`
- `app.agent.cache.*`
- `app.agent.features.*`
- `app.agent.feign.order-service.*`

其中：

- `app.agent.*` 绑定到 `@RefreshScope` 的 `AgentRuntimeProperties`，会随 Nacos 刷新。
- `logging.level.*` 由 Spring Cloud Refresh 重绑，可在线调整日志级别。
- `app.agent.feign.order-service.*` 虽然来自运行时配置，但最终会装配成 Feign `Request.Options` Bean；该 Bean 创建后不会因单次配置刷新自动重建，所以改完超时参数后应重启或滚动发布实例验证。

`pay-service` 也保留了 `pay-service-runtime-dev.yml` 作为非线程池运行时配置入口；线程池部分已经迁移到 `pay-service-dtp-dev.yml`。

## group-buy-service 特殊说明

`group-buy-service` 是当前动态配置能力最完整的服务，配置拆分如下：

| DataId | 用途 |
|---|---|
| `group-buy-service-dtp-dev.yml` | DynamicTp / Tomcat |
| `pay-service-dtp-dev.yml` | DynamicTp / Tomcat |
| `group-buy-service-datasource-dev.yml` | Hikari 连接池 |
| `group-buy-service-runtime-dev.yml` | 日志级别、`app.agent.*`、Feign 超时 |

它同时还有一个实现边界需要注意：

- `OrderServiceFeignAgentConfig` 明确不能使用 `@RefreshScope`，因为 Feign 客户端配置运行在 `NamedContextFactory` 子上下文中，直接加 `refresh` 作用域会启动失败。

所以对拼团服务来说：

- 调线程池：改 `group-buy-service-dtp-dev.yml`
- 调数据库池：改 `group-buy-service-datasource-dev.yml`
- 调日志和 Agent 开关：改 `group-buy-service-runtime-dev.yml`
- 调 Feign 超时：改完配置后重启或滚动发布实例

## 事实来源

- `springcloud-gateway/app/src/main/resources/application-dev.yml`
- `springcloud-gateway/app/src/main/java/cn/bugstack/gateway/config/SentinelGatewayDataSourceConfig.java`
- `mall/mall-app/src/main/resources/application-dev.yml`
- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `group-buy-service/group-buy-service-app/src/main/resources/application-dev.yml`
- `seckill-service/seckill-service-app/src/main/resources/application-dev.yml`
- `pay/pay-app/src/main/resources/application-dev.yml`
- 各服务 `src/main/resources/nacos/*.yml`
- `dev-ops/nacos/sentinel-rules/README.md`
- 各服务 `HikariPoolDynamicRefresher`
- `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/config/AgentRuntimeProperties.java`
- `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/gateway/config/OrderServiceFeignAgentConfig.java`

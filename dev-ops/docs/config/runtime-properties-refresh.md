# 运行时属性热更新

本文说明业务运行时属性这一类配置更新的实现原理。它通常用于日志级别、业务开关、缓存策略、少量非核心参数，不等同于线程池或 Sentinel 规则。

## 当前支持范围

当前仓库里已明确落地的重点案例是：

- `group-buy-service`

对应 DataId：

- `group-buy-service-runtime-dev.yml`

当前承载内容：

- `logging.level.*`
- `app.agent.cache.*`
- `app.agent.features.*`
- `app.agent.feign.order-service.*`

`pay-service` 也拆出了 `pay-service-runtime-dev.yml`，但线程池部分已经迁移到 `pay-service-dtp-dev.yml`；本文只记录非线程池运行时配置边界。

## 实现原理

### 1. `@RefreshScope` 属性类

`group-buy-service` 提供了：

- `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/config/AgentRuntimeProperties.java`

这个类具备：

- `@ConfigurationProperties(prefix = "app.agent")`
- `@RefreshScope`

当 Nacos 配置刷新后：

- Spring 重新绑定 `app.agent.*`
- 重新创建或代理刷新对应 Bean
- 后续读取该 Bean 的组件会拿到新值

### 2. 日志级别

`logging.level.*` 属于 Spring Cloud Refresh 可处理的运行时配置，适合排障时快速升降日志级别。

### 3. Feign 超时的边界

`group-buy-service` 的 `app.agent.feign.order-service.*` 最终会装配到：

- `OrderServiceFeignAgentConfig`

这里生成的是 Feign `Request.Options` Bean。

当前代码注释已经明确：

- 不能在这个 Feign 配置类上直接使用 `@RefreshScope`
- 因为它运行在 `NamedContextFactory` 子上下文中
- 该子上下文没有注册 `refresh` scope，直接使用会启动失败

所以这类参数虽然存放在 runtime 配置里，但不等于“改完马上对现有 Feign 客户端实例生效”。实践口径仍然是：

- 修改后重启或滚动发布实例验证

## 生效边界

- `@RefreshScope` 适合业务属性、功能开关、缓存策略一类轻量运行时参数。
- 不适合直接套在所有基础设施 Bean 上，特别是 Feign 子上下文、部分框架托管 Bean。
- 如果参数最终只在 Bean 初始化时被读取一次，即使配置源变了，也可能需要重建 Bean 或重启实例。

## 事实来源

- `group-buy-service/group-buy-service-app/src/main/resources/nacos/group-buy-service-runtime-dev.yml`
- `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/config/AgentRuntimeProperties.java`
- `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/gateway/config/OrderServiceFeignAgentConfig.java`
- `pay/pay-app/src/main/resources/nacos/pay-service-runtime-dev.yml`

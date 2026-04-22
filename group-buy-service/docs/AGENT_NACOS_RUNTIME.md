# group-buy-service：Agent 与 Nacos 动态运行时配置

本文说明在已接入 **Tomcat**、**DynamicTp（线程池）** 与 **Nacos** 的前提下，如何把 **数据源连接池（Hikari）**、**日志级别**、**缓存策略参数**、**业务功能开关**、**OpenFeign 超时** 交给配置中心热更新，供运维 Agent 在过载或排障场景下调参。

## 1. 配置加载方式

**DynamicTp 与 Agent 运行时（Hikari / 日志 / `app.agent` / Feign）统一放在** `group-buy-service-runtime-dev.yml`（文件名不再使用 dtp 前缀，避免与「仅线程池」混淆）：

| 来源 | 说明 |
|------|------|
| `classpath:nacos/group-buy-service-runtime-dev.yml` | 本地默认 |
| `optional:nacos:group-buy-service-runtime-dev.yml?group=DEFAULT_GROUP&refreshEnabled=true` | Nacos 同名 DataId 覆盖并监听变更 |

入口：`group-buy-service-app/src/main/resources/application-dev.yml` 中的 `spring.config.import`。

生产环境请在 Nacos 维护 **DataId**：`group-buy-service-runtime-dev.yml`，**Group**：`DEFAULT_GROUP`。

若 Nacos 中仍使用旧名 **`group-buy-service-dtp-dev.yml`** 或 **`group-buy-service-agent-runtime-dev.yml`**，请迁移为上述 DataId 并删除旧配置，避免重复键覆盖顺序不确定。

## 2. 数据源连接池（HikariCP）

本服务使用 Spring Boot 默认的 **HikariCP**（未引入 Druid 时）。可调参数写在 **`spring.datasource.hikari.*`**，与 `order-service` / `mall` 中已有实现一致。

- **实现类**：`group-buy-service-app/.../HikariPoolDynamicRefresher.java`
- **行为**：收到 Spring Cloud 的 `EnvironmentChangeEvent` 且变更键以 `spring.datasource.hikari.` 开头时，对当前 `DataSource`（需为 `HikariDataSource`）调用 `setMaximumPoolSize` / `setMinimumIdle` / `setConnectionTimeout`。

**典型 Agent 场景（数据库压力过大）**：在 Nacos 中下调 `maximum-pool-size`、或下调 `connection-timeout`，减少连接占用与等待时间，起到变相「降级」。

示例键（完整见 classpath 内 `group-buy-service-runtime-dev.yml`）：

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 10
      connection-timeout: 30000
```

**若使用 Druid**：需单独写监听器，对 `DruidDataSource` 调用 `setMaxActive` / `setMaxWait` 等，键名与 Hikari 不同，不可混用。

## 3. 日志级别（INFO ↔ DEBUG）

在 **`group-buy-service-runtime-dev.yml`**（内含 DynamicTp 与 `spring.dynamic.tp`）中维护：

```yaml
logging:
  level:
    com.yue.groupbuy: info
```

Nacos 推送后，Spring Cloud 会刷新环境并 **重绑日志级别**（无需额外 Java 代码）。

**排障场景**：将业务包改为 `debug`，例如：

```yaml
logging:
  level:
    com.yue.groupbuy: debug
```

若你方规范使用 `com.mall.service` 包名，增加一行即可：

```yaml
logging:
  level:
    com.mall.service: debug
```

排障结束后改回 `info`，避免日志量与磁盘压力。

## 4. 缓存过期与降级策略（Redis / Caffeine）

业务代码通过注入 **`AgentRuntimeProperties`**（`@RefreshScope`）读取：

| 属性（YAML kebab-case） | 含义 |
|-------------------------|------|
| `app.agent.cache.redis-default-ttl-seconds` | Redis 写入 TTL 参考（秒） |
| `app.agent.cache.caffeine-expire-after-write-seconds` | 本地 Caffeine 过期参考（秒） |
| `app.agent.cache.cache-fallback-strategy` | `CACHE_FIRST` / `DIRECT_DB` / `BACKUP_CACHE` |

本仓库 **group-buy-service** 当前未强依赖 Redis/Caffeine 缓存门面；接入 Redisson 或 `@Cacheable` 时，在 Repository 或领域服务中根据 `cacheFallbackStrategy` 分支即可（例如击穿时切 `DIRECT_DB`，雪崩前缩短 TTL）。

## 5. 业务功能开关（Feature Toggles）

| 属性 | 默认 | 说明 |
|------|------|------|
| `app.agent.features.marketing-push-enabled` | `true` | 非核心营销推送等可关闭 |
| `app.agent.features.statistics-report-enabled` | `true` | 统计报表等非核心可关闭 |

在 Controller / 应用服务中注入 `AgentRuntimeProperties`，对非核心路径做短路判断，保证核心下单链路资源。

**本服务示例**：`GroupBuyMarketController#queryGroupBuyMarketConfig` 在 `app.agent.features.statistics-report-enabled=false` 时跳过 `queryTeamStatisticByGoodsId`，统计字段返回 0，减轻数据库压力。

## 6. HTTP 客户端超时（OpenFeign → order-service）

- **配置类**：`group-buy-service-infrastructure/.../OrderServiceFeignAgentConfig.java`
- **属性**：`app.agent.feign.order-service.connect-timeout-ms`、`read-timeout-ms`（与 `AgentRuntimeProperties` 中 `app.agent.*` 一致，仍由 Nacos / 本地 yml 维护）
- **接口**：`IOrderService` 已指定 `configuration = OrderServiceFeignAgentConfig.class`，在启动阶段将上述属性绑定为 Feign `Request.Options`。

**为何不给 `Request.Options` 加 `@RefreshScope`**：Feign 的 `configuration` 类运行在 `NamedContextFactory` 的子上下文中，该上下文**未注册** `refresh` 作用域，若对 `Request.Options` 使用 `@RefreshScope` 会导致启动失败（`No Scope registered for scope name 'refresh'`）。因此 **`AgentRuntimeProperties` 仍可随 Nacos 刷新**，但 **`Request.Options` 在进程生命周期内以启动时读到的值为准**；若在 Nacos 中调整 Feign 超时，需 **重启应用**（或重新部署）后才会作用于 Feign 客户端。

**下游抖动场景**：在配置中 **缩短 `read-timeout-ms`**，避免慢响应占满调用方线程（发布后重启或滚动发布生效）。

若后续需要 **RestTemplate**，可为 `RestTemplate` Bean 使用 `@RefreshScope` 或从同一 `AgentRuntimeProperties` 读取超时并重建/更新 `ClientHttpRequestFactory`（主上下文中 `refresh` 作用域可用，与 Feign 子上下文不同）。

## 7. Nacos 发布与验证建议

1. 在 Nacos 控制台编辑 `group-buy-service-runtime-dev.yml` 并发布。
2. 观察应用日志中配置刷新（或依赖你们平台对 Spring Cloud Bus / Refresh 的集成）。
3. Hikari：可通过 Actuator `metrics` / Hikari JMX / 日志确认池大小变化。
4. Feign：调整超时并 **重启或滚动发布后**，可对 `order-service` 模拟慢响应验证快速失败。

## 8. 与 DynamicTp、Sentinel 的关系

- **DynamicTp**：线程池维度（与 Agent 项同在 `group-buy-service-runtime-dev.yml`）。
- **Sentinel**：流控熔断（`application-dev.yml` 中 datasource 规则）。
- **本文 Agent 运行时**：连接池大小/超时、日志、业务开关、Feign 超时等，与上述互补，按场景选择组合使用。

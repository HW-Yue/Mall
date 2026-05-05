# group-buy-service：Agent 与 Nacos 运行时配置

拼团服务现在按职责拆成 3 份配置，不再把 DynamicTp / Hikari / Agent 全塞进一份 runtime：

| DataId | 用途 |
|------|------|
| `group-buy-service-dtp-dev.yml` | `spring.dynamic.tp.*` 与 `tomcat-tp` |
| `group-buy-service-datasource-dev.yml` | `spring.datasource.hikari.*` |
| `group-buy-service-runtime-dev.yml` | `logging.level.*`、`app.agent.*`、Feign 超时 |

## 1. 配置加载方式

入口在 `group-buy-service-app/src/main/resources/application-dev.yml` 与 `application-test.yml`：

```yaml
spring:
  config:
    import:
      - classpath:nacos/group-buy-service-dtp-dev.yml
      - optional:nacos:group-buy-service-dtp-dev.yml?group=DEFAULT_GROUP&refreshEnabled=true
      - optional:nacos:group-buy-service-datasource-dev.yml?group=DEFAULT_GROUP&refreshEnabled=true
      - classpath:nacos/group-buy-service-runtime-dev.yml
      - optional:nacos:group-buy-service-runtime-dev.yml?group=DEFAULT_GROUP&refreshEnabled=true
```

说明：

- `dtp` 与 `runtime` 有 classpath 默认模板
- `datasource` 只通过 Nacos 同名 DataId 做运行时覆盖
- Nacos group 统一为 `DEFAULT_GROUP`

## 2. Hikari 连接池

可调参数位于 `group-buy-service-datasource-dev.yml`：

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 10
      connection-timeout: 30000
```

运行时刷新实现：

- 类：`group-buy-service-app/.../HikariPoolDynamicRefresher.java`
- 行为：监听 `EnvironmentChangeEvent`，对运行中的 `HikariDataSource` 应用 `maximum-pool-size`、`minimum-idle`、`connection-timeout`

## 3. DynamicTp

线程池与 Tomcat 工作线程配置位于 `group-buy-service-dtp-dev.yml`：

```yaml
spring:
  dynamic:
    tp:
      executors:
        - thread-pool-name: threadPoolExecutor
      tomcat-tp:
        thread-pool-alias-name: group-buy-tomcat
```

它只负责线程池维度，不再混入 Hikari、日志或 Agent 参数。

## 4. Agent / 日志 / Feign

`group-buy-service-runtime-dev.yml` 现在只保留：

- `logging.level.*`
- `app.agent.cache.*`
- `app.agent.features.*`
- `app.agent.feign.order-service.*`

其中：

- 日志与 `app.agent.*` 走 Spring Cloud Refresh，可随 Nacos 刷新
- `app.agent.feign.order-service.*` 最终绑定到 `Request.Options`，修改后需重启或滚动发布实例才会完全生效

## 5. 变更与验证建议

1. 在线程池压测场景，优先改 `group-buy-service-dtp-dev.yml`
2. 在数据库压力场景，优先改 `group-buy-service-datasource-dev.yml`
3. 在排障、降噪、功能开关场景，改 `group-buy-service-runtime-dev.yml`
4. Feign 超时调整后，记得重启或滚动发布拼团实例验证生效

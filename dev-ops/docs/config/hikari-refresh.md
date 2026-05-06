# Hikari 连接池动态更新

本文说明 `spring.datasource.hikari.*` 这一类配置为什么只能“部分热更”，以及仓库里当前是怎么实现的。

## 支持范围

已实现 `HikariPoolDynamicRefresher` 的服务：

- `mall`
- `order-service`
- `group-buy-service`
- `seckill-service`
- `pay`

## 实现原理

这一类配置先通过 Nacos 导入到 Spring `Environment`：

```yaml
spring:
  config:
    import:
      - optional:nacos:xxx-service-datasource-dev.yml?group=DEFAULT_GROUP&refreshEnabled=true
```

当 Nacos 配置变化时，Spring Cloud 会触发 `EnvironmentChangeEvent`。

各服务中的 `HikariPoolDynamicRefresher` 监听这个事件：

- 只处理前缀为 `spring.datasource.hikari.` 的变更
- 只在当前 `DataSource` 实际是 `HikariDataSource` 时生效
- 从 `Environment` 重新取值
- 调用运行中 `HikariDataSource` 的 setter 应用新参数

所以它本质上不是 Hikari 自动热更，而是项目里手动做了一层桥接。

## 当前支持字段

| 服务 | 支持字段 |
|---|---|
| `mall` | `maximum-pool-size`、`minimum-idle` |
| `order-service` | `maximum-pool-size`、`minimum-idle` |
| `group-buy-service` | `maximum-pool-size`、`minimum-idle`、`connection-timeout` |
| `seckill-service` | `maximum-pool-size`、`minimum-idle` |
| `pay` | `maximum-pool-size`、`minimum-idle`、`connection-timeout` |

## 生效边界

- 表中未列出的 Hikari 参数，改完通常不会自动作用到已运行连接池。
- 即使支持动态修改，也只影响后续连接池行为，不代表已经借出的连接会立刻重建。
- 涉及 JDBC URL、用户名、密码、驱动、池实现类型这类基础项时，应按重启或滚动发布处理。

## 适用场景

- 数据库压力高时，临时调低或调高 `maximum-pool-size`
- 连接等待时间需要快速收紧时，调整 `connection-timeout`
- 做数据库降级或容量观察时，小范围修改 `minimum-idle`

## 事实来源

- `mall/mall-app/src/main/java/com/yue/config/HikariPoolDynamicRefresher.java`
- `order-service/order-service-app/src/main/java/com/yue/order/config/HikariPoolDynamicRefresher.java`
- `group-buy-service/group-buy-service-app/src/main/java/com/yue/groupbuy/config/HikariPoolDynamicRefresher.java`
- `seckill-service/seckill-service-app/src/main/java/com/yue/seckill/config/HikariPoolDynamicRefresher.java`
- `pay/pay-app/src/main/java/cn/bugstack/config/HikariPoolDynamicRefresher.java`

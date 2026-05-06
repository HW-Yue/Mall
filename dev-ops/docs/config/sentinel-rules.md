# Sentinel 规则动态更新

本文说明 Sentinel 这一类配置更新的实现原理。它和 `@RefreshScope`、`EnvironmentChangeEvent` 不是一套机制。

## 支持范围

业务服务支持以下规则类型：

- `mall`
- `order-service`
- `group-buy-service`
- `seckill-service`
- `pay`

以上服务都接入了：

- `flow`
- `degrade`
- `param-flow`
- `system`
- `authority`

网关单独接入：

- `springcloud-gateway`

网关当前规则类型：

- `gw-flow`
- `gw-api` 作为接口分组规则文件存在于 `dev-ops/nacos/sentinel-rules/gateway/`

## 实现原理

业务服务的配置形态类似：

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        flow:
          nacos:
            server-addr: ${spring.cloud.nacos.discovery.server-addr}
            dataId: ${spring.application.name}-flow-rules.json
            groupId: SENTINEL_GROUP
            rule-type: flow
            data-type: json
```

这意味着：

- Sentinel 在启动时会向 Nacos 注册规则数据源。
- 规则源是独立 JSON 文档，不是 Spring Boot 主配置 YAML 的一部分。
- Nacos DataId 内容变化后，Sentinel 数据源会刷新内存中的规则集。
- 整个过程不依赖 `@RefreshScope`，也不依赖 `EnvironmentChangeEvent`。

## 网关特殊点

`springcloud-gateway` 使用的是 Sentinel Gateway 规则，不是普通业务规则。

当前入口：

- `springcloud-gateway/app/src/main/resources/application-dev.yml`
- `springcloud-gateway/app/src/main/java/cn/bugstack/gateway/config/SentinelGatewayDataSourceConfig.java`

其中：

- `gw-flow` 对应 `GatewayFlowRule`
- `SentinelGatewayDataSourceConfig` 提供 JSON 到 `List<GatewayFlowRule>` 的转换器
- `spring.cloud.sentinel.filter.enabled=false`，避免和 Servlet CommonFilter 冲突

## 规则文件组织

规则文件统一在：

- `dev-ops/nacos/sentinel-rules/`

按类型拆分为：

- `flow/`
- `degrade/`
- `param-flow/`
- `system/`
- `authority/`
- `gateway/`

发布脚本：

- `dev-ops/nacos/sentinel-rules/init-nacos-rules.sh`
- `dev-ops/nacos/init-nacos-runtime.sh`

## 生效边界

- 修改规则 JSON 后，通常不需要重启服务。
- Dashboard 主要用于观测，不是规则真源。
- 规则是否真正拦截，还取决于服务里是否存在对应资源埋点、URI 资源命名是否一致。

## 事实来源

- `dev-ops/docs/monitoring/sentinel.md`
- `dev-ops/nacos/sentinel-rules/README.md`
- `springcloud-gateway/app/src/main/resources/application-dev.yml`
- `springcloud-gateway/app/src/main/java/cn/bugstack/gateway/config/SentinelGatewayDataSourceConfig.java`
- `mall/mall-app/src/main/resources/application-dev.yml`
- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `group-buy-service/group-buy-service-app/src/main/resources/application-dev.yml`
- `seckill-service/seckill-service-app/src/main/resources/application-dev.yml`
- `pay/pay-app/src/main/resources/application-dev.yml`

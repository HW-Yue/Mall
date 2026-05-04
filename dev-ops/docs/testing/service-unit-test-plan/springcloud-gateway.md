# Springcloud Gateway Test Plan

## Scope

- `RouteConfiguration`
- `RequestRateLimiter`
- `SentinelGatewayDataSourceConfig`

## Required Scenarios

- 路由表是否包含 `mall`、`pay`、`order-service`、`group-buy-service`、`seckill-service`、`ops-agent-spring-ai`
- `StripPrefix`、路径重写、超时配置
- 跨域配置
- Sentinel gateway fallback 响应体
- 限流 key 生成规则

## Dependency Rules

- 不依赖真实 Nacos、Sentinel Dashboard 或下游服务
- 以配置测试、路由断言、WebFlux slice 为主
- 不涉及 MQ mock

## Priority

- `P0`：路由规则、重写规则
- `P1`：Sentinel fallback、限流逻辑
- `P2`：监控与配置边缘项

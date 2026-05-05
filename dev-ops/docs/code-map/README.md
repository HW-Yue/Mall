# 代码地图

`dev-ops/docs/code-map/` 维护仓库各服务的代码入口、关键实现位置和常用配置位置，解决“从哪里开始看代码”的问题。

## 文档导航

- [网关与前端](./gateway-and-frontend.md)
- [order-service](./order-service.md)
- [group-buy-service](./group-buy-service.md)
- [seckill-service](./seckill-service.md)
- [mall](./mall.md)
- [pay](./pay.md)

## 使用方式

- 想看 HTTP 入口：先看各服务 `trigger/http`
- 想看核心业务：再看 `domain/service`
- 想看下游调用和 MQ：看 `infrastructure/adapter/port`、`infrastructure/event`、`trigger/listener`
- 想看运行配置：看 `*-app/src/main/resources/application-*.yml`

## 维护规则

- 新增关键入口文件时，优先更新对应服务文档，而不是继续把路径堆在 `AGENTS.md`
- 这里写“代码入口和实现位置”，接口参数与返回值仍以 `dev-ops/docs/api/` 为准
- MQ 详细链路仍以 `dev-ops/docs/mq/` 为准

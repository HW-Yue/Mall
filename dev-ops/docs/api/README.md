# API 文档总览

这里维护交易域和网关相关的正式 HTTP 接口文档。

## 接口地图

- `springcloud-gateway`
  - 统一入口，负责 `/gw/api/v1/**` 转发和路径裁剪
- `mall`
  - 商品展示、普通单下单入口、后台配置
- `order-service`
  - 统一订单创建、查询支付链接、退款、订单查询
- `group-buy-service`
  - 拼团市场查询、拼团下单、拼团退款
- `seckill-service`
  - 秒杀市场查询、秒杀下单、秒杀退款、后台预热
- `pay`
  - 支付订单、支付宝回调、登录与微信扫码

## 前端与网关关系

前端事实来源：

- `dev-ops/nginx/html/js/api-config.js`

网关事实来源：

- `springcloud-gateway/app/src/main/resources/application-dev.yml`

当前前端主要通过这些网关前缀进入后端：

- `/gw/api/v1/mall/** -> mall-service`
- `/gw/api/v1/pay/** -> pay-service`
- `/gw/api/v1/alipay/** -> pay-service`
- `/gw/api/v1/order/** -> order-service`
- `/gw/api/v1/group-buy/** -> group-buy-service`
- `/gw/api/v1/seckill/** -> seckill-service`
- `/gw/api/v1/ops-ai/** -> ops-agent-spring-ai`

## 服务间 HTTP 调用概览

- `mall -> order-service`
  - 普通商品锁库后调用 `create_order_normal_from_mall`
- `order-service -> pay`
  - 创建支付单、发起退款、发起关单
- `group-buy-service -> order-service`
  - 创建拼团订单、执行退款
- `seckill-service -> order-service`
  - 创建秒杀订单、执行退款

## 统一 ID 规则

- `orderId` 由 `order-service` 统一生成，格式为 `OD{snowflake}`
- `outTradeNo` 由 `order-service` 统一生成，格式为 `OT{snowflake}`
- `outTradeNo` 只允许存在于 `order-service` 与 `pay-service` 的内部调用、内部表和两者之间的 MQ
- `mall`、`group-buy-service`、`seckill-service`、前端都只感知 `orderId`

## 当前业务数据流转

### 普通单

- 前端调用 `mall create_normal_order`
- `mall` 锁普通库存后调用 `order-service create_order_normal_from_mall`
- `order-service` 生成 `orderId/outTradeNo`，立即返回 `orderId`，同时写 Redis pending 标记并发 `normal-order-create`
- 前端用 `orderId` 调 `order-service get_pay_url`
- `order-service` 通过内部 `outTradeNo` 调 `pay-service`

### 拼团单

- 前端调用 `group-buy-service create_pay_order`
- `group-buy-service` 只负责活动校验、占团锁单、建队/参团，随后调用 `order-service create_order`
- `order-service` 生成 `orderId/outTradeNo`，立即返回 `orderId` 给拼团服务；拼团服务本地只保存 `orderId`
- 前端拿 `orderId` 调 `order-service get_pay_url`
- 支付成功后链路为：`pay-success-group-buy -> order-service -> order-paid-group_buy -> group-buy-service`
- 退款/关单链路为：营销服务只调用 `order-service refund_execute(orderId)`；`order-service` 用内部 `outTradeNo` 驱动 `pay-service`，再向拼团发布仅含 `orderId` 的 `order-refund-group-buy` / `order-close-group-buy-market`

### 秒杀单

- 前端调用 `seckill-service create_pay_order`
- `seckill-service` 只生成 `seckillToken`，扣 Redis 可售库存后发 `seckill-order-create`
- `order-service` 异步建单时生成 `orderId/outTradeNo`，并把 `orderId` 回写到 `seckillToken`
- 前端轮询 `query_seckill_order` 拿到 `orderId`，再调 `get_pay_url`
- 支付成功后链路为：`pay-success-seckill -> order-service -> order-paid-seckill -> seckill-service`
- 退款/关单链路为：`order-service` 内部对 `pay-service` 仍用 `outTradeNo`，对秒杀服务只发 `order-refund-seckill` / `order-close-seckill-market`

## 服务文档

- [mall](./services/mall.md)
- [order-service](./services/order-service.md)
- [group-buy-service](./services/group-buy-service.md)
- [seckill-service](./services/seckill-service.md)
- [pay](./services/pay.md)
- [springcloud-gateway](./services/springcloud-gateway.md)

## 详细接口文档

- [详细接口索引](./details/README.md)
- [mall 详细接口](./details/mall.md)
- [order-service 详细接口](./details/order-service.md)
- [group-buy-service 详细接口](./details/group-buy-service.md)
- [seckill-service 详细接口](./details/seckill-service.md)
- [pay 详细接口](./details/pay.md)
- [springcloud-gateway 详细接口](./details/springcloud-gateway.md)

## 维护规则

接口文档以代码为准，优先读取：

1. `*-trigger/src/main/java/**/http/**`
2. `*-api/src/main/java/**`
3. `dev-ops/nginx/html/js/api-config.js`
4. `springcloud-gateway/app/src/main/resources/application-*.yml`

接口变更时必须同步修改：

- 后端 Controller / API 契约
- gateway 路由
- `api-config.js`
- 对应服务接口文档

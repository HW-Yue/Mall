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

- `/gw/api/v1/mall/** -> mall`
- `/gw/api/v1/login-pay/** -> login-pay`
- `/gw/api/v1/alipay/** -> login-pay`
- `/gw/api/v1/order/** -> order-service`
- `/gw/api/v1/group-buy/** -> group-buy-service`
- `/gw/api/v1/seckill/** -> seckill-service`
- `/gw/api/v1/ops-ai/** -> ops-agent-spring-ai`

## 服务间 HTTP 调用概览

- `mall -> order-service`
  - 普通商品锁库后调用 `create_order_normal_from_mall`
- `order-service -> pay`
  - 创建支付单
- `group-buy-service -> order-service`
  - 创建拼团订单、查询订单、执行退款
- `seckill-service -> order-service`
  - 创建秒杀订单、按外部单号查询、执行退款

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

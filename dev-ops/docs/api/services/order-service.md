# order-service 接口文档

## 服务职责

`order-service` 负责统一订单创建、支付链接获取、订单查询、普通订单退款，以及营销订单的退款执行。

详细参数和请求/响应样例见：[order-service 详细接口](../details/order-service.md)

## Base Path

- 前端入口：`/gw/api/v1/order/**`
- 服务内实际路径：`/api/v1/order/**`

## 面向前端的接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/order/create_order` | POST | 通用创建订单 | 拼团、秒杀服务 | 普通 / 拼团 / 秒杀统一入口 |
| `/api/v1/order/get_pay_url` | POST | 获取支付链接 | 支付页 | 用户拿到 `orderId` 后调用 |
| `/api/v1/order/refund` | POST | 普通订单退款 | 前端订单页 | 包含业务状态校验 |
| `/api/v1/order/query_user_order_list` | POST | 查询用户订单列表 | 订单列表页 | 游标分页 |
| `/api/v1/order/query_seckill_order` | POST | 查询秒杀建单结果 | 秒杀前端 | 用 `seckillToken` 轮询 |

## 面向服务间调用的接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/order/create_order_normal_from_mall` | POST | 普通商品锁库后落单 | `mall` | 可带 `X-Internal-Token` |
| `/api/v1/order/create_order` | POST | 营销订单创建 | `group-buy-service`、`seckill-service` | 与前端入口共用 |
| `/api/v1/order/refund_execute` | POST | 营销订单退款执行 | `group-buy-service`、`seckill-service` | 跳过前端业务校验 |
| `/api/v1/order/query_order_by_out_trade_no` | POST | 按外部单号查询订单 | `group-buy-service`、`seckill-service` | 幂等补偿、超时确认 |

## 关键同步文件

- API 契约：`order-service/order-service-api/src/main/java/com/yue/order/api/IOrderController.java`
- Controller：`order-service/order-service-trigger/src/main/java/com/yue/order/trigger/http/OrderController.java`
- 前端路径：`dev-ops/nginx/html/js/api-config.js`
- 网关路由：`springcloud-gateway/app/src/main/resources/application-*.yml`

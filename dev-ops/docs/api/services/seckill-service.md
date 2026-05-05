# seckill-service 接口文档

## 服务职责

`seckill-service` 负责秒杀商品市场查询、秒杀下单、秒杀退款，以及后台库存预热管理。

详细参数和请求/响应样例见：[seckill-service 详细接口](../details/seckill-service.md)

## Base Path

- 前端入口：`/gw/api/v1/seckill/**`
- 服务内实际路径：`/api/v1/seckill/**`

## 面向前端的接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/seckill/market/query_goods_list` | GET | 查询秒杀商品列表 | 秒杀商品页 | |
| `/api/v1/seckill/trade/create_pay_order` | POST | 秒杀下单 | 秒杀下单页 | 支持 `isTest` 压测模式 |
| `/api/v1/seckill/trade/refund` | POST | 秒杀退款 | 秒杀订单页 | 请求体为 JSON 字符串 |

## 管理端接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/seckill/admin/query_activities` | GET | 查询有效活动及商品列表 | 后台页面 | 返回当前预热状态 |
| `/api/v1/seckill/admin/preheat` | POST | 手动预热库存到 Redis | 后台页面 | 支持单商品或整活动预热 |

## 面向服务间调用

当前 `seckill-service` 自身没有额外暴露独立的服务间 HTTP 专用入口，主要通过内部 port 调用 `order-service`。

## 关键同步文件

- API 契约：`seckill-service/seckill-service-api/src/main/java/com/yue/seckill/api/**`
- Controller：`seckill-service/seckill-service-trigger/src/main/java/com/yue/seckill/trigger/http/**`
- 前端路径：`dev-ops/nginx/html/js/api-config.js`
- 网关路由：`springcloud-gateway/app/src/main/resources/application-*.yml`

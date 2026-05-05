# group-buy-service 接口文档

## 服务职责

`group-buy-service` 负责拼团商品市场查询、拼团下单、拼团退款，以及将试算、参团记录、统计聚合成拼团市场配置响应。

## Base Path

- 前端入口：`/gw/api/v1/group-buy/**`
- 服务内实际路径：`/api/v1/group-buy/**`

## 面向前端的接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/group-buy/market/query_goods_list` | GET | 查询拼团商品列表 | 拼团商品页 | 市场商品卡片 |
| `/api/v1/group-buy/market/query_group_buy_market_config` | POST | 查询拼团市场配置 | 拼团详情页 | 聚合接口，返回试算结果、进行中的团、团队统计 |
| `/api/v1/group-buy/trade/create_pay_order` | POST | 拼团下单 | 拼团下单页 | 开团或参团 |
| `/api/v1/group-buy/trade/refund` | POST | 拼团退款 | 拼团订单页 | 调用 `order-service` 执行退款 |

## 聚合说明

旧文档里出现过 `trial / query_orders / team_statistics` 的独立接口描述，但按当前代码实现，这三部分能力已经合并进：

- `/api/v1/group-buy/market/query_group_buy_market_config`

因此接口文档按现状维护，不再继续写成三个独立 HTTP 路径。

## 面向服务间调用

当前 `group-buy-service` 主要通过内部 gateway / port 调用 `order-service`，自身没有额外暴露独立的服务间 HTTP 专用入口。

## 关键同步文件

- API 契约：`group-buy-service/group-buy-service-api/src/main/java/com/yue/groupbuy/api/**`
- Controller：`group-buy-service/group-buy-service-trigger/src/main/java/com/yue/groupbuy/trigger/http/**`
- 前端路径：`dev-ops/nginx/html/js/api-config.js`
- 网关路由：`springcloud-gateway/app/src/main/resources/application-*.yml`

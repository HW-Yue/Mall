# `pay-refund-group-buy`

- 消息用途：拼团退款请求，只用于 `order-service -> pay-service`。
- 生产者：`order-service/.../OrderRefundMqProducer.java`
- 消费者：`pay/.../PayRefundGroupBuyListener.java`
- 消费者组：`CG_PAY_PAY_REFUND_GROUP_BUY`
- 消息类型：事务消息

## 关键参数

发送主体包含：

- `userId`
- `outTradeNo`
- `marketType=group_buy`
- `outTradeTime`
- `source`
- `channel`
- `bizType=pay_refund_request`，仅 `order-service` 事务消息带 header

消费者实际依赖：

- `outTradeNo`

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `pay/pay-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderRefundMqProducer.java`
- `pay/pay-trigger/src/main/java/cn/bugstack/trigger/listener/PayRefundGroupBuyListener.java`

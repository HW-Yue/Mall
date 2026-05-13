# `order-paid-group_buy`

- 消息用途：拼团订单支付成功后，由 `order-service` 通知 `group-buy-service` 执行拼团结算。
- 生产者：`order-service/order-service-infrastructure/.../OrderPaidMqProducer.java`
- 消费者：`group-buy-service/group-buy-service-trigger/.../OrderPaidGroupBuyListener.java`
- 消费者组：`CG_GROUP_BUY_ORDER_PAID`
- 消息类型：普通消息

## 关键参数

发送主体包含：

- `userId`
- `orderId`
- `marketType=group_buy`
- `outTradeTime`

消费者实际依赖：

- `userId`
- `orderId`
- `outTradeTime`

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `group-buy-service/group-buy-service-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderPaidMqProducer.java`
- `group-buy-service/group-buy-service-trigger/src/main/java/com/yue/groupbuy/trigger/listener/OrderPaidGroupBuyListener.java`

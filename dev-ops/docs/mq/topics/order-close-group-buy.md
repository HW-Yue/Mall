# `order-close-group-buy`

- 消息用途：拼团未支付关单或超时关单链路，同时驱动 `pay`、`order-service`、`group-buy-service` 各自回收状态。
- 生产者：`order-service/.../OrderCloseMqProducer.java`，`pay/.../OrderCloseMqProducer.java`，`group-buy-service/.../GroupBuyRefundMqProducer.java`
- 消费者：`pay/.../OrderCloseGroupBuyListener.java`，`order-service/.../OrderCloseGroupBuyListener.java`，`group-buy-service/.../OrderCloseGroupBuyListener.java`
- 消费者组：`CG_PAY_ORDER_CLOSE_GROUP_BUY`、`CG_ORDER_CLOSE_GROUP_BUY`、`CG_GROUP_BUY_ORDER_CLOSE`
- 消息类型：普通消息

## 关键参数

发送主体包含：

- `userId`
- `orderId`
- `outTradeNo`
- `marketType=group_buy`
- `outTradeTime`
- `source`
- `channel`

消费者实际依赖：

- `outTradeNo`
- `marketType`，`group-buy-service` 会校验为 `group_buy`

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `pay/pay-app/src/main/resources/application-dev.yml`
- `group-buy-service/group-buy-service-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderCloseMqProducer.java`
- `pay/pay-infrastructure/src/main/java/cn/bugstack/infrastructure/adapter/port/OrderCloseMqProducer.java`
- `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/event/GroupBuyRefundMqProducer.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/OrderCloseGroupBuyListener.java`
- `pay/pay-trigger/src/main/java/cn/bugstack/trigger/listener/OrderCloseGroupBuyListener.java`
- `group-buy-service/group-buy-service-trigger/src/main/java/com/yue/groupbuy/trigger/listener/OrderCloseGroupBuyListener.java`

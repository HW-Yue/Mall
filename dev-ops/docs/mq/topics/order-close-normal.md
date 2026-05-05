# `order-close-normal`

- 消息用途：普通订单关单链路，驱动 `pay` 关闭支付单，同时驱动 `order-service` 回收订单状态。
- 生产者：`order-service/.../OrderCloseMqProducer.java`，`pay/.../OrderCloseMqProducer.java`
- 消费者：`pay/.../OrderCloseNormalListener.java`，`order-service/.../OrderCloseNormalListener.java`
- 消费者组：`CG_PAY_ORDER_CLOSE_NORMAL`、`CG_ORDER_CLOSE_NORMAL`
- 消息类型：普通消息

## 关键参数

发送主体包含：

- `userId`
- `orderId`
- `outTradeNo`
- `marketType`
- `outTradeTime`
- `source`
- `channel`

消费者实际依赖：

- `outTradeNo`

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `pay/pay-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderCloseMqProducer.java`
- `pay/pay-infrastructure/src/main/java/cn/bugstack/infrastructure/adapter/port/OrderCloseMqProducer.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/OrderCloseNormalListener.java`
- `pay/pay-trigger/src/main/java/cn/bugstack/trigger/listener/OrderCloseNormalListener.java`

# `order-ship-task`

- 消息用途：订单履约任务，由 `order-service` 通过事务消息发出，再由 `order-service` 本地消费者推进发货或履约处理。
- 生产者：`order-service/order-service-infrastructure/.../OrderShipTaskMqProducer.java`
- 消费者：`order-service/order-service-trigger/.../OrderShipTaskListener.java`
- 消费者组：`CG_ORDER_SHIP_TASK`
- 消息类型：事务消息

## 关键参数

事务消息 payload 与 header 语义一致，包含：

- `bizType=order_ship_task`
- `userId`
- `orderId`
- `outTradeNo`

消费者实际依赖：

- `outTradeNo`

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderShipTaskMqProducer.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/OrderShipTaskListener.java`

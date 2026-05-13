# `order-close-seckill`

- 消息用途：秒杀订单的支付侧关单请求，仅在 `order-service -> pay-service` 与 `order-service` 本地回写中使用。
- 生产者：`order-service/.../OrderCloseMqProducer.java`，`pay/.../OrderCloseMqProducer.java`
- 消费者：`pay/.../OrderCloseSeckillListener.java`，`order-service/.../OrderCloseSeckillListener.java`
- 消费者组：`CG_PAY_ORDER_CLOSE_SECKILL`、`CG_ORDER_CLOSE_SECKILL`
- 消息类型：普通消息

## 关键参数

发送主体包含：

- `userId`
- `orderId`
- `outTradeNo`
- `marketType=seckill`
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
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/OrderCloseSeckillListener.java`
- `pay/pay-trigger/src/main/java/cn/bugstack/trigger/listener/OrderCloseSeckillListener.java`

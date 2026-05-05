# `order-paid-seckill`

- 消息用途：秒杀订单支付成功后，由 `order-service` 通知 `seckill-service` 扣减真实库存并继续发 MySQL 库存任务。
- 生产者：`order-service/order-service-infrastructure/.../OrderPaidMqProducer.java`
- 消费者：`seckill-service/seckill-service-trigger/.../OrderPaidSeckillListener.java`
- 消费者组：`CG_ORDER_PAID_SECKILL`
- 消息类型：普通消息

## 关键参数

发送主体包含：

- `userId`
- `orderId`
- `outTradeNo`
- `marketType=seckill`
- `outTradeTime`

消费者实际依赖：

- `orderId`
- `outTradeNo`

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `seckill-service/seckill-service-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderPaidMqProducer.java`
- `seckill-service/seckill-service-trigger/src/main/java/com/yue/seckill/trigger/listener/OrderPaidSeckillListener.java`

# `seckill-order-create`

- 消息用途：秒杀服务异步建单请求，由 `seckill-service` 发出，`order-service` 消费建单并写回 Redis 结果。
- 生产者：`seckill-service/seckill-service-infrastructure/.../SeckillOrderTaskPort.java`
- 消费者：`order-service/order-service-trigger/.../SeckillOrderCreateListener.java`
- 消费者组：`CG_SECKILL_ORDER_CREATE_ORDER`
- 消息类型：普通消息

## 关键参数

消费者实际依赖以下字段：

- `seckillToken`
- `userId`
- `productId`
- `activityId`
- `source`
- `channel`
- `goodsName`
- `goodsImageUrl`
- `outTradeNo`
- `originalPrice`
- `deductionPrice`
- `payPrice`

## 事实来源

- `seckill-service/seckill-service-app/src/main/resources/application-dev.yml`
- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `seckill-service/seckill-service-infrastructure/src/main/java/com/yue/seckill/infrastructure/adapter/port/SeckillOrderTaskPort.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/SeckillOrderCreateListener.java`

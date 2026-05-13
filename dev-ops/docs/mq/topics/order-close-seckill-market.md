# `order-close-seckill-market`

- 消息用途：秒杀本地关单完成事件，由 `order-service` 发给 `seckill-service`，只驱动营销侧恢复 Redis 可售库存。
- 生产者：`order-service/order-service-infrastructure/.../OrderCloseMqProducer.java`
- 消费者：`seckill-service/seckill-service-trigger/.../OrderCloseSeckillListener.java`
- 消费者组：`CG_ORDER_CLOSE_SECKILL_MARKET`
- 消息类型：普通消息

## 关键参数

- `userId`
- `orderId`
- `marketType=seckill`

消费者实际依赖：

- `orderId`

## 说明

- 这个 topic 不包含 `outTradeNo`
- 这是营销侧专用 topic；支付侧仍消费 `order-close-seckill`

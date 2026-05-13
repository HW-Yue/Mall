# `order-refund-seckill`

- 消息用途：秒杀退款完成事件，由 `order-service` 发给 `seckill-service`，驱动 Redis / MySQL 库存恢复。
- 生产者：`order-service/order-service-infrastructure/.../OrderRefundMqProducer.java`
- 消费者：`seckill-service/seckill-service-trigger/.../PayRefundSeckillListener.java`
- 消费者组：`CG_SECKILL_PAY_REFUND`
- 消息类型：普通消息

## 关键参数

- `userId`
- `orderId`
- `marketType=seckill`

消费者实际依赖：

- `orderId`

## 说明

- 这个 topic 不包含 `outTradeNo`
- 支付侧退款请求仍走 `pay-refund-seckill`

# `pay-refund-seckill`

- 消息用途：秒杀订单退款请求。`order-service` 发起支付宝退款，同时 `seckill-service` 恢复 Redis / MySQL 库存。
- 生产者：`order-service/order-service-infrastructure/.../OrderRefundMqProducer.java`
- 消费者：`pay/pay-trigger/.../PayRefundSeckillListener.java`，`seckill-service/.../PayRefundSeckillListener.java`
- 消费者组：`CG_PAY_PAY_REFUND_SECKILL`、`CG_SECKILL_PAY_REFUND`
- 消息类型：事务消息

## 关键参数

事务消息 payload 与 header 语义一致，包含：

- `bizType=pay_refund_request`
- `userId`
- `outTradeNo`
- `marketType=seckill`
- `outTradeTime`
- `source`
- `channel`

消费者实际依赖：

- `outTradeNo`
- `marketType`，`seckill-service` 会校验为 `seckill`

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `pay/pay-app/src/main/resources/application-dev.yml`
- `seckill-service/seckill-service-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderRefundMqProducer.java`
- `pay/pay-trigger/src/main/java/cn/bugstack/trigger/listener/PayRefundSeckillListener.java`
- `seckill-service/seckill-service-trigger/src/main/java/com/yue/seckill/trigger/listener/PayRefundSeckillListener.java`

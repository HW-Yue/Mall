# `pay-refund-seckill-result`

- 消息用途：秒杀订单退款完成回执，由 `pay` 回写给 `order-service`。
- 生产者：`pay/pay-infrastructure/.../PayRefundReceiptMqProducer.java`
- 消费者：`order-service/order-service-trigger/.../PayRefundSeckillListener.java`
- 消费者组：`CG_PAY_REFUND_SECKILL_RESULT`
- 消息类型：事务消息

## 关键参数

事务消息 payload 与 header 语义一致，包含：

- `userId`
- `outTradeNo`
- `marketType=seckill`

消费者实际依赖：

- `outTradeNo`

## 后续流转

- `order-service` 先按 `outTradeNo` 更新自身订单退款状态
- 随后再发布营销侧 topic `order-refund-seckill`
- 秒杀服务收到的是 `orderId` 语义消息，用它恢复真实库存与本地退款上下文

## 事实来源

- `pay/pay-app/src/main/resources/application-dev.yml`
- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `pay/pay-infrastructure/src/main/java/cn/bugstack/infrastructure/adapter/port/PayRefundReceiptMqProducer.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/PayRefundSeckillListener.java`

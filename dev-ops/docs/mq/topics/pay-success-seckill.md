# `pay-success-seckill`

- 消息用途：秒杀支付成功后，由 `pay` 通知 `order-service` 更新支付状态，再由 `order-service` 继续发布秒杀支付完成事件。
- 生产者：`pay/pay-infrastructure/.../PaySuccessMqProducer.java`
- 消费者：`order-service/order-service-trigger/.../PaySuccessSeckillListener.java`
- 消费者组：`CG_PAY_SUCCESS_SECKILL`
- 消息类型：普通消息

## 关键参数

发送主体包含：

- `userId`
- `outTradeNo`
- `outTradeTime`
- `marketType=seckill`
- `source`
- `channel`

消费者实际依赖：

- `outTradeNo`
- `outTradeTime`

## 事实来源

- `pay/pay-app/src/main/resources/application-dev.yml`
- `pay/pay-infrastructure/src/main/java/cn/bugstack/infrastructure/adapter/port/PaySuccessMqProducer.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/PaySuccessSeckillListener.java`

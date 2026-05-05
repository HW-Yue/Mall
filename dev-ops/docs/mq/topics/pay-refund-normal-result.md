# `pay-refund-normal-result`

- 消息用途：普通订单退款完成回执，由 `pay` 回写给 `order-service`，推动订单状态进入已退款。
- 生产者：`pay/pay-infrastructure/.../PayRefundReceiptMqProducer.java`
- 消费者：`order-service/order-service-trigger/.../PayRefundNormalListener.java`
- 消费者组：`CG_PAY_REFUND_NORMAL_RESULT`
- 消息类型：事务消息

## 关键参数

事务消息 payload 与 header 语义一致，包含：

- `userId`
- `outTradeNo`
- `marketType`

消费者实际依赖：

- `outTradeNo`

## 事实来源

- `pay/pay-app/src/main/resources/application-dev.yml`
- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `pay/pay-infrastructure/src/main/java/cn/bugstack/infrastructure/adapter/port/PayRefundReceiptMqProducer.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/PayRefundNormalListener.java`

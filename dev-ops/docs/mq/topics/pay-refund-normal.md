# `pay-refund-normal`

- 消息用途：普通订单退款请求，由 `order-service` 发起，`pay` 执行支付宝退款。
- 生产者：`order-service/order-service-infrastructure/.../OrderRefundMqProducer.java`
- 消费者：`pay/pay-trigger/.../PayRefundNormalListener.java`
- 消费者组：`CG_PAY_PAY_REFUND_NORMAL`
- 消息类型：事务消息

## 关键参数

事务消息 payload 与 header 语义一致，包含：

- `bizType=pay_refund_request`
- `userId`
- `outTradeNo`
- `marketType`
- `outTradeTime`
- `source`
- `channel`

消费者实际依赖：

- `outTradeNo`

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `pay/pay-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderRefundMqProducer.java`
- `pay/pay-trigger/src/main/java/cn/bugstack/trigger/listener/PayRefundNormalListener.java`

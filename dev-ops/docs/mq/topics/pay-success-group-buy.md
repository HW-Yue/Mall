# `pay-success-group-buy`

- 消息用途：拼团支付成功后，由 `pay` 通知 `order-service` 更新支付状态，再由 `order-service` 继续发布拼团支付完成事件。
- 生产者：`pay/pay-infrastructure/.../PaySuccessMqProducer.java`
- 消费者：`order-service/order-service-trigger/.../PaySuccessGroupBuyListener.java`
- 消费者组：`CG_PAY_SUCCESS_GROUP_BUY`
- 消息类型：普通消息

## 关键参数

发送主体包含：

- `userId`
- `outTradeNo`
- `outTradeTime`
- `marketType=group_buy`
- `source`
- `channel`

消费者实际依赖：

- `outTradeNo`
- `outTradeTime`

## 事实来源

- `pay/pay-app/src/main/resources/application-dev.yml`
- `pay/pay-infrastructure/src/main/java/cn/bugstack/infrastructure/adapter/port/PaySuccessMqProducer.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/PaySuccessGroupBuyListener.java`

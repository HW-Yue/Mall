# `pay-success-normal`

- 消息用途：普通订单支付成功后，由 `pay` 通知 `order-service` 更新订单支付状态。
- 生产者：`pay/pay-infrastructure/.../PaySuccessMqProducer.java`
- 消费者：`order-service/order-service-trigger/.../PaySuccessListener.java`
- 消费者组：`CG_PAY_SUCCESS_NORMAL`
- 消息类型：普通消息

## 关键参数

`pay` 发送的主体包含：

- `userId`
- `outTradeNo`
- `outTradeTime`
- `marketType`
- `source`
- `channel`

`order-service` 实际依赖：

- `outTradeNo`
- `marketType`，缺省回落为 `normal`
- `outTradeTime`

## 事实来源

- `pay/pay-app/src/main/resources/application-dev.yml`
- `pay/pay-infrastructure/src/main/java/cn/bugstack/infrastructure/adapter/port/PaySuccessMqProducer.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/PaySuccessListener.java`

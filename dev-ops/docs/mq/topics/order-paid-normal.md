# `order-paid-normal`

- 消息用途：`order-service` 为普通订单预留的支付完成事件 topic。
- 生产者：`order-service/order-service-infrastructure/.../OrderPaidMqProducer.java`
- 消费者：当前仓库内未发现显式 `@RocketMQMessageListener`
- 消费者组：无
- 消息类型：普通消息

## 关键参数

发送主体包含：

- `userId`
- `orderId`
- `outTradeNo`
- `marketType`
- `outTradeTime`

## 备注

- `application-dev.yml` 中已配置 `orderPaidNormal=order-paid-normal`。
- 当前文档仅记录实际配置，后续若补消费者，需要同步更新本页和总览。

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderPaidMqProducer.java`

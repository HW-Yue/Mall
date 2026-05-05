# `pay-refund-group-buy`

- 消息用途：拼团退款请求。`order-service` 可发事务消息请求 `pay` 退款；`group-buy-service` 也会在本地业务链路发普通退款消息。
- 生产者：`order-service/.../OrderRefundMqProducer.java`，`group-buy-service/.../GroupBuyRefundMqProducer.java`
- 消费者：`pay/.../PayRefundGroupBuyListener.java`，`group-buy-service/.../PayRefundGroupBuyListener.java`
- 消费者组：`CG_PAY_PAY_REFUND_GROUP_BUY`、`CG_GROUP_BUY_PAY_REFUND`
- 消息类型：混合，`order-service` 为事务消息，`group-buy-service` 为普通消息

## 关键参数

发送主体包含：

- `userId`
- `outTradeNo`
- `marketType=group_buy`
- `outTradeTime`
- `source`
- `channel`
- `bizType=pay_refund_request`，仅 `order-service` 事务消息带 header

消费者实际依赖：

- `outTradeNo`

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `pay/pay-app/src/main/resources/application-dev.yml`
- `group-buy-service/group-buy-service-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderRefundMqProducer.java`
- `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/event/GroupBuyRefundMqProducer.java`
- `pay/pay-trigger/src/main/java/cn/bugstack/trigger/listener/PayRefundGroupBuyListener.java`
- `group-buy-service/group-buy-service-trigger/src/main/java/com/yue/groupbuy/trigger/listener/PayRefundGroupBuyListener.java`

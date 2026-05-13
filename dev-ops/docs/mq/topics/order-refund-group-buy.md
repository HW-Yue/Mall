# `order-refund-group-buy`

- 消息用途：拼团退款完成事件，由 `order-service` 发给 `group-buy-service`，驱动本地订单状态从退款处理中更新为已退款。
- 生产者：`order-service/order-service-infrastructure/.../OrderRefundMqProducer.java`
- 消费者：`group-buy-service/group-buy-service-trigger/.../PayRefundGroupBuyListener.java`
- 消费者组：`CG_GROUP_BUY_PAY_REFUND`
- 消息类型：普通消息

## 关键参数

- `userId`
- `orderId`
- `marketType=group_buy`

消费者实际依赖：

- `orderId`

## 说明

- 这个 topic 不包含 `outTradeNo`
- 支付侧退款请求仍走 `pay-refund-group-buy`

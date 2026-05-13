# `order-close-group-buy-market`

- 消息用途：拼团本地关单完成事件，由 `order-service` 发给 `group-buy-service`，只驱动营销侧回退团占用库存与本地状态。
- 生产者：`order-service/order-service-infrastructure/.../OrderCloseMqProducer.java`
- 消费者：`group-buy-service/group-buy-service-trigger/.../OrderCloseGroupBuyListener.java`
- 消费者组：`CG_GROUP_BUY_ORDER_CLOSE`
- 消息类型：普通消息

## 关键参数

- `userId`
- `orderId`
- `marketType=group_buy`

消费者实际依赖：

- `orderId`

## 说明

- 这个 topic 不包含 `outTradeNo`
- 这是营销侧专用 topic；支付侧仍消费 `order-close-group-buy`

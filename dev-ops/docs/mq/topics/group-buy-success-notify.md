# `group-buy-success-notify`

- 消息用途：拼团成团成功通知，由 `group-buy-service` 发送给 `order-service`，驱动后续结算或通知逻辑。
- 生产者：`group-buy-service/group-buy-service-infrastructure/.../GroupBuyEventPublisher.java`
- 消费者：`order-service/order-service-trigger/.../GroupBuySuccessNotifyListener.java`
- 消费者组：`CG_GROUP_BUY_SUCCESS_NOTIFY`
- 消息类型：普通消息

## 关键参数

发送主体为 `TeamSuccessNotifyMessage`，包含：

- `teamId`
- `orders[]`
- `orders[].orderId`
- `orders[].userId`
- `orders[].goodsType`
- `orders[].resKey`
- `orders[].resValue`
- `orders[].amount`

消费者实际依赖：

- 当前监听器直接将原始消息透传给 `orderDomainService.handleGroupBuySuccess(message)`，字段约束以领域服务解析逻辑为准。

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/event/GroupBuyEventPublisher.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/GroupBuySuccessNotifyListener.java`

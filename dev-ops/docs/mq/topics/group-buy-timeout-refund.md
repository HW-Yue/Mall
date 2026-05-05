# `group-buy-timeout-refund`

- 消息用途：拼团超时退款定时消息，到达拼团截止时间后由 `group-buy-service` 自消费，处理未成团队伍的退款或关单。
- 生产者：`group-buy-service/group-buy-service-infrastructure/.../GroupBuyTimeoutRefundProducer.java`
- 消费者：`group-buy-service/group-buy-service-trigger/.../GroupBuyTimeoutRefundListener.java`
- 消费者组：`CG_GROUP_BUY_TIMEOUT_REFUND`
- 消息类型：定时消息

## 关键参数

发送主体包含：

- `teamId`
- `deliverTimeMs`

消费者实际依赖：

- `teamId`

## 事实来源

- `group-buy-service/group-buy-service-app/src/main/resources/application-dev.yml`
- `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/event/GroupBuyTimeoutRefundProducer.java`
- `group-buy-service/group-buy-service-trigger/src/main/java/com/yue/groupbuy/trigger/listener/GroupBuyTimeoutRefundListener.java`

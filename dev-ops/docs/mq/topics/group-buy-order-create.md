# `group-buy-order-create`

- 消息用途：拼团商品异步落单。`order-service.createOrder` 在生成 `orderId/outTradeNo`、写入 Redis 存在标记后同步投递；由 `order-service` 自身消费 INSERT `t_order`。
- 生产者：`order-service/order-service-infrastructure/.../GroupBuyOrderPendingPublisher.java`
- 消费者：`order-service/order-service-trigger/.../GroupBuyOrderCreateListener.java`
- 消费者组：`CG_GROUP_BUY_ORDER_CREATE`
- 消息类型：同步投递普通消息

## 关键参数

消费者实际依赖以下字段：

- `orderId`
- `userId`
- `outTradeNo`
- `payPrice`
- `goodsId`
- `goodsName`
- `goodsImageUrl`
- `source`
- `channel`
- `originalPrice`
- `deductionPrice`
- `marketType`（固定 `group_buy`）
- `notifyType`

## 备注

- 拼团服务自管 `lock_count`，落库时不调用 `mallDubboService.lockStock`，与 `normal-order-create` 区分开。
- 发送前在 Redis 写入存在标记 `order:exists:{userId}:{orderId}`（默认 30 min TTL），消费成功后由 listener `DEL`，TTL 兜底。
- 发送失败时 publisher 抛 `AppException`，上层 `OrderDomainService.createOrder` 会清理 Redis 标记并把异常抛回 group-buy-service。

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/GroupBuyOrderPendingPublisher.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/GroupBuyOrderCreateListener.java`
- `order-service/order-service-domain/src/main/java/com/yue/order/domain/order/service/OrderDomainService.java`

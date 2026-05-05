# `normal-order-create`

- 消息用途：普通商品异步落单。`mall` 侧完成锁库后，经 `order-service` 将待建单消息同步投递，再由 `order-service` 消费落库。
- 生产者：`order-service/order-service-infrastructure/.../NormalOrderPendingPublisher.java`
- 消费者：`order-service/order-service-trigger/.../NormalOrderCreateListener.java`
- 消费者组：`CG_NORMAL_ORDER_CREATE`
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
- `notifyType`

## 备注

- topic 实际由 `order-service` 发出，但业务入口在 `mall` 普通下单链路。
- 发送端只校验发送成功，不在该类中定义字段结构，字段约束以消费者实现为准。

## 事实来源

- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/NormalOrderPendingPublisher.java`
- `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/NormalOrderCreateListener.java`

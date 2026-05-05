# MQ 文档

`dev-ops/docs/mq/` 维护仓库内业务 RocketMQ topic 的文字说明。图形化路由图仍放在 `dev-ops/docs/diagrams/mq/`。

## 生产者视角

| 生产者 | Topic | 消费者 | 消费者组 | 类型 | 详情 |
|---|---|---|---|---|---|
| `pay` | `pay-success-normal` | `order-service` | `CG_PAY_SUCCESS_NORMAL` | 普通 | [pay-success-normal](./topics/pay-success-normal.md) |
| `pay` | `pay-success-group-buy` | `order-service` | `CG_PAY_SUCCESS_GROUP_BUY` | 普通 | [pay-success-group-buy](./topics/pay-success-group-buy.md) |
| `pay` | `pay-success-seckill` | `order-service` | `CG_PAY_SUCCESS_SECKILL` | 普通 | [pay-success-seckill](./topics/pay-success-seckill.md) |
| `pay` | `order-close-normal` | `pay`、`order-service` | `CG_PAY_ORDER_CLOSE_NORMAL`、`CG_ORDER_CLOSE_NORMAL` | 普通 | [order-close-normal](./topics/order-close-normal.md) |
| `pay` | `order-close-group-buy` | `pay`、`order-service`、`group-buy-service` | `CG_PAY_ORDER_CLOSE_GROUP_BUY`、`CG_ORDER_CLOSE_GROUP_BUY`、`CG_GROUP_BUY_ORDER_CLOSE` | 普通 | [order-close-group-buy](./topics/order-close-group-buy.md) |
| `pay` | `order-close-seckill` | `pay`、`order-service`、`seckill-service` | `CG_PAY_ORDER_CLOSE_SECKILL`、`CG_ORDER_CLOSE_SECKILL`、`CG_ORDER_CLOSE_SECKILL_MARKET` | 普通 | [order-close-seckill](./topics/order-close-seckill.md) |
| `pay` | `pay-refund-normal-result` | `order-service` | `CG_PAY_REFUND_NORMAL_RESULT` | 事务消息 | [pay-refund-normal-result](./topics/pay-refund-normal-result.md) |
| `pay` | `pay-refund-group-buy-result` | `order-service` | `CG_PAY_REFUND_GROUP_BUY_RESULT` | 事务消息 | [pay-refund-group-buy-result](./topics/pay-refund-group-buy-result.md) |
| `pay` | `pay-refund-seckill-result` | `order-service` | `CG_PAY_REFUND_SECKILL_RESULT` | 事务消息 | [pay-refund-seckill-result](./topics/pay-refund-seckill-result.md) |
| `order-service` | `order-paid-normal` | 当前无显式消费者 | 无 | 普通 | [order-paid-normal](./topics/order-paid-normal.md) |
| `order-service` | `order-paid-group_buy` | `group-buy-service` | `CG_GROUP_BUY_ORDER_PAID` | 普通 | [order-paid-group_buy](./topics/order-paid-group_buy.md) |
| `order-service` | `order-paid-seckill` | `seckill-service` | `CG_ORDER_PAID_SECKILL` | 普通 | [order-paid-seckill](./topics/order-paid-seckill.md) |
| `order-service` | `order-close-normal` | `pay`、`order-service` | `CG_PAY_ORDER_CLOSE_NORMAL`、`CG_ORDER_CLOSE_NORMAL` | 普通 | [order-close-normal](./topics/order-close-normal.md) |
| `order-service` | `order-close-group-buy` | `pay`、`order-service`、`group-buy-service` | `CG_PAY_ORDER_CLOSE_GROUP_BUY`、`CG_ORDER_CLOSE_GROUP_BUY`、`CG_GROUP_BUY_ORDER_CLOSE` | 普通 | [order-close-group-buy](./topics/order-close-group-buy.md) |
| `order-service` | `order-close-seckill` | `pay`、`order-service`、`seckill-service` | `CG_PAY_ORDER_CLOSE_SECKILL`、`CG_ORDER_CLOSE_SECKILL`、`CG_ORDER_CLOSE_SECKILL_MARKET` | 普通 | [order-close-seckill](./topics/order-close-seckill.md) |
| `order-service` | `pay-refund-normal` | `pay` | `CG_PAY_PAY_REFUND_NORMAL` | 事务消息 | [pay-refund-normal](./topics/pay-refund-normal.md) |
| `order-service` | `pay-refund-group-buy` | `pay`、`group-buy-service` | `CG_PAY_PAY_REFUND_GROUP_BUY`、`CG_GROUP_BUY_PAY_REFUND` | 事务消息 | [pay-refund-group-buy](./topics/pay-refund-group-buy.md) |
| `order-service` | `pay-refund-seckill` | `pay`、`seckill-service` | `CG_PAY_PAY_REFUND_SECKILL`、`CG_SECKILL_PAY_REFUND` | 事务消息 | [pay-refund-seckill](./topics/pay-refund-seckill.md) |
| `order-service` | `normal-order-create` | `order-service` | `CG_NORMAL_ORDER_CREATE` | 同步投递普通消息 | [normal-order-create](./topics/normal-order-create.md) |
| `order-service` | `order-ship-task` | `order-service` | `CG_ORDER_SHIP_TASK` | 事务消息 | [order-ship-task](./topics/order-ship-task.md) |
| `group-buy-service` | `group-buy-success-notify` | `order-service` | `CG_GROUP_BUY_SUCCESS_NOTIFY` | 普通 | [group-buy-success-notify](./topics/group-buy-success-notify.md) |
| `group-buy-service` | `group-buy-timeout-refund` | `group-buy-service` | `CG_GROUP_BUY_TIMEOUT_REFUND` | 定时消息 | [group-buy-timeout-refund](./topics/group-buy-timeout-refund.md) |
| `group-buy-service` | `order-close-group-buy` | `pay`、`order-service`、`group-buy-service` | `CG_PAY_ORDER_CLOSE_GROUP_BUY`、`CG_ORDER_CLOSE_GROUP_BUY`、`CG_GROUP_BUY_ORDER_CLOSE` | 普通 | [order-close-group-buy](./topics/order-close-group-buy.md) |
| `group-buy-service` | `pay-refund-group-buy` | `pay`、`group-buy-service` | `CG_PAY_PAY_REFUND_GROUP_BUY`、`CG_GROUP_BUY_PAY_REFUND` | 普通 | [pay-refund-group-buy](./topics/pay-refund-group-buy.md) |
| `seckill-service` | `seckill-order-create` | `order-service` | `CG_SECKILL_ORDER_CREATE_ORDER` | 普通 | [seckill-order-create](./topics/seckill-order-create.md) |
| `seckill-service` | `seckill-stock-deduct` | `seckill-service` | `CG_SECKILL_STOCK_DEDUCT` | 普通 | [seckill-stock-deduct](./topics/seckill-stock-deduct.md) |

## 消费者视角

| 消费者 | Topic | 生产者 | 消费者组 | 监听入口 | 详情 |
|---|---|---|---|---|---|
| `order-service` | `pay-success-normal` | `pay` | `CG_PAY_SUCCESS_NORMAL` | `PaySuccessListener` | [pay-success-normal](./topics/pay-success-normal.md) |
| `order-service` | `pay-success-group-buy` | `pay` | `CG_PAY_SUCCESS_GROUP_BUY` | `PaySuccessGroupBuyListener` | [pay-success-group-buy](./topics/pay-success-group-buy.md) |
| `order-service` | `pay-success-seckill` | `pay` | `CG_PAY_SUCCESS_SECKILL` | `PaySuccessSeckillListener` | [pay-success-seckill](./topics/pay-success-seckill.md) |
| `order-service` | `order-close-normal` | `pay`、`order-service` | `CG_ORDER_CLOSE_NORMAL` | `OrderCloseNormalListener` | [order-close-normal](./topics/order-close-normal.md) |
| `order-service` | `order-close-group-buy` | `pay`、`order-service`、`group-buy-service` | `CG_ORDER_CLOSE_GROUP_BUY` | `OrderCloseGroupBuyListener` | [order-close-group-buy](./topics/order-close-group-buy.md) |
| `order-service` | `order-close-seckill` | `pay`、`order-service` | `CG_ORDER_CLOSE_SECKILL` | `OrderCloseSeckillListener` | [order-close-seckill](./topics/order-close-seckill.md) |
| `order-service` | `pay-refund-normal-result` | `pay` | `CG_PAY_REFUND_NORMAL_RESULT` | `PayRefundNormalListener` | [pay-refund-normal-result](./topics/pay-refund-normal-result.md) |
| `order-service` | `pay-refund-group-buy-result` | `pay` | `CG_PAY_REFUND_GROUP_BUY_RESULT` | `PayRefundGroupBuyListener` | [pay-refund-group-buy-result](./topics/pay-refund-group-buy-result.md) |
| `order-service` | `pay-refund-seckill-result` | `pay` | `CG_PAY_REFUND_SECKILL_RESULT` | `PayRefundSeckillListener` | [pay-refund-seckill-result](./topics/pay-refund-seckill-result.md) |
| `order-service` | `normal-order-create` | `order-service` | `CG_NORMAL_ORDER_CREATE` | `NormalOrderCreateListener` | [normal-order-create](./topics/normal-order-create.md) |
| `order-service` | `seckill-order-create` | `seckill-service` | `CG_SECKILL_ORDER_CREATE_ORDER` | `SeckillOrderCreateListener` | [seckill-order-create](./topics/seckill-order-create.md) |
| `order-service` | `order-ship-task` | `order-service` | `CG_ORDER_SHIP_TASK` | `OrderShipTaskListener` | [order-ship-task](./topics/order-ship-task.md) |
| `order-service` | `group-buy-success-notify` | `group-buy-service` | `CG_GROUP_BUY_SUCCESS_NOTIFY` | `GroupBuySuccessNotifyListener` | [group-buy-success-notify](./topics/group-buy-success-notify.md) |
| `group-buy-service` | `order-paid-group_buy` | `order-service` | `CG_GROUP_BUY_ORDER_PAID` | `OrderPaidGroupBuyListener` | [order-paid-group_buy](./topics/order-paid-group_buy.md) |
| `group-buy-service` | `order-close-group-buy` | `pay`、`order-service`、`group-buy-service` | `CG_GROUP_BUY_ORDER_CLOSE` | `OrderCloseGroupBuyListener` | [order-close-group-buy](./topics/order-close-group-buy.md) |
| `group-buy-service` | `pay-refund-group-buy` | `order-service`、`group-buy-service` | `CG_GROUP_BUY_PAY_REFUND` | `PayRefundGroupBuyListener` | [pay-refund-group-buy](./topics/pay-refund-group-buy.md) |
| `group-buy-service` | `group-buy-timeout-refund` | `group-buy-service` | `CG_GROUP_BUY_TIMEOUT_REFUND` | `GroupBuyTimeoutRefundListener` | [group-buy-timeout-refund](./topics/group-buy-timeout-refund.md) |
| `seckill-service` | `order-paid-seckill` | `order-service` | `CG_ORDER_PAID_SECKILL` | `OrderPaidSeckillListener` | [order-paid-seckill](./topics/order-paid-seckill.md) |
| `seckill-service` | `order-close-seckill` | `pay`、`order-service` | `CG_ORDER_CLOSE_SECKILL_MARKET` | `OrderCloseSeckillListener` | [order-close-seckill](./topics/order-close-seckill.md) |
| `seckill-service` | `pay-refund-seckill` | `order-service` | `CG_SECKILL_PAY_REFUND` | `PayRefundSeckillListener` | [pay-refund-seckill](./topics/pay-refund-seckill.md) |
| `seckill-service` | `seckill-stock-deduct` | `seckill-service` | `CG_SECKILL_STOCK_DEDUCT` | `SeckillStockDeductListener` | [seckill-stock-deduct](./topics/seckill-stock-deduct.md) |
| `pay` | `order-close-normal` | `pay`、`order-service` | `CG_PAY_ORDER_CLOSE_NORMAL` | `OrderCloseNormalListener` | [order-close-normal](./topics/order-close-normal.md) |
| `pay` | `order-close-group-buy` | `pay`、`order-service`、`group-buy-service` | `CG_PAY_ORDER_CLOSE_GROUP_BUY` | `OrderCloseGroupBuyListener` | [order-close-group-buy](./topics/order-close-group-buy.md) |
| `pay` | `order-close-seckill` | `pay`、`order-service` | `CG_PAY_ORDER_CLOSE_SECKILL` | `OrderCloseSeckillListener` | [order-close-seckill](./topics/order-close-seckill.md) |
| `pay` | `pay-refund-normal` | `order-service` | `CG_PAY_PAY_REFUND_NORMAL` | `PayRefundNormalListener` | [pay-refund-normal](./topics/pay-refund-normal.md) |
| `pay` | `pay-refund-group-buy` | `order-service`、`group-buy-service` | `CG_PAY_PAY_REFUND_GROUP_BUY` | `PayRefundGroupBuyListener` | [pay-refund-group-buy](./topics/pay-refund-group-buy.md) |
| `pay` | `pay-refund-seckill` | `order-service` | `CG_PAY_PAY_REFUND_SECKILL` | `PayRefundSeckillListener` | [pay-refund-seckill](./topics/pay-refund-seckill.md) |

## 维护规则

- 事实来源优先看各服务 `application-*.yml` 中的 topic 和 consumer group 配置。
- 单个 topic 的字段说明以 producer 实际发送内容和 consumer 实际解析字段为准。
- 新增或修改 MQ 链路时，先补 `topics/*.md`，再回到本索引更新生产者表和消费者表。

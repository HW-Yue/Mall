# `seckill-stock-deduct`

- 消息用途：秒杀库存异步回写 MySQL。支付成功时发 `deduct`，退款恢复时发 `recover`。
- 生产者：`seckill-service/seckill-service-infrastructure/.../SeckillStockDeductPort.java`
- 消费者：`seckill-service/seckill-service-trigger/.../SeckillStockDeductListener.java`
- 消费者组：`CG_SECKILL_STOCK_DEDUCT`
- 消息类型：普通消息

## 关键参数

发送主体包含：

- `activityId`
- `productId`
- `op`，取值 `deduct` 或 `recover`

消费者实际依赖：

- `activityId`
- `productId`
- `op`

## 事实来源

- `seckill-service/seckill-service-app/src/main/resources/application-dev.yml`
- `seckill-service/seckill-service-infrastructure/src/main/java/com/yue/seckill/infrastructure/adapter/port/SeckillStockDeductPort.java`
- `seckill-service/seckill-service-trigger/src/main/java/com/yue/seckill/trigger/listener/SeckillStockDeductListener.java`

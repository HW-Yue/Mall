# Seckill Service Test Plan

## Scope

- `domain/activity`
  - `SeckillMarketServiceImpl`
  - `SeckillAdminServiceImpl`
- `domain/trade`
  - `SeckillTradeServiceImpl`
- `trigger`
  - `SeckillMarketController`
  - `SeckillTradeController`
  - `SeckillAdminController`
  - `OrderCloseSeckillListener`
  - `OrderPaidSeckillListener`
  - `PayRefundSeckillListener`
  - `SeckillStockDeductListener`
  - `SeckillStockPreheatJob`
- `infrastructure`
  - `SeckillActivityRepository`
  - `SeckillGoodsCachePort`
  - `SeckillStockPort`
  - `OrderServicePort`
  - `SeckillOrderTaskPort`
  - `SeckillStockDeductPort`

## Required Scenarios

- 活动查询、商品预热、活动不存在、未开始、已结束
- 秒杀 token 校验、重复抢购、库存不足
- Redis 缓存命中/回源
- 秒杀下单消息发送、库存扣减/恢复消息发送
- 支付成功、关单、退款后的库存与订单联动
- 后台预热接口与手动触发逻辑

## Dependency Rules

- MySQL：真实测试库
- Redis：真实测试 Redis
- MQ：mock `RocketMQTemplate`
- Feign：
  - `OrderServicePort` 保留实现
  - 内部 `IOrderService` Feign Client mock
  - 验证创建订单、查询订单、退款执行的 DTO 映射与异常处理

## Priority

- `P0`：`SeckillTradeServiceImpl`、库存链路、消息发送
- `P1`：market/admin controller、listener、Feign port
- `P2`：预热 job 和边缘缓存分支

# Group Buy Service Test Plan

## Scope

- `domain/activity`
  - `IndexGroupBuyMarketServiceImpl`
  - `DefaultActivityStrategyFactory`
  - `ZKCalculateService`
  - `NCalculateService`
  - `MJCalculateService`
  - `ZJCalculateService`
  - `RootNode`
  - `SwitchNode`
  - `TagNode`
  - `MarketNode`
  - `EndNode`
  - `ErrorNode`
- `domain/trade`
  - `GroupBuyDomainService`
  - `TradeLockOrderService`
  - `TradeSettlementOrderService`
  - `TradeRefundOrderService`
  - `TradeTaskService`
  - `GroupBuyTradeOrderStateMachine`
  - `GroupBuyTeamStateMachine`
  - `Paid2RefundStrategy`
  - `PaidTeam2RefundStrategy`
  - `Unpaid2RefundStrategy`
- `trigger`
  - `GroupBuyMarketController`
  - `GroupBuyTradeController`
  - `GroupBuyTimeoutRefundListener`
  - `OrderCloseGroupBuyListener`
  - `OrderPaidGroupBuyListener`
  - `PayRefundGroupBuyListener`
- `infrastructure`
  - `ActivityRepository`
  - `GroupBuyRepository`
  - `TradeRepository`
  - `OrderServicePort`
  - `TradePort`
  - `GroupBuyEventPublisher`
  - `GroupBuyRefundMqProducer`
  - `GroupBuyTimeoutRefundProducer`

## Required Scenarios

- 活动试算、标签命中、折扣计算、无可用活动
- 开团/参团锁单、库存占用、限购、活动失效
- 成团、未成团、超时退款、已支付退款
- team 与 trade order 双状态机推进
- 通知任务类别分发
- 发给 `order-service`、`pay` 的请求参数组装
- 成团通知、退款、超时消息的 topic 与 payload

## Dependency Rules

- MySQL：真实测试库
- Redis：真实测试 Redis
- MQ：mock `RocketMQTemplate`
- Dubbo：
  - `OrderServicePort` 保留实现
  - 内部 `IOrderDubboService` mock（`@DubboReference`，通过 `ReflectionTestUtils.setField` 注入）
  - 验证请求 DTO、失败补偿、按 `orderId` 的退款/关单/结算推进逻辑

## Priority

- `P0`：锁单、结算、退款、状态机、规则链
- `P1`：controller、listener、event publisher、Dubbo port
- `P2`：活动试算的非主路径与异常提示

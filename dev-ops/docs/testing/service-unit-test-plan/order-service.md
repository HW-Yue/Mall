# Order Service Test Plan

## Scope

- `domain`
  - `OrderDomainService`
  - `OrderStateMachine`
  - `MarketTypeVO`
  - `OrderStatusVO`
- `trigger`
  - `OrderController`
  - `TimeoutCloseOrderJob`
  - `GroupBuySuccessNotifyListener`
  - `NormalOrderCreateListener`
  - `SeckillOrderCreateListener`
  - `PaySuccess*Listener`
  - `PayRefund*Listener`
  - `OrderClose*Listener`
  - `OrderShipTaskListener`
- `infrastructure`
  - `OrderRepository`
  - `PayServicePort`
  - `NormalOrderPendingPublisher`
  - `NormalOrderPendingPublisherStub`
  - `OrderPaidMqProducer`
  - `OrderCloseMqProducer`
  - `OrderRefundMqProducer`
  - `OrderShipTaskMqProducer`
  - `OrderShipTaskTransactionListener`

## Required Scenarios

- 创建订单幂等、普通单开关、`outTradeNo` 冲突
- `submitNormalOrderFromMall` 入队消息体组装
- `getPayUrl` 重试、订单不存在、状态非法
- 支付成功、拼团成功、关单、退款、发货任务状态推进
- MQ consumer 对不同 market 事件的分发与容错
- `PayServicePort` 返回空值、失败码、异常传播
- 无 MQ 配置时 stub 行为
- `IMallService` / `IPayService` 的请求 DTO 组装与失败回滚

## Dependency Rules

- MySQL：真实测试库
- Redis：真实测试 Redis
- MQ：mock `RocketMQTemplate`
- Feign：
  - `PayServicePort` 内部 Feign Client mock
  - `Mall` 下游调用 mock
  - 不走真实 Nacos 服务发现

## Priority

- `P0`：`OrderDomainService`、`OrderStateMachine`、`OrderController`
- `P1`：各类 listener、publisher、transaction listener、Feign port
- `P2`：repository 细节与边缘错误分支

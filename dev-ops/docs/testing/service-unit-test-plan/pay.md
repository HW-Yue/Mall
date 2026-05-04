# Pay Test Plan

## Scope

- `domain`
  - `OrderService`
  - `AbstractOrderService`
  - `PayOrderStateMachine`
  - `WeixinLoginService`
- `trigger`
  - `AliPayController`
  - `LoginController`
  - `WeixinPortalController`
  - `TimeoutCloseOrderJob`
  - `NoPayNotifyOrderJob`
  - `OrderClose*Listener`
  - `PayRefund*Listener`
- `infrastructure`
  - `PaySuccessMqProducer`
  - `OrderCloseMqProducer`
  - `OrderRefundMqProducer`
  - `PayRefundReceiptMqProducer`
  - `PayRefundReceiptTransactionListener`
  - `OrderRepository`
  - `LoginPort`

## Required Scenarios

- 创建支付单、重复创建、不同 `marketType` 路由
- 支付成功、关单、退款状态迁移
- 支付回调签名通过、失败、`mock_sign_bypass`
- MQ topic、payload、header 组装
- 退款回执事务消息本地事务分支
- 超时关单、未支付通知 job 分发

## Dependency Rules

- MySQL：真实测试库
- Redis：无特殊 mock 要求
- MQ：mock `RocketMQTemplate`
- Feign / HTTP gateway：对 `LoginPort`、外部接口做 mock 或 stub

## Priority

- `P0`：`OrderService`、`PayOrderStateMachine`、`AliPayController`、`PaySuccessMqProducer`
- `P1`：退款/关单 listener、job、repository
- `P2`：登录与微信相关边缘路径

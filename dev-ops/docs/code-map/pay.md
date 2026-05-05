# pay 代码地图

## 启动与配置

- 启动模块：`pay/pay-app`
- 启动类：`cn.bugstack.PayApplication`
- 配置文件：
  - `pay/pay-app/src/main/resources/application-dev.yml`
  - `pay/pay-app/src/main/resources/application-test.yml`
  - `pay/pay-app/src/main/resources/application-prod.yml`
  - `pay/pay-app/src/main/resources/application-test-mock.yml`

## HTTP 入口

- 支付宝支付相关：
  - `pay/pay-trigger/src/main/java/cn/bugstack/trigger/http/AliPayController.java`
- 微信登录 / 门户相关：
  - `pay/pay-trigger/src/main/java/cn/bugstack/trigger/http/`

## 核心业务

- 领域服务：
  - `pay/pay-domain/src/main/java/cn/bugstack/domain/`
- 支付单创建、支付确认、关单、退款核心逻辑从这里进入

## 下游调用与适配

- 支付成功消息发送：
  - `pay/pay-infrastructure/src/main/java/cn/bugstack/infrastructure/adapter/port/PaySuccessMqProducer.java`
- 关单消息发送：
  - `OrderCloseMqProducer.java`
- 退款请求消息发送：
  - `OrderRefundMqProducer.java`
- 退款回执事务消息：
  - `PayRefundReceiptMqProducer.java`

## MQ

### 消费者

- 关单：
  - `pay/pay-trigger/src/main/java/cn/bugstack/trigger/listener/OrderCloseNormalListener.java`
  - `OrderCloseGroupBuyListener.java`
  - `OrderCloseSeckillListener.java`
- 退款请求：
  - `PayRefundNormalListener.java`
  - `PayRefundGroupBuyListener.java`
  - `PayRefundSeckillListener.java`

### 生产者

- 支付成功：
  - `pay/pay-infrastructure/src/main/java/cn/bugstack/infrastructure/adapter/port/PaySuccessMqProducer.java`
- 关单：
  - `OrderCloseMqProducer.java`
- 退款请求：
  - `OrderRefundMqProducer.java`
- 退款回执：
  - `PayRefundReceiptMqProducer.java`

## 什么时候先看这里

- 支付单创建
- 支付宝回调
- 支付 / 关单 / 退款 MQ 链路

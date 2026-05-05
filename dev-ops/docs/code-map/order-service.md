# order-service 代码地图

## 启动与配置

- 启动模块：`order-service/order-service-app`
- 启动类：`com.yue.order.OrderServiceApplication`
- 配置文件：
  - `order-service/order-service-app/src/main/resources/application-dev.yml`
  - `order-service/order-service-app/src/main/resources/application-test.yml`
  - `order-service/order-service-app/src/main/resources/application-prod.yml`
  - `order-service/order-service-app/src/main/resources/application-test-mock.yml`

## HTTP 入口

- 主 Controller：
  - `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/http/OrderController.java`
- API 契约：
  - `order-service/order-service-api/src/main/java/com/yue/order/api/IOrderController.java`

## 核心业务

- 订单领域服务：
  - `order-service/order-service-domain/src/main/java/com/yue/order/domain/order/service/`
- 创建单、支付成功、关单、退款、履约的核心逻辑都从这里进入

## 下游调用与适配

- 支付服务出站 Port：
  - `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/adapter/port/PayServicePort.java`
- repository / mapper 入口：
  - `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/repository/`
  - `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/dao/`

## MQ

### 消费者

- 支付成功监听：
  - `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/PaySuccessListener.java`
  - `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/PaySuccessGroupBuyListener.java`
  - `order-service/order-service-trigger/src/main/java/com/yue/order/trigger/listener/PaySuccessSeckillListener.java`
- 关单监听：
  - `OrderCloseNormalListener.java`
  - `OrderCloseGroupBuyListener.java`
  - `OrderCloseSeckillListener.java`
- 退款回执监听：
  - `PayRefundNormalListener.java`
  - `PayRefundGroupBuyListener.java`
  - `PayRefundSeckillListener.java`
- 异步建单 / 履约：
  - `NormalOrderCreateListener.java`
  - `SeckillOrderCreateListener.java`
  - `OrderShipTaskListener.java`
  - `GroupBuySuccessNotifyListener.java`

### 生产者

- 支付完成：
  - `order-service/order-service-infrastructure/src/main/java/com/yue/order/infrastructure/event/OrderPaidMqProducer.java`
- 关单：
  - `OrderCloseMqProducer.java`
- 退款事务消息：
  - `OrderRefundMqProducer.java`
- 普通单异步落单：
  - `NormalOrderPendingPublisher.java`
- 履约任务：
  - `OrderShipTaskMqProducer.java`

## 什么时候先看这里

- 普通单 / 拼团单 / 秒杀单统一订单逻辑
- `order-service -> pay` 出站调用
- 支付成功、关单、退款、履约 MQ 链路

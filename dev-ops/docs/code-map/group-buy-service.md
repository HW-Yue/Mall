# group-buy-service 代码地图

## 启动与配置

- 启动模块：`group-buy-service/group-buy-service-app`
- 启动类：`com.yue.groupbuy.GroupBuyServiceApplication`
- 配置文件：
  - `group-buy-service/group-buy-service-app/src/main/resources/application-dev.yml`
  - `group-buy-service/group-buy-service-app/src/main/resources/application-test.yml`
  - `group-buy-service/group-buy-service-app/src/main/resources/application-prod.yml`
  - `group-buy-service/group-buy-service-app/src/main/resources/application-test-mock.yml`

## HTTP 入口

- 市场查询：
  - `group-buy-service/group-buy-service-trigger/src/main/java/com/yue/groupbuy/trigger/http/GroupBuyMarketController.java`
- 交易入口：
  - `group-buy-service/group-buy-service-trigger/src/main/java/com/yue/groupbuy/trigger/http/GroupBuyTradeController.java`
- API 契约：
  - `group-buy-service/group-buy-service-api/src/main/java/com/yue/groupbuy/api/`

## 核心业务

- 领域服务：
  - `group-buy-service/group-buy-service-domain/src/main/java/com/yue/groupbuy/domain/trade/service/`
- 拼团结算、组队、超时退款等核心逻辑从这里进入

## 下游调用与适配

- 调 `order-service` 的 Port：
  - `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/adapter/port/OrderServicePort.java`
- 超时和 Feign 相关配置：
  - `group-buy-service/group-buy-service-app/src/main/resources/application-*.yml`
  - 关键前缀：`app.agent.feign.order-service.*`

## MQ

### 消费者

- 支付成功后拼团结算：
  - `group-buy-service/group-buy-service-trigger/src/main/java/com/yue/groupbuy/trigger/listener/OrderPaidGroupBuyListener.java`
- 关单回退：
  - `OrderCloseGroupBuyListener.java`
- 退款回执：
  - `PayRefundGroupBuyListener.java`
- 超时退款：
  - `GroupBuyTimeoutRefundListener.java`

### 生产者

- 拼团退款 / 关单消息：
  - `group-buy-service/group-buy-service-infrastructure/src/main/java/com/yue/groupbuy/infrastructure/event/GroupBuyRefundMqProducer.java`
- 拼团超时定时消息：
  - `GroupBuyTimeoutRefundProducer.java`
- 拼团成功通知：
  - `GroupBuyEventPublisher.java`

## 什么时候先看这里

- 开团 / 参团下单
- 成团结算
- 拼团超时、关单、退款
- `group-buy-service -> order-service` 的 Port 适配

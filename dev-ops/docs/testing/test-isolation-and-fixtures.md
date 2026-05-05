# 单测隔离与测试基座

## 禁止连接真实依赖

不要让测试去连接真实：

- Nacos
- RocketMQ broker
- Sentinel dashboard
- Logstash
- 其他微服务

## 每个服务的统一结构

每个服务都按这套结构维护：

- `src/main/resources/application-test-mock.yml`
- `src/test/java/.../config/RocketMqMockTestConfig.java`
- `src/test/java/.../config/Abstract*ComponentTest.java`
- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`

## 规则

- `test-mock` profile 只给测试使用
- Feign 一律在测试类里 `@MockBean` 或 Mockito mock
- MQ 一律 mock，不连真实 broker
- repository 组件测试只连本服务测试库

## 现有测试基座

- `pay/pay-app/src/test/java/cn/bugstack/test/config/AbstractPayComponentTest.java`
- `order-service/order-service-app/src/test/java/com/yue/order/test/config/AbstractOrderServiceComponentTest.java`
- `group-buy-service/group-buy-service-app/src/test/java/com/yue/groupbuy/test/config/AbstractGroupBuyComponentTest.java`
- `seckill-service/seckill-service-app/src/test/java/com/yue/seckill/test/config/AbstractSeckillComponentTest.java`
- `mall/mall-app/src/test/java/com/yue/test/config/AbstractMallComponentTest.java`

## MQ mock 配置

- `pay/pay-app/src/test/java/cn/bugstack/test/config/RocketMqMockTestConfig.java`
- `order-service/order-service-app/src/test/java/com/yue/order/test/config/RocketMqMockTestConfig.java`
- `group-buy-service/group-buy-service-app/src/test/java/com/yue/groupbuy/test/config/RocketMqMockTestConfig.java`
- `seckill-service/seckill-service-app/src/test/java/com/yue/seckill/test/config/RocketMqMockTestConfig.java`
- `mall/mall-app/src/test/java/com/yue/test/config/RocketMqMockTestConfig.java`

## 相关入口

- 服务内自治单测策略：`service-standalone-test-strategy.md`
- 改代码后怎么改测试：`change-driven-test-rules.md`

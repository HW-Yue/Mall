# 单测执行方式

统一从各自 `app` 模块执行，`surefire` 已默认注入 `spring.profiles.active=test-mock`。

## 跑整服务测试

```bash
mvn -pl pay/pay-app -am test -DskipTests=false
mvn -pl order-service/order-service-app -am test -DskipTests=false
mvn -pl group-buy-service/group-buy-service-app -am test -DskipTests=false
mvn -pl seckill-service/seckill-service-app -am test -DskipTests=false
mvn -pl mall/mall-app -am test -DskipTests=false
```

## 跑单个测试类

```bash
mvn -pl order-service/order-service-app -am test -DskipTests=false -Dtest=OrderDomainServiceTest
mvn -pl seckill-service/seckill-service-app -am test -DskipTests=false -Dtest=SeckillTradeServiceImplTest
```

## 跑单个测试方法

```bash
mvn -pl pay/pay-app -am test -DskipTests=false -Dtest=AliPayControllerTest#payNotifyHandlesClosedOrderByRefunding
```

## 相关入口

- 服务内自治单测策略：`service-standalone-test-strategy.md`
- 提交前最低要求：`minimum-test-requirements.md`

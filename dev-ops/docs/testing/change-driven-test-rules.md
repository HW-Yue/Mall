# 改代码后怎么改测试

这份文档定义“代码改到哪里，测试就应该跟到哪里”的最低约定。

## 改 `domain service / state machine / rule chain`

- 同步修改或新增 `*DomainServiceTest`、`*StateMachineTest`、`*Rule*Test`、`*Calculator*Test`
- 不启 Spring，只用 Mockito / stub / fake object
- 覆盖成功路径、失败补偿路径、幂等、重复消息、非法状态迁移

## 改 `controller`

- 同步修改或新增 `*ControllerTest`
- 优先纯 Mockito controller 测试
- 断言参数校验、返回码映射、service 调用、异常映射
- 如果 controller 组装了跨服务请求 DTO，必须断言 DTO 字段

## 改 `listener / job`

- 同步修改或新增 `*ListenerTest`、`*JobTest`
- listener 直接调用 `onMessage(...)`
- job 直接调用 job 方法
- 覆盖正常消息、缺字段 / 非法消息、重复消息 / 幂等、下游异常

## 改 `MQ producer / transaction listener`

- 同步修改或新增 `*InfrastructureTest`、`*MqProducerTest`、`*TransactionListenerTest`
- 不连真实 RocketMQ
- 只 mock `RocketMQTemplate`
- 断言 `topic`、`payload`、`headers`
- 事务消息断言本地事务返回值：`COMMIT / ROLLBACK / UNKNOWN`

## 改 `Feign port / 外部 adapter`

- 同步修改或新增 `*PortTest`、`*InfrastructureTest`
- 不走真实 Nacos / 服务发现 / HTTP
- mock 下游 client
- 断言请求 DTO 组装、空响应、失败码映射、异常映射、失败补偿

## 改 `repository / DAO / MyBatis mapper`

- 同步修改或新增 `*RepositoryTest`、`*InfrastructureTest`
- 允许连本服务测试库
- 不跨服务读写别的服务库
- 至少覆盖新增 SQL 的读写主路径和关键状态分支

## 改 `profile / 配置装配 / app 启动相关`

- 必须同步检查 `application-test-mock.yml`
- 必须同步检查 `maven-surefire-plugin`
- 必须同步检查 `mockito-extensions/org.mockito.plugins.MockMaker`
- 必须同步检查本服务测试基类和 test config

## 改 `动态配置刷新 / 配置中心监听器 / 运行时线程池刷新`

- 同步修改或新增 `*RefresherTest`、`*PropertiesTest`
- 不连真实 Nacos / RocketMQ / Sentinel Dashboard
- 可以直接构造 `EnvironmentChangeEvent` 或手工调用启动钩子验证刷新逻辑
- 必须断言“刷新前”和“刷新后”的关键配置值，不只断言方法被调用
- 如果代码会改运行中的线程池、连接池、consumer 容器，必须同时断言外层配置对象和底层运行时对象都已变化

当前示例：

- `order-service/order-service-app/src/test/java/com/yue/order/config/RocketMqConsumerThreadPoolRefresherTest.java`
  断言 RocketMQ consumer `consumeThreadMin / consumeThreadMax` 与内部 `consumeExecutor core/max` 在 startup + refresh 两个阶段都发生变化
- `mall/mall-app/src/test/java/com/yue/config/HikariPoolDynamicRefresherTest.java`
  断言 Hikari 连接池 `maximumPoolSize / minimumIdle` 在刷新前后发生变化
- `group-buy-service/group-buy-service-app/src/test/java/com/yue/groupbuy/config/HikariPoolDynamicRefresherTest.java`
  断言 Hikari 连接池 `maximumPoolSize / minimumIdle / connectionTimeout` 在刷新前后发生变化
- `mall/mall-app/src/test/java/com/yue/config/DynamicTpRefreshTest.java`
  断言 `DynamicTp` 业务线程池和 `tomcatTp` 线程池在刷新前后 `corePoolSize / maximumPoolSize / keepAliveTime` 都发生变化
- `group-buy-service/group-buy-service-app/src/test/java/com/yue/groupbuy/config/AgentRuntimePropertiesTest.java`
  断言 `@RefreshScope` 运行时属性类在配置变更前后绑定出的业务开关、缓存 TTL、Feign 超时值都发生变化
- `pay/pay-app/src/test/java/cn/bugstack/config/DynamicTpRefreshTest.java`
  断言 `pay` 的 `DynamicTp` 业务线程池和 `tomcatTp` 线程池在刷新前后 `corePoolSize / maximumPoolSize / keepAliveTime` 都发生变化
- `springcloud-gateway/app/src/test/java/cn/bugstack/gateway/config/GatewaySentinelConfigTest.java`
  断言网关 Sentinel fallback 和 `gw-flow` 数据源配置符合约定
- `*/src/test/java/**/SentinelDatasourceConfigTest.java`
  断言业务服务的 Sentinel `flow / degrade / param-flow / system / authority` 5 类数据源 DataId 与 rule-type 配置符合约定

## 相关入口

- 服务内自治单测策略：`service-standalone-test-strategy.md`
- 单测计划：`service-unit-test-plan/README.md`

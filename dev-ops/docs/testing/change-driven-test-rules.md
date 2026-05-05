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

## 相关入口

- 服务内自治单测策略：`service-standalone-test-strategy.md`
- 单测计划：`service-unit-test-plan/README.md`

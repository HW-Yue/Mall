# 服务内自治单测设计

目标：每个服务单独执行自己的单测和轻量组件测试，不依赖其他服务启动，不改业务代码，不在生产代码里加测试分支。

## 设计原则

- 业务代码不加 `if (test)` 分支。
- 测试隔离只放在 `app` 模块的 `pom.xml`、`src/test/java`、`src/test/resources` 和 `application-test-mock.yml`。
- 同步依赖用 `@MockBean` 或 MockitoBean 替换，不走真实 Feign。
- 异步依赖只 mock `RocketMQTemplate`，listener 直接调用 `onMessage(...)`。
- repository 组件测试只连本服务测试库，不跨服务建库或联调。

## 每个服务的统一结构

- `src/main/resources/application-test-mock.yml`
- `src/test/java/.../config/RocketMqMockTestConfig.java`
- `src/test/java/.../config/Abstract*ComponentTest.java`
- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`

其中：

- `application-test-mock.yml` 负责关闭 Nacos、Sentinel、真实 MQ，并指向测试 MySQL/Redis。
- `RocketMqMockTestConfig` 在 `test-mock` profile 下提供 `RocketMQTemplate` mock bean。
- `Abstract*ComponentTest` 统一使用 `@SpringBootTest + @ActiveProfiles("test-mock")`。
- 具体 Feign 依赖不放在公共基类里，避免公共测试配置反向耦合业务模块；由具体测试类按需 `@MockBean`。

## Maven 入口规范

各服务 `app` 模块统一使用 surefire：

- 只匹配 `**/*Test.java`
- 默认注入 `spring.profiles.active=test-mock`
- 不忽略失败

这样直接执行下面命令即可在服务内自治运行：

```bash
mvn -pl pay/pay-app -am test -DskipTests=false
mvn -pl order-service/order-service-app -am test -DskipTests=false
mvn -pl group-buy-service/group-buy-service-app -am test -DskipTests=false
mvn -pl seckill-service/seckill-service-app -am test -DskipTests=false
mvn -pl mall/mall-app -am test -DskipTests=false
```

## 测试写法约定

纯单测：

- domain
- state machine
- calculator
- rule chain
- controller / listener 的纯 Mockito 测试

组件测试：

- 继承 `Abstract*ComponentTest`
- 只验证本服务 Spring 装配、repository 读写、事务边界
- Feign 依赖用 `@MockBean`

MQ 测试：

- producer 断言 `topic / payload / headers / transaction arg`
- listener 直接调用 `onMessage(...)`
- 必须覆盖成功、重复消息、下游异常、非法消息

动态配置测试：

- 不连真实配置中心，直接构造配置对象和刷新事件
- 对 `HikariPoolDynamicRefresher`、`RocketMqConsumerThreadPoolRefresher`、`@RefreshScope` 属性类这类代码，优先写纯单测
- 必须打印或断言刷新前后关键值，避免只验证“没报错”
- 如果刷新逻辑涉及运行中的执行器，至少断言：
  - 配置对象视角的值已变化
  - 底层线程池 / consumer / datasource 的运行时值也已变化

当前示例：

- `order-service/order-service-app/src/test/java/com/yue/order/config/RocketMqConsumerThreadPoolRefresherTest.java`
  通过真实 `DefaultMQPushConsumer` + `ConsumeMessageConcurrentlyService` 验证 RocketMQ 消费线程池在刷新前后确实变化，并输出前后参数
- `mall/mall-app/src/test/java/com/yue/config/HikariPoolDynamicRefresherTest.java`
  通过真实 `HikariDataSource` + `EnvironmentChangeEvent` 验证连接池参数在刷新前后确实变化，并输出前后参数
- `mall/mall-app/src/test/java/com/yue/config/DynamicTpRefreshTest.java`
  通过真实 `DtpExecutor` + `TomcatDtpAdapter` 验证业务线程池与 `tomcatTp` 在线刷新前后确实变化，并输出前后参数

Feign 测试：

- 业务层测试 DTO 组装、空响应、失败码、异常补偿
- port / adapter 层单独验证响应映射

## 对业务代码的约束

以下做法禁止引入：

- 在生产代码里新增测试专用开关分支
- 为了测试修改业务对外接口
- 在生产 bean 里内嵌 mock 行为
- 直接在业务类里 `new` 外部客户端导致无法注入替身

允许的最小可测性要求只有一条：外部依赖继续保持 Spring 注入。

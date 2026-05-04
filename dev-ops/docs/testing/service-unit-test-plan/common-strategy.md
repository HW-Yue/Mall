# Common Strategy

## Test Layers

### `pure unit test`

- 不启 Spring
- 直接测状态机、规则类、领域服务、工厂、过滤器
- 依赖全部使用 mock / stub

### `component test`

- 启 Spring
- 允许真实 MySQL + 真实 Redis
- Feign 与 MQ 必须替换
- 重点验证 service、repository、port 适配与参数组装

### `trigger test`

- 覆盖 HTTP controller、MQ listener、job
- 验证入参解析、分发、异常路径、幂等和返回体

## Feign Strategy

测试环境里的 Feign 统一按“外部依赖”处理，不走真实服务发现，也不依赖 Nacos 或下游服务实际可用。

### Default Rule

- 领域层与应用层的出站调用，不直接测真实 Feign Client
- 测试中替换为 mock bean、stub bean，或对上层 `Port` 做 mock
- 验证重点放在：
  - 请求 DTO 组装是否正确
  - 下游成功时本服务行为是否正确
  - 下游返回空值、失败码、异常时本服务补偿或回滚是否正确

### Recommended Injection Boundary

- `domain` 测试：mock `Port` 接口，不感知 Feign
- `component test`：保留 `Port` 实现，mock 其内部 Feign Client
- `controller test`：mock app service 或上层 service，不把 Feign 带入 MVC slice

### Known Feign Entry Points

- `mall` → `order-service`
- `group-buy-service` → `order-service`
- `seckill-service` → `order-service`
- `order-service` → `pay`、`mall`

## MQ Mock Strategy

测试环境里的 RocketMQ 统一不连真实 broker。

### Producer Side

- 所有 `RocketMQTemplate` 统一用 mock bean 替换
- 断言：
  - 调用的方法
  - topic
  - payload
  - header
  - 事务消息的本地事务分支

### Consumer Side

- 不启动真实 broker 拉取消息
- 直接调用 listener 的 `onMessage` / 处理方法
- 构造正常消息、空消息、非法消息、重复消息、下游异常消息

### Profile Convention

- 所有服务统一使用 `test-mock` 作为单测 profile 名称
- 所有服务统一新增 `application-test-mock.yml`
- 文件位置统一为各服务 app 模块的 `src/main/resources/application-test-mock.yml`
- 标准示例：`pay/pay-app/src/main/resources/application-test-mock.yml`
- profile 中不配置真实 `rocketmq.name-server`
- 对需要 `RocketMQTemplate` 的模块，在测试配置中显式注入 mock bean
- Feign 相关超时、URL、发现配置不依赖真实 Nacos 注册发现，测试中以 mock bean / stub bean 替代

## External Dependencies

- MySQL：真实测试库
- Redis：真实测试 Redis
- MQ：mock
- Feign：mock / stub
- Nacos / Sentinel / Logstash：不纳入单测依赖

## Naming Convention

- `*Test`：纯单测
- `*ComponentTest`：Spring 组件测试
- `*ControllerTest`：Controller
- `*ListenerTest`：MQ listener
- `*JobTest`：调度任务

## Common Acceptance

- MQ 发送点必须断言消息内容
- Feign 调用点必须断言请求参数与异常路径
- Listener 至少覆盖正常消费、非法消息、下游异常
- Controller 至少覆盖成功、参数非法、业务异常

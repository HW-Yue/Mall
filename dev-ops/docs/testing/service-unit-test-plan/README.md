# Service Unit Test Plan

## Summary

这套文档用于规划业务服务集的单测建设，范围包括：

- `mall`
- `pay`
- `order-service`
- `group-buy-service`
- `seckill-service`
- `springcloud-gateway`

统一约束：

- MySQL：继续使用现有测试表
- Redis：继续使用真实测试 Redis
- 单测 profile 文件统一命名为 `application-test-mock.yml`，位置放在各服务 app 模块的 `src/main/resources/`
- 参考格式：`pay/pay-app/src/main/resources/application-test-mock.yml`
- Feign：测试环境不走真实注册发现链路，统一替换为 mock bean 或 stub adapter
- MQ：统一不连真实 RocketMQ，发送端与消费端都走 mock 方案
- 现有 `test.md` 和 `dev-ops/full-flow-test/README.md` 继续承接集成/全链路，不算单测覆盖

## Documents

- [common-strategy.md](/home/yue/IdeaProjects/Nexus/dev-ops/docs/testing/service-unit-test-plan/common-strategy.md)
- [pay.md](/home/yue/IdeaProjects/Nexus/dev-ops/docs/testing/service-unit-test-plan/pay.md)
- [order-service.md](/home/yue/IdeaProjects/Nexus/dev-ops/docs/testing/service-unit-test-plan/order-service.md)
- [group-buy-service.md](/home/yue/IdeaProjects/Nexus/dev-ops/docs/testing/service-unit-test-plan/group-buy-service.md)
- [seckill-service.md](/home/yue/IdeaProjects/Nexus/dev-ops/docs/testing/service-unit-test-plan/seckill-service.md)
- [mall.md](/home/yue/IdeaProjects/Nexus/dev-ops/docs/testing/service-unit-test-plan/mall.md)
- [springcloud-gateway.md](/home/yue/IdeaProjects/Nexus/dev-ops/docs/testing/service-unit-test-plan/springcloud-gateway.md)

## Delivery Order

1. `pay`、`order-service`、`group-buy-service`
2. `seckill-service`、`mall`
3. `springcloud-gateway`
4. 统一补齐 repository 组件测试、异常分支、公共测试基类

## Acceptance

- 不允许单测直接依赖真实 RocketMQ NameServer
- 不允许把 Docker 全链路脚本算作单测覆盖
- 所有交易服务的 MQ 发送点必须断言消息内容，不只断言“调用过”
- 所有 Feign 出站调用必须验证请求参数、异常映射与失败回滚逻辑
- 状态机相关测试必须覆盖非法状态迁移

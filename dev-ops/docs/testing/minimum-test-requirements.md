# 提交前最低测试要求

## 基线要求

- 如果改动只在单一服务内，至少跑该服务 `app` 模块测试。
- 如果改动跨服务边界，除了受影响服务单测，还必须补跑集成 / 全链路测试。

## 交易主链路改动

如果改动涉及交易主链路，至少跑受影响的服务：

- 改 `mall -> order-service`：跑 `mall-app`、`order-service-app`
- 改 `group-buy-service -> order-service -> pay`：跑 `group-buy-service-app`、`order-service-app`、`pay-app`
- 改 `seckill-service -> order-service -> pay`：跑 `seckill-service-app`、`order-service-app`、`pay-app`
- 改公共 MQ topic 路由或退款/关单链路：跑 `pay-app`、`order-service-app` 和对应营销服务

## 跨服务改动的集成测试要求

以下类型的改动，不能只停留在单测，必须再做集成 / 全链路测试：

- 任意服务间 HTTP / Feign 调用链改动
- 网关路由、前端 API 路径映射改动
- MQ topic、生产者、消费者、事务消息、补偿链路改动
- 订单、拼团、秒杀、支付主链路改动
- Nacos、Docker、MySQL 初始化、环境地址、服务注册发现相关改动
- 业务库 SQL / 初始化 SQL 改动

集成测试入口：

- 文档：`dev-ops/full-flow-test/README.md`
- 脚本：`bash dev-ops/app/group-buy-full-flow-test.sh`

## 补充说明

- 更完整的服务内自治单测说明见 `service-standalone-test-strategy.md`
- 更细的服务级单测建设范围见 `service-unit-test-plan/README.md`
- 如果改了 `dev-ops/mysql/sql/*.sql` 里的业务表结构或初始化数据，必须同步检查并更新 `dev-ops/mysql/sql/test/*.sql`

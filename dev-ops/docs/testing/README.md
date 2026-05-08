# 测试文档

## 文档导航

- [服务内自治单测策略](./service-standalone-test-strategy.md)
- [改代码后怎么改测试](./change-driven-test-rules.md)
- [单测隔离与测试基座](./test-isolation-and-fixtures.md)
- [单测执行方式](./test-execution.md)
- [提交前最低测试要求](./minimum-test-requirements.md)
- [Docker Mock 联调](./docker-test-mock-compose.md)
- [服务单测计划](./service-unit-test-plan/README.md)
- [集成 / 全链路测试](../../full-flow-test/README.md)

## 相关图表

- `../diagrams/trade/core-trade-flow.svg`
- `../diagrams/trade/core-trade-test-matrix.svg`

## 维护规则

- 单测策略、执行方式、测试 profile 规范统一写在这里
- 代码改动对应的测试补齐规则统一写在这里
- 跨服务改动的集成 / 全链路测试入口也统一从这里索引
- 图表不再混放在 `testing/` 根目录，统一收口到 `dev-ops/docs/diagrams/`

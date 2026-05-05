# Dev-Ops Docs

`dev-ops/docs/` 是仓库里所有正式文档、图表和测试说明的统一入口。

## 文档分层

- [API 文档](./api/README.md)
  - 服务对外 HTTP 接口
  - 网关路由和前端入口映射
- [代码地图](./code-map/README.md)
  - 各服务从哪里开始看代码
  - Controller、Domain、Port、MQ、配置位置
- [MQ 文档](./mq/README.md)
  - Topic 清单
  - 生产者、消费者、消费者组映射
- [监控文档](./monitoring/README.md)
  - Prometheus、Alertmanager、Sentinel、DynamicTP、ELK、SkyWalking
  - 启动顺序与指标暴露约定
- [测试文档](./testing/README.md)
  - 服务单测策略
  - 单测计划
- 图表文档
  - `./diagrams/mq/`：MQ 路由图
  - `./diagrams/trade/`：交易链路图、测试矩阵图

## 维护规则

- 新增正式文档统一放在 `dev-ops/docs/`
- 接口说明写入 `dev-ops/docs/api/`
- 代码入口与关键文件说明写入 `dev-ops/docs/code-map/`
- MQ 说明写入 `dev-ops/docs/mq/`
- 监控说明写入 `dev-ops/docs/monitoring/`
- 测试方案写入 `dev-ops/docs/testing/`
- SVG / PNG / Mermaid 图统一写入 `dev-ops/docs/diagrams/`
- 不再把正式接口清单和流程图散落到根目录 `docs/` 或服务私有目录

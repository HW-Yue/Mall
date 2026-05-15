# 告警归因知识库

`ops-agent-spring-ai` 运行时会从模块内快照 `src/main/resources/ops-catalog/catalog.json` 加载一份只读知识库，用来把告警里的 `application`、`resource`、`topic`、`consumerGroup`、`table`、`db`、`pool` 归因到具体服务，也给 Catalog Skill 提供“先列对象、再归因”的兜底入口。

## 结构

- `serviceAliases`
  - 服务别名到标准服务名的映射，如 `mall -> mall-service`
- `serviceProfiles`
  - 服务静态画像，包含 `application`、`composeService`、`containerName`，以及 Nacos 配置入口 `configEntries`
- `resourceOwners`
  - 服务内 HTTP URI 到 owning service 的映射
- `topics`
  - topic 到生产者、消费者、consumer group 的映射
- `consumerGroups`
  - consumer group 到消费者服务的映射
- `tableOwners`
  - MySQL 表名到 owning service 列表的映射
- `databaseOwners`
  - 库名/schema 到 owning service 列表的映射
- `poolOwners`
  - Hikari pool name 到 owning service 的映射

## 事实来源

- MQ：`dev-ops/docs/mq/README.md` 与 `topics/*.md`
- HTTP 资源：`dev-ops/docs/api/services/*.md` 与各服务 Controller `@RequestMapping`
- MySQL：`dev-ops/mysql/sql/*.sql` 与 `dev-ops/mysql/sql/test/*.sql`
- 连接池：仓库约定文档与各服务配置

## 更新规则

- 新增或修改 Topic / consumer group：
  - 先改业务服务代码与 `dev-ops/docs/mq/`
  - 再同步更新 `catalog.json`
- 新增或修改业务表：
  - 先改 `dev-ops/mysql/sql/*.sql`
  - 再同步更新 `catalog.json`
- 修改对外或服务内 URI：
  - 先改 Controller / gateway / 前端配置
  - 再同步更新 `catalog.json`

## 运行时使用

- `AlertSignalResolver`：从 labels / annotations / 文本中提取线索
- `AlertEnrichmentService`：结合 catalog 输出 `primaryService`、候选服务和归因证据
- `SopDispatcher` / `SopStepRunner`：消费 enrichment 结果做确定性多步 SOP 编排
- `Catalog Skill`：
  - `catalog_list_services` 先列出当前已知服务名
  - `catalog_list_topics` 先列出当前已知 Topic
  - `catalog_describe_service` 再查看某个服务对应的 application、容器名、configEntries/configDataIds、topic、database、pool 等静态拓扑

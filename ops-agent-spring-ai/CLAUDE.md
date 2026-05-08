> 注意：如果修改本文件内容，必须同步更新 `AGENTS.md`；如果修改 `AGENTS.md`，也必须同步更新本文件。

## 模块概览

`ops-agent-spring-ai` 是 Nexus 单仓中的运维 Agent 模块。

基于 Spring Boot / Spring AI Alibaba 构建，负责接收告警、匹配 SOP 规则、运行 ReAct Agent、委托领域子 Agent，并执行受控的操作工具。

**运行时信息：**
- 应用名：`ops-agent-spring-ai`
- 端口：`8096`
- Java：`21`
- 主技术栈：Spring Boot、Spring AI Alibaba、Nacos、Prometheus、Elasticsearch、Docker Java、RocketMQ tools、Wrench rule chain

## 职责矩阵

| 领域 | 职责 |
|------|------|
| 告警接收（Alert intake） | 接收 Alertmanager webhook，将其路由到运行会话 |
| SOP 路由（SOP routing） | 按告警文本/标签匹配 YAML SOP 规则，配置处可启用 AI 兜底 |
| Parent Agent | ReAct 编排与领域子 Agent 委托 |
| Sub Agents | 每个运维领域一个独立的 ReAct 循环 |
| Skill Registry | 注册领域 Skill 并路由具体的工具调用 |
| 审批（Approval） | 将 risky 写操作暂存在内存审批队列中 |
| 可观测性（Observability） | 输出运行事件、日志、Prometheus 指标和链路追踪数据 |
| Web 控制台 | 提供聊天、工具、审批、运行时间线等静态演示页面 |

## 包结构

```text
src/main/java/com/yue/opsagent/springai/
├── trigger/          # HTTP Controller 与请求 DTO
├── service/          # 路由、SOP、审批、运行状态、编排等 Service
├── agent/            # Parent ReAct Agent、子 Agent 委托、ReAct 解析/执行器
├── skill/            # 领域 Skill 注册表、工具箱、执行规则链
├── domain/           # 告警、审批、运维运行等领域对象
└── infrastructure/   # 配置、可观测性、外部客户端
```

静态演示页面位于 `dev-ops/frontend/`，默认以前后端分离方式部署。

SOP 规则位于 `src/main/resources/sop/rules/`。

## Skill 注册机制

本项目**不**通过扫描 `SKILL.md` 文件来注册 Skill。

Skill 以 Spring Bean 形式注册：

1. 领域注册表实现 `com.yue.opsagent.springai.skill.api.OpsSkillRegistry` 接口。
2. 注册表标注 `@Component`。
3. `MasterRegistry` 通过 Spring 注入接收 `Collection<OpsSkillRegistry>`。
4. `MasterRegistry` 按 `OpsSkillRegistry.name()` 索引注册表。
5. 工具执行在到达注册表 `execute(...)` 之前，先经过 Wrench 规则链。

核心接口：

```java
public interface OpsSkillRegistry {
    String name();
    String description();
    String promptFragment();
    Set<String> toolNames();
    ToolResult execute(String toolName, Map<String, Object> args);
    default boolean requiresApproval(String toolName) { return false; }
}
```

当前领域 Skill 列表：

| Skill id | Parent tool | Registry |
|----------|-------------|----------|
| `docker_ops` | `docker_skill` | `DockerSkillRegistry` |
| `mysql_inspect` | `mysql_skill` | `MysqlSkillRegistry` |
| `rocketmq_inspect` | `rocketmq_skill` | `RocketMqSkillRegistry` |
| `metrics_ops` | `prometheus_skill` | `MetricsSkillRegistry` |
| `elasticsearch_ops` | `elasticsearch_skill` | `ElasticsearchSkillRegistry` |
| `redis_inspect` | `redis_skill` | `RedisSkillRegistry` |
| `nacos_config` | `nacos_skill` | `NacosSkillRegistry` |

Parent tool 名称映射在 `agent/sub/ISubAgent.java` 中定义。

## 新增或修改 Skill

新增 Skill 时按以下步骤操作：

1. 在 `skill/<domain>/` 下新增包。
2. 实现一个带 `@Component` 的 `*SkillRegistry`。
3. 返回稳定的 `name()` id；如无明确需求不要重命名已有 id，否则需同步更新 SOP 规则和子 Agent 映射。
4. 实现 `description()`、`promptFragment()`、`toolMenuBrief()` 和 `toolSpecification(...)`，使 Parent 和 Sub-agent 能渐进式地选择工具。
5. `toolNames()` 返回允许调用的工具白名单。
6. `execute(...)` 委托给 toolkit 类实现，返回 `ToolResult`，不要返回原始 map。
7. 如有工具会修改状态，重写 `requiresApproval(toolName)` 并将操作接入审批流程。
8. 如 Parent ReAct Agent 需要委托给该 Skill，添加或更新 `ISubAgent` 实现。
9. 如告警工作流需使用该 Skill，更新 SOP YAML 规则。
10. 补充针对性测试：注册表路由、工具名称、审批行为、SOP 集成等。

正常工具执行不要绕过 `MasterRegistry`。它提供了 Skill 解析、白名单检查、审批检查、运行取消检查、链路追踪和结果记录等能力。

## 工具执行链

工具调用路径：

```text
HTTP /api/v1/tools/execute
  -> ToolsController
  -> MasterRegistry.execute(skill, tool, args)
  -> Wrench 规则链
  -> 具体 OpsSkillRegistry.execute(tool, args)
  -> Toolkit
  -> ToolResult
```

执行规则链在 `ToolExecutionRuleFilterFactory` 中组装。

当前过滤器：

- `SkillResolveRuleFilter`
- `ToolWhitelistRuleFilter`
- `ToolApprovalRuleFilter`
- `RunCancelRuleFilter`
- `ToolTraceStartRuleFilter`
- `ToolExecuteRuleFilter`
- `ToolResultRecordRuleFilter`

仅当行为是**横切关注点**时才放入过滤器；领域特定逻辑应放在注册表或 toolkit 中。

## Agent 流程

Parent ReAct 流程：

```text
ChatController / OpsRouteService
  -> ParentReactAgent
  -> AgentToolRegistry
  -> ISubAgent
  -> AbstractISubReactAgent
  -> 通过 MasterRegistry 调用领域工具
```

子 Agent 必须保持单一技术域，并向 Parent Agent 返回简洁的中文摘要。

子 Agent prompt 要求输出单一合法 JSON action：

- `{"action":"CALL_TOOL","tool":"<toolName>","args":{...}}`
- `{"action":"FINAL","answer":"<summary>"}`

当工具返回空、未找到、未知、404、连接失败或空指标时，应将这些视为证据，不要发散到无关工具上继续搜索。

## SOP 规则

SOP YAML 文件位于 `src/main/resources/sop/rules/`。

规则字段通常引用以下 skill id：

- `metrics_ops`
- `mysql_inspect`
- `redis_inspect`
- `rocketmq_inspect`
- `nacos_config`

修改 skill id、工具名或期望参数时，必须同步更新所有引用它的 SOP 规则和测试。

## 审批规则

只读工具直接执行。

状态变更工具必须经过审批。典型示例：

```java
NacosSkillRegistry.requiresApproval("nacos_publish_config") == true
```

已审批的操作通过现有审批服务路径执行。不要新增跳过审批的直接写端点。

## API 概览

| Method | Path | 用途 |
|--------|------|------|
| `POST` | `/api/v1/chat/stream` | SSE 聊天端点 |
| `POST` | `/api/v1/chat/react` | Parent ReAct 调试端点 |
| `POST` | `/api/v1/tools/execute` | 直接 Skill/工具执行 |
| `POST` | `/api/v1/alert/receive` | Alertmanager webhook |
| `GET` | `/api/v1/approvals/pending` | 查询待审批列表 |
| `POST` | `/api/v1/approvals/{id}/approve` | 审批通过 |
| `POST` | `/api/v1/approvals/{id}/reject` | 审批拒绝 |
| `POST` | `/api/v1/ops/route-text` | 路由纯文本事件 |
| `GET` | `/api/v1/ops/runs/{runId}` | 查询运行状态 |
| `GET` | `/api/v1/ops/runs/{runId}/events` | 运行事件流 |
| `POST` | `/api/v1/ops/runs/{runId}/cancel` | 取消运行 |

## 配置与运行

本地配置模板：

```bash
cp application-local.example.yml application-local.yml
```

`application-local.yml` 已被 git 忽略，用于存放本地端点和密钥。

DashScope key：

```bash
export DASHSCOPE_API_KEY=your-key
```

本地运行：

```bash
mvn spring-boot:run
```

构建：

```bash
mvn clean package -DskipTests
```

运行测试：

```bash
mvn test
```

Docker test compose 约定：

- 当前 Docker 联调/测试默认以 `dev-ops/docker-compose-apps-test.yml` 为准。
- 该 compose 下所有服务统一使用各自的 `application-test.yml`，不要按 `application-dev.yml` 推断容器内行为。
- `test` / Docker compose 场景下，Nacos 地址应与其他服务保持一致，使用 `server-addr: nacos:8848`。
- `test` / Docker compose 场景下，不要显式配置 `spring.cloud.nacos.discovery.ip`；注册 IP 应交给容器网络自动探测。
- 在 Docker test 配置里把 `spring.cloud.nacos.discovery.ip` 写成 `127.0.0.1` 或宿主机固定 IP，视为错误配置，会导致网关或其他容器通过 Nacos 发现到不可达实例。
- `ops-agent-spring-ai` 在 Docker test 下必须注册为服务名 `ops-agent-spring-ai`，并与 gateway 路由目标保持一致。

## 关键文件路径

| 用途 | 路径 |
|---------|------|
| 项目总览 | `README.md` |
| DevOps 总入口 | `dev-ops/README.md` |
| 架构文档 | `dev-ops/docs/ops-agent/architecture.md` |
| API 文档 | `dev-ops/docs/ops-agent/api.md` |
| Skill 契约 | `src/main/java/com/yue/opsagent/springai/skill/api/OpsSkillRegistry.java` |
| Skill 聚合器 | `src/main/java/com/yue/opsagent/springai/skill/registry/MasterRegistry.java` |
| 工具规则链工厂 | `src/main/java/com/yue/opsagent/springai/skill/registry/ToolExecutionRuleFilterFactory.java` |
| Parent 工具注册表 | `src/main/java/com/yue/opsagent/springai/agent/registry/AgentToolRegistry.java` |
| 子 Agent 契约 | `src/main/java/com/yue/opsagent/springai/agent/sub/ISubAgent.java` |
| 子 Agent 基类 | `src/main/java/com/yue/opsagent/springai/agent/sub/AbstractISubReactAgent.java` |
| SOP 规则 | `src/main/resources/sop/rules/` |
| Web 控制台 | `dev-ops/frontend/` |

## 开发规则

- 如无显式要求，保留现有 skill id 和工具名，不做破坏性变更。
- Skill 行为保持领域专用；横切行为放在规则过滤器中。
- Risky 操作必须走审批。
- Registry prompt fragment 保持简短、面向操作；详细行为尽可能放在 `toolSpecification(...)` 中。
- Toolkit 和 Registry 返回结构化的 `ToolResult`。
- 引入网络调用或 shell 执行路径时，必须有明确的边界和测试覆盖。
- 修改公共 HTTP API 路径或报文时，同步更新 `dev-ops/docs/ops-agent/api.md` 和相关静态页面。
- 修改 SOP 工具引用时，同步更新对应 Registry 测试或补充覆盖。
- 优先编写聚焦的单元测试：注册表路由、ReAct 解析、规则链行为、SOP 执行等。

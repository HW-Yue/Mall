# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Goal

`ops-agent` is a Nacos operations agent powered by the AgentScope Java framework and AG-UI protocol. It lets users manage Nacos configuration and service discovery through natural language via a chat UI. The next major feature is integrating Prometheus metrics so the agent can autonomously detect anomalies and trigger Nacos dynamic config changes in response.

## Build & Run

```bash
# Build (Java 21 required)
mvn clean package -DskipTests

# Run locally (requires Nacos at localhost:8848)
mvn spring-boot:run

# Run single test
mvn test -Dtest=ClassName#methodName
```

**Environment variable**: `DASHSCOPE_API_KEY` — the LLM API key (falls back to the hardcoded value in `application.yml` if unset).

The app starts on port **8098**. The frontend has been moved out of Spring Boot's classpath — it now lives in a standalone [`devops/`](./devops/) directory and is **opened directly via `file://`**:

- `devops/index.html` — **single-page console** with top tabs switching between:
  - **对话（chat）** — AG-UI stream to `NacosManagerAgent` (future: MCP + Skill agent)
  - **审批收件箱（inbox）** — Prometheus-triggered Nacos change approvals, IDEA-style side-by-side diff, SSE live updates
- Tabs are deep-linkable via hash: `index.html#chat` / `index.html#inbox`. The inbox tab shows a live PENDING-count badge; the API chip in the top-right reflects SSE connection state.

`devops/config.js` injects `window.API_BASE` (default `http://localhost:8098`, overridable via `?api=...` query param or `localStorage`). Cross-origin (including `Origin: null` from `file://`) is enabled by [`CorsConfig`](./src/main/java/com/yue/opsagent/config/CorsConfig.java) for `/api/**` and `/agui/**`. See [`devops/README.md`](./devops/README.md) for the layout and module map.

## Architecture

The project is a **single Spring Boot module** (not DDD multi-module). It follows a hexagonal/ports-and-adapters style within one package tree:

```
com.yue.opsagent
├── agent/          # Domain agent assembly — NacosManagerAgent (Nacos tools, chat + Nacos write strategies)
│                   # 及 DiagnoseAgent (ES MCP 子 Agent，给 NotifyOnly 域做异步诊断)
├── adapter/
│   ├── tool/        # AgentScope @Tool adapters — delegate to domain ports, never touch Nacos SDK directly
│   ├── sdk/         # Nacos SDK implementations of domain ports
│   ├── inbox/       # InMemoryApprovalInbox (storage.type=memory, debug-only fallback)
│   ├── persistence/ # MybatisApprovalInbox + Mapper XML (storage.type=mysql, default)
│   ├── strategy/    # AlertStrategy implementations (sentinel / dtp / notify / ...)
│   ├── handler/     # AlertHandlerPort implementations (logging + agent triggering)
│   └── hook/        # AgentScope hooks: ApprovalInterceptorHook, AlertContextHolder
├── config/         # Spring @Configuration: Nacos beans, AG-UI registry, model/prompt/approval properties
├── controller/     # REST endpoints: /api/agent/chat, /api/v1/approvals, /api/v1/alerts
└── domain/port/    # Interfaces: NacosConfigPort, NacosNamingPort, ApprovalInboxPort, AlertHandlerPort
```

**Call chain (AG-UI path)**:
```
Browser (devops/index.html #chat tab + js/agui-client.js, file://)
  → POST ${API_BASE}/agui/run  (AG-UI SSE stream, CORS enabled)
    → AguiAgentConfig registry → NacosManagerAgent.createAgent()
      → ReActAgent (AgentScope, ReAct loop, max 10 iters)
        → NacosConfigTool / NacosNamingTool  (@Tool adapters)
          → NacosConfigPort / NacosNamingPort  (domain interfaces)
            → NacosConfigSdkAdapter / NacosNamingSdkAdapter  (Nacos Java SDK)
```

**REST fallback path**: `POST /api/agent/chat` — useful for testing without a browser.

## Key Constraints

- **Tool classes must not import Nacos SDK** — only the `sdk/` adapters may do so. Keep `@Tool` classes thin: annotation + param conversion + port delegation only.
- **Agent is stateless per request**: `createAgent()` is a factory that builds a new `ReActAgent` with fresh `InMemoryMemory` on each call. Tool Spring beans (SDK adapters, ports) are singletons and shared.
- **AG-UI agent ID is `"nacos"`** — registered in `AguiAgentConfig`, referenced by `application.yml` (`default-agent-id: nacos`) and the frontend (`/agui/run`).

## Configuration Reference (`application.yml`)

| Key | Purpose |
|-----|---------|
| `nacos.server.addr` | Nacos server address |
| `ops-agent.model.provider` | `dashscope` (default) or `openai` |
| `ops-agent.model.api-key` | LLM API key (`DASHSCOPE_API_KEY` env var takes precedence) |
| `ops-agent.model.model-name` | Model name, e.g. `qwen-max` |
| `ops-agent.model.base-url` | Optional custom base URL |
| `ops-agent.system-prompt` | System prompt injected into every agent session |
| `ops-agent.approval.storage.type` | `mysql` (default) or `memory` — inbox backend |
| `spring.datasource.*` | MySQL 连接（`ops_agent_db`，复用 mall 的 localhost:13306 实例） |
| `mybatis.mapper-locations` | `classpath:/mybatis/mapper/*.xml`，和 mall 风格一致 |
| `agentscope.agui.path-prefix` | AG-UI endpoint prefix (`/agui`) |

## Persistence (Approval tasks → MySQL)

All `ApprovalTask` records are persisted in **MySQL** (独立库 `ops_agent_db`，复用 mall
的 MySQL 实例 `localhost:13306`)。建表脚本在
[`mall/docs/dev-ops/mysql/sql/ops_agent.sql`](../mall/docs/dev-ops/mysql/sql/ops_agent.sql)，
执行前确保 mall 的 `docker-compose-environment.yml` 已经把 MySQL 跑起来。

- **Storage port**: [`ApprovalInboxPort`](src/main/java/com/yue/opsagent/domain/port/ApprovalInboxPort.java)
- **Mysql 实现（默认）**: `adapter/persistence/approval/MybatisApprovalInbox.java`
  - MyBatis Mapper: `ApprovalTaskMapper.java` + `resources/mybatis/mapper/approval_task_mapper.xml`
  - `alert` 字段通过 `AlertEventJsonTypeHandler` 做 JSON ↔ `AlertEvent` 的双向映射
  - SSE 广播仍然走 `Sinks.Many`，启动时 `ApprovalController.stream()` 会先用 `inbox.list(null)` 从 DB 拉初始快照，客户端重连/重启都不会丢任务
  - `expireStaleTasks` 从定时扫 Map 变成：`selectExpiredPendingIds` → `expirePendingBefore` (批量 UPDATE) → `findByIds` → 逐条 emit
- **降级实现**: `adapter/inbox/InMemoryApprovalInbox.java`，当 `ops-agent.approval.storage.type=memory` 时启用，仅用于本地调试 / MySQL 不可用的临时兜底。

表结构（`approval_task`）：

| 列 | 类型 | 说明 |
|----|------|------|
| `id` | VARCHAR(64) PK | task UUID |
| `alert_key` | VARCHAR(255) | 告警聚合键 `alertname|app|resource` |
| `alert_json` | JSON | 完整 `AlertEvent`（Jackson 序列化，带 JavaTimeModule） |
| `domain` | VARCHAR(32) | `sentinel` / `dtp` / `notify` / `manual` |
| `tool_name` | VARCHAR(64) | 被拦截的 tool 名（`publishConfig` 等） |
| `data_id` / `group_name` | VARCHAR | Nacos dataId / group（字段名避开保留字 `group`） |
| `content_before` / `content_after` | MEDIUMTEXT | 变更前后完整内容，供并发校验和 diff |
| `reasoning` / `error_message` | TEXT | Agent 理由 / 失败原因 |
| `status` | VARCHAR(16) | `PENDING` / `APPROVED` / `REJECTED` / `APPLIED` / `FAILED` / `EXPIRED` |
| `created_at` / `decided_at` | DATETIME(3) | 时间戳（毫秒） |

索引：`(status, created_at)` 供 inbox 列表 + expire 扫描；`(alert_key, created_at)` 供节流 / 审计。

## Adding New Tools (Prometheus / other integrations)

1. Define a port interface under `domain/port/` (e.g. `PrometheusPort`).
2. Implement it under `adapter/sdk/` using the target SDK/HTTP client.
3. Create an `adapter/tool/` class with `@Tool`-annotated methods that delegate to the port.
4. Register the new tool in `NacosManagerAgent.createAgent()` via `toolkit.registerTool(...)`.
5. Update `ops-agent.system-prompt` in `application.yml` to describe the new capability.

## Alert Handling Architecture (Strategy Pattern)

Prometheus webhook alerts flow through a **strategy-based router**. Each business
domain (`sentinel`, `dtp`, `notify`, and — in the future — `mysql`, `redis`,
`rocketmq`, ...) gets its own `AlertStrategy` implementation with its own system
prompt, Nacos dataId resolver, and Agent identity.

```
Alertmanager webhook
  → AlertReceiveController
    → LoggingAlertHandler            (@Order 10, structured logs)
    → AgentTriggeringAlertHandler    (@Order 20, throttle + router)
        → List<AlertStrategy> sorted by order()
            → first supports(event) wins → dispatch(event, alertKey)
                (sentinel / dtp: build prompt → ReActAgent.call → publishConfig tool
                 → ToolSuspendException → ApprovalInbox SSE → human approve → Nacos)
                (notify: write ApprovalTask directly, no agent, no Nacos)
```

Key classes:

| Class | Role |
|-------|------|
| `adapter/strategy/AlertStrategy` | Interface: `domain()` / `order()` / `supports()` / `dispatch()` |
| `adapter/strategy/AbstractNacosWriteStrategy` | Base class (Nacos Write domains): "fetch config → build prompt → call Agent → inbox". Hook `requireApplicationLabel()` for domains without `application` label (e.g. Hikari uses `pool`). |
| `adapter/strategy/AbstractNotifyOnlyStrategy` | Base class (NotifyOnly domains): whitelist → write `ApprovalTask` directly, no agent, no Nacos |
| `adapter/strategy/AbstractDiagnoseAgentStrategy` | Base class (NotifyOnly + Diagnose): 先落 PENDING 任务占位 reasoning，再异步调 `DiagnoseAgent`（ES MCP 子 Agent）产出四段式诊断文本，通过 `ApprovalInboxPort.updateReasoning` 回填 |
| `adapter/strategy/SentinelFlowRuleStrategy` | `order=10`, `category=sentinel` → `${app}-flow-rules.json` @ `SENTINEL_GROUP` |
| `adapter/strategy/DynamicTpStrategy` | `order=20`, `category=dynamictp` → `${app}-dtp-${profile}.yml` @ `DEFAULT_GROUP` |
| `adapter/strategy/MySqlTuningStrategy` | `order=30`, `category=mysql` (application=shared) → `shared-mysql-tuning.yml` @ `DEFAULT_GROUP` |
| `adapter/strategy/HikariTuningStrategy` | `order=35`, `category=hikari` → pool-name 反查 `ops-agent.mapping.pool-name-to-app` → `${app}-datasource-dev.yml` @ `DEFAULT_GROUP` |
| `adapter/strategy/RedisNotifyStrategy` | `order=40`, `category=redis` → **继承 `AbstractDiagnoseAgentStrategy`**：借 `DiagnoseAgent` 调 ES MCP 查 `nexus-*` 索引产出根因假设 / 责任服务 / 排查动作 / 风险等级四段结论 |
| `adapter/strategy/RocketMqNotifyStrategy` | `order=50`, `category=rocketmq` → NotifyOnly（Broker / 消费堆积 / DLQ） |
| `adapter/strategy/NotifyOnlyStrategy` | `order=100` fallback, writes inbox only, `ApprovalService.approve` short-circuits `domain=notify/redis/rocketmq` to `APPLIED` |
| `agent/DiagnoseAgent` | 诊断子 Agent 工厂，启动时通过 `McpClientBuilder.streamableHttpTransport` 握手 `docker.elastic.co/mcp/elasticsearch`，缓存 `Toolkit` 供每次 `createAgent(sysPrompt, agentName)` 复用；MCP 未就绪时 `isReady()` 返回 false 触发降级 |
| `config/McpProperties` | 绑定 `ops-agent.mcp.elasticsearch.{enabled,url,timeout,initTimeout}`，控制是否拉起 ES MCP 子 Agent |
| `config/MappingProperties` | 绑定 `ops-agent.mapping.pool-name-to-app` / `client-name-to-app`，供 Hikari / Redis 策略反查 |

Config lives in `application.yml`:

- `ops-agent.prompts.<domain>` — per-domain system prompt (map)
- `ops-agent.approval.strategies.<domain>` — per-domain `alerts` whitelist + `group` + `data-id-template` + free-form `extra` map

### Adding a new alert domain (e.g. MySQL exporter)

**Three steps**, no framework changes:

1. **Add YAML config** in `application.yml`:
   ```yaml
   ops-agent:
     prompts:
       mysql: |
         你是 MySQL DBA ...
     approval:
       strategies:
         mysql:
           group: DEFAULT_GROUP
           data-id-template: "${app}-mysql.yml"
           alerts: [ MysqlConnectionsHigh, MysqlSlowQueryRateHigh ]
   ```

2. **Create a new strategy class** extending `AbstractNacosWriteStrategy`:
   ```java
   @Component
   public class MySqlStrategy extends AbstractNacosWriteStrategy {
       public MySqlStrategy(ApprovalProperties a, AgentSystemPromptProperties p,
                            NacosConfigPort c, NacosManagerAgent f) {
           super(a, p, c, f);
       }
       @Override public String domain() { return "mysql"; }
       @Override public int order() { return 30; }
       @Override protected boolean matches(AlertEvent e) { return "mysql".equalsIgnoreCase(e.category()); }
       @Override protected String resolveDataId(AlertEvent e, StrategyProps p) {
           return p.getDataIdTemplate().replace("${app}", e.application());
       }
       @Override protected String buildPromptBody(AlertEvent e, String alertKey,
                                                  String dataId, String group, String current) {
           return "...领域专属 prompt...";
       }
   }
   ```
   Spring will auto-discover and inject it into `AgentTriggeringAlertHandler` via `List<AlertStrategy>`.

3. **Add a CSS line** in `devops/css/app.css` for the domain badge color (look
   for the `/* 策略域 */` block, several stub lines are already commented out):
   ```css
   .badge.domain.mysql { background: #fef9c3; color: #a16207; }
   ```
   (Unknown domains gracefully fall back to the neutral `.badge.domain` gray.)

That's it. No changes to `AgentTriggeringAlertHandler`, `ApprovalTask`,
`ApprovalService`, `NacosConfigTool`, or the inbox JS.

For domains that **only need to notify a human** (no Nacos config to edit, e.g.
`ServiceDown`, `JvmGcPauseHigh`): append the alertname to
`ops-agent.approval.strategies.notify.alerts`. `NotifyOnlyStrategy` (the
`domain=notify`, `order=100` fallback) picks it up automatically.

If you need a **dedicated NotifyOnly domain** (e.g. RocketMQ / Redis that need
richer reason text or their own badge color), you have **two sub-flavors**:

- **Plain NotifyOnly** (reason 文本纯粹拼字符串，不调 LLM)：extend
  `AbstractNotifyOnlyStrategy`，覆盖 `domain()` / `order()` / `matches()` /
  `buildNotifyMessage()`。参考 `RocketMqNotifyStrategy`。
- **NotifyOnly + Diagnose**（借 ES MCP 子 Agent 异步产出诊断）：extend
  `AbstractDiagnoseAgentStrategy`，覆盖 `domain()` / `order()` / `matches()` /
  `buildDiagnosePrompt(event, alertKey)`；在 `ops-agent.prompts.<domain>-diagnose`
  里写对应的 system prompt（约束工具使用 + 输出格式）。参考 `RedisNotifyStrategy`。
  需要 `ops-agent.mcp.elasticsearch.enabled=true` 且 `elasticsearch-mcp` 容器已拉起；
  否则策略自动降级把 `[诊断失败] ES MCP 未就绪` 写到 reasoning。

### ES MCP 诊断子 Agent

`DiagnoseAgent` 是独立于 `NacosManagerAgent` 的诊断画像，工具链只挂
Elasticsearch MCP Server（`docker.elastic.co/mcp/elasticsearch` streamable-HTTP
模式，容器 `:8080` 映射到宿主机 `:8085`，端点 `/mcp`）。暴露的只读工具：
`list_indices` / `get_mappings` / `search` / `esql` / `get_shards`。

- **启动**：`@EventListener(ApplicationReadyEvent)` 里通过
  `McpClientBuilder.streamableHttpTransport(url).buildAsync()` 异步握手 + `Toolkit.registerMcpClient`，
  完成后把 `Toolkit` 缓存在 `volatile` 字段；每次 `createAgent` 复用同一个 `Toolkit`，
  不重复拉工具列表。
- **降级**：MCP 未启用 / 握手超时 / SSE 断开时 `isReady()` 返回 false，策略
  `AbstractDiagnoseAgentStrategy` 自动给 inbox 写 `[诊断失败] ES MCP 未就绪`，
  主告警链路不阻塞。
- **Docker Compose**：[`mall/docs/dev-ops/docker-compose-mcp.yml`](../mall/docs/dev-ops/docker-compose-mcp.yml)，
  前置是 ELK 9.x（`docker-compose-elk.yml`），共享 `nexus-devops` 外部网络。
- **配置键**（默认值）：`ops-agent.mcp.elasticsearch.enabled=true` /
  `transport=sse`（`http://127.0.0.1:8085/mcp/sse`）或 `transport=streamable-http`（`/mcp`）/
  `timeout=PT20S` / `init-timeout=PT10S`。若 streamable 模式出现 `401 Unauthorized: Session ID is required`，
  请改用 `sse` + `/mcp/sse`。

### Connection-pool reverse lookup (`ops-agent.mapping.*`)

Some domains (Hikari / Redis) don't have an `application` label directly in the
alert; instead Prometheus carries a pool identifier (Hikari `pool` / Redis
`client-name`) that is globally unique across services. `MappingProperties`
exposes two maps:

- `pool-name-to-app` — `Pool_HikariCP → { application, dataId, group }`, used by
  `HikariTuningStrategy` to pick the right `${app}-datasource-dev.yml`.
- `client-name-to-app` — `mall-redisson → mall`, used by `RedisNotifyStrategy`
  as a hint in the human-facing reason text.

When onboarding a new service, just append a row to both maps in
`ops-agent/src/main/resources/application.yml` — no code change needed.

## Key Dependencies

| Dependency | Version | Role |
|-----------|---------|------|
| `io.agentscope:agentscope` | 1.0.11 | ReActAgent, Tool/Toolkit, Model wrappers |
| `io.agentscope:agentscope-extensions-agui` | 1.0.11 | AG-UI SSE protocol |
| `io.agentscope:agentscope-agui-spring-boot-starter` | 1.0.11 | Auto-configures AG-UI endpoint |
| `com.alibaba.nacos:nacos-client` | 2.4.3 | Nacos ConfigService + NamingService |
| Spring Boot | 3.2.12 | Web + WebFlux (WebFlux needed for SSE streaming) |

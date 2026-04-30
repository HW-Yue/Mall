# AGENTS.md

## Repository Overview

`ops-agent-spring-ai` is the operations agent module in the Nexus mono-repo.

It is a Spring Boot / Spring AI Alibaba application that receives alerts, matches SOP rules, runs a ReAct agent, delegates to domain sub-agents, and executes controlled operations tools.

**Runtime:**
- Application: `yue-ops-agent`
- Port: `2322`
- Java: `21`
- Main stack: Spring Boot, Spring AI Alibaba, Nacos, Prometheus, Elasticsearch, Docker Java, RocketMQ tools, Wrench rule chain

## Module Responsibilities

| Area | Responsibility |
|------|----------------|
| Alert intake | Receive Alertmanager webhooks and route them into run sessions |
| SOP routing | Match alert text/labels to YAML SOP rules, with AI fallback where configured |
| Parent Agent | ReAct orchestration and domain sub-agent delegation |
| Sub Agents | One focused ReAct loop per ops domain |
| Skill Registry | Register domain skills and route concrete tool calls |
| Approval | Hold risky write operations in an in-memory approval queue |
| Observability | Emit run events, logs, Prometheus metrics, and tracing data |
| Web console | Static pages for chat, tools, approvals, and run timeline demos |

## Package Layout

```text
src/main/java/com/yue/opsagent/springai/
├── trigger/          # HTTP controllers and request DTOs
├── service/          # Routing, SOP, approval, run state, and orchestration services
├── agent/            # Parent ReAct agent, sub-agent delegation, ReAct parser/runner
├── skill/            # Domain skill registries, toolkits, and execution rule chain
├── domain/           # Alert, approval, and ops run domain objects
└── infrastructure/   # Configuration, observability, and external clients
```

Static demo pages live under `src/main/resources/static/`.

SOP rules live under `src/main/resources/sop/rules/`.

## Skill Registration

This project does **not** register skills by scanning `SKILL.md` files.

Project skills are Spring beans:

1. A domain registry implements `com.yue.opsagent.springai.skill.api.OpsSkillRegistry`.
2. The registry is annotated with `@Component`.
3. `MasterRegistry` receives `Collection<OpsSkillRegistry>` through Spring injection.
4. `MasterRegistry` indexes registries by `OpsSkillRegistry.name()`.
5. Tool execution goes through the Wrench rule chain before reaching the registry `execute(...)`.

Core interface:

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

Current domain skill ids:

| Skill id | Parent tool | Registry |
|----------|-------------|----------|
| `docker_ops` | `docker_skill` | `DockerSkillRegistry` |
| `mysql_inspect` | `mysql_skill` | `MysqlSkillRegistry` |
| `rocketmq_inspect` | `rocketmq_skill` | `RocketMqSkillRegistry` |
| `metrics_ops` | `prometheus_skill` | `MetricsSkillRegistry` |
| `elasticsearch_ops` | `elasticsearch_skill` | `ElasticsearchSkillRegistry` |
| `redis_inspect` | `redis_skill` | `RedisSkillRegistry` |
| `nacos_config` | `nacos_skill` | `NacosSkillRegistry` |

Parent tool names are mapped in `agent/sub/ISubAgent.java`.

## Adding Or Changing A Skill

When adding a new skill:

1. Add a package under `skill/<domain>/`.
2. Implement a `*SkillRegistry` with `@Component`.
3. Return a stable `name()` id; do not rename existing ids without updating SOP rules and sub-agent mappings.
4. Implement `description()`, `promptFragment()`, `toolMenuBrief()`, and `toolSpecification(...)` so the parent and sub-agents can choose tools with progressive disclosure.
5. Implement `toolNames()` as the whitelist for valid tool calls.
6. Implement `execute(...)` by delegating to a toolkit class; return `ToolResult`, not raw maps.
7. If any tool mutates state, override `requiresApproval(toolName)` and route the operation through the approval flow.
8. Add or update an `ISubAgent` implementation if the parent ReAct agent should delegate to this skill.
9. Update SOP YAML rules if alert workflows should use the new skill.
10. Add focused tests for registry routing, tool names, approval behavior, and SOP integration where relevant.

Do not bypass `MasterRegistry` for normal tool execution. It provides skill resolution, whitelist checks, approval checks, run cancellation checks, tracing, and result recording.

## Tool Execution Chain

Tool calls follow this path:

```text
HTTP /api/v1/tools/execute
  -> ToolsController
  -> MasterRegistry.execute(skill, tool, args)
  -> Wrench rule chain
  -> concrete OpsSkillRegistry.execute(tool, args)
  -> Toolkit
  -> ToolResult
```

The execution rule chain is assembled in `ToolExecutionRuleFilterFactory`.

Current filters include:

- `SkillResolveRuleFilter`
- `ToolWhitelistRuleFilter`
- `ToolApprovalRuleFilter`
- `RunCancelRuleFilter`
- `ToolTraceStartRuleFilter`
- `ToolExecuteRuleFilter`
- `ToolResultRecordRuleFilter`

Keep new behavior in filters only when it is cross-cutting. Domain-specific logic belongs in the registry or toolkit.

## Agent Flow

Parent ReAct flow:

```text
ChatController / OpsRouteService
  -> ParentReactAgent
  -> AgentToolRegistry
  -> ISubAgent
  -> AbstractISubReactAgent
  -> domain tools via MasterRegistry
```

Sub-agents must stay inside one technical domain and return a concise Chinese summary to the parent agent.

The sub-agent prompt intentionally requires a single legal JSON action:

- `{"action":"CALL_TOOL","tool":"<toolName>","args":{...}}`
- `{"action":"FINAL","answer":"<summary>"}`

When a tool returns empty, not found, unknown, 404, connection failure, or empty metrics, treat that as evidence. Do not fan out into unrelated tools just to keep searching.

## SOP Rules

SOP YAML files live in `src/main/resources/sop/rules/`.

Typical rule fields reference skill ids such as:

- `metrics_ops`
- `mysql_inspect`
- `redis_inspect`
- `rocketmq_inspect`
- `nacos_config`

When changing a skill id, tool name, or expected args, update all SOP rules and tests that reference it.

## Approval Rules

Read-only tools should execute directly.

State-changing tools must require approval. The canonical example is:

```java
NacosSkillRegistry.requiresApproval("nacos_publish_config") == true
```

Approved actions are executed after approval through the existing approval service path. Do not add direct write endpoints that skip approval.

## API Overview

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/v1/chat/stream` | SSE chat endpoint |
| `POST` | `/api/v1/chat/react` | Parent ReAct debug endpoint |
| `POST` | `/api/v1/tools/execute` | Direct skill/tool execution |
| `POST` | `/api/v1/alert/receive` | Alertmanager webhook |
| `GET` | `/api/v1/approvals/pending` | Pending approvals |
| `POST` | `/api/v1/approvals/{id}/approve` | Approve pending operation |
| `POST` | `/api/v1/approvals/{id}/reject` | Reject pending operation |
| `POST` | `/api/v1/ops/route-text` | Route plain text incident |
| `GET` | `/api/v1/ops/runs/{runId}` | Run state |
| `GET` | `/api/v1/ops/runs/{runId}/events` | Run event stream |
| `POST` | `/api/v1/ops/runs/{runId}/cancel` | Cancel run |

## Configuration And Running

Local configuration template:

```bash
cp application-local.example.yml application-local.yml
```

`application-local.yml` is ignored by git and is the right place for local endpoints and keys.

DashScope key:

```bash
export DASHSCOPE_API_KEY=your-key
```

Run locally:

```bash
mvn spring-boot:run
```

Build:

```bash
mvn clean package -DskipTests
```

Run tests:

```bash
mvn test
```

## Key Files

| Purpose | Path |
|---------|------|
| Project overview | `README.md` |
| Architecture docs | `docs/architecture.md` |
| API docs | `docs/api.md` |
| Skill contract | `src/main/java/com/yue/opsagent/springai/skill/api/OpsSkillRegistry.java` |
| Skill aggregator | `src/main/java/com/yue/opsagent/springai/skill/registry/MasterRegistry.java` |
| Tool rule chain factory | `src/main/java/com/yue/opsagent/springai/skill/registry/ToolExecutionRuleFilterFactory.java` |
| Parent tool registry | `src/main/java/com/yue/opsagent/springai/agent/registry/AgentToolRegistry.java` |
| Sub-agent contract | `src/main/java/com/yue/opsagent/springai/agent/sub/ISubAgent.java` |
| Sub-agent base class | `src/main/java/com/yue/opsagent/springai/agent/sub/AbstractISubReactAgent.java` |
| SOP rules | `src/main/resources/sop/rules/` |
| Web console | `src/main/resources/static/` |

## Development Rules

- Preserve existing skill ids and tool names unless the caller explicitly asks for a breaking change.
- Keep skill behavior domain-specific; keep cross-cutting behavior in rule filters.
- Keep risky operations behind approval.
- Keep registry prompt fragments short and operational. Put detailed behavior in `toolSpecification(...)` where possible.
- Return structured `ToolResult` values from toolkits and registries.
- Do not introduce network calls or shell execution paths without clear bounds and tests.
- When changing public HTTP API paths or payloads, update `docs/api.md` and affected static pages.
- When changing SOP tool references, update the matching registry tests or add coverage.
- Prefer focused unit tests around registry routing, ReAct parsing, rule chain behavior, and SOP execution.


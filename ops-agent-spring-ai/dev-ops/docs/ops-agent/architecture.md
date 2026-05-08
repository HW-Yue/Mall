# 项目架构

`ops-agent-spring-ai` 是一个面向运维场景的 Spring AI Agent 应用，核心目标不是“聊天演示”，而是把真实运维动作串成可解释、可审批、可追踪的自动化链路。

## 架构总览

```mermaid
flowchart LR
    U[用户 / 运维同学] --> UI[Web 控制台]
    U --> API[REST API]
    AM[Alertmanager] --> API

    API --> Route[OpsRouteService]
    API --> Chat[ChatController]
    API --> Tools[ToolsController]
    API --> Approval[ApprovalController]

    Route --> Match[SOP 匹配<br/>硬匹配 + AI 匹配]
    Match --> Agent[OpsAgent / ParentReactAgent]
    Agent --> Registry[Tool Registry]
    Registry --> Skill[Skill Registry / Toolkit]
    Skill --> Result[工具执行结果]
    Result --> ApprovalQueue[审批队列]

    Route --> RunState[运行状态 / 事件流]
    ApprovalQueue --> UI
    RunState --> UI
    API --> Obs[日志 / Prometheus / OTLP]
    Route --> Obs
```

![总体架构图](../diagrams/ops-agent/architecture-overview.svg)

## 组件职责

- `trigger`：HTTP 入口层，负责聊天、工具、告警、审批和运行查询
- `service`：业务编排层，负责路由、SOP 匹配、审批、运行状态管理
- `agent`：ReAct 编排层，负责父 Agent 与子 Agent 的委派
- `skill`：工具域实现层，按技能域组织 registry / toolkit
- `domain`：告警、审批、路由、运行态等领域对象
- `infrastructure`：配置、日志、追踪、SOP 外部加载
- `dev-ops/frontend`：独立部署的 Web 控制台

## 核心流转

### 告警流

```mermaid
sequenceDiagram
    participant AM as Alertmanager
    participant API as AlertReceiveController
    participant Route as OpsRouteService
    participant SOP as SopDispatcher / AI Matcher
    participant Agent as OpsAgent
    participant Tool as Tool Registry

    AM->>API: POST /api/v1/alert/receive
    API->>Route: routeAsync(AlertEvent)
    Route->>SOP: hard match / AI match
    alt 命中 SOP
        Route->>Agent: runForAlert(event, rule)
        Agent->>Tool: delegate tool / sub agent
        Tool-->>Agent: result
        Agent-->>Route: summary
    else 未命中 SOP
        Route-->>Route: 生成排查草案
    end
    Route-->>API: run session / result
```

![告警处理流程图](../diagrams/ops-agent/alert-flow.svg)

### 工具流

```mermaid
flowchart TD
    Chat[对话 / 工具调用] --> Master[MasterRegistry]
    Master --> Chain[规则链]
    Chain --> Skill[具体 Skill Registry]
    Skill --> Tool[ToolExecutor]
    Tool --> Check{是否需要审批}
    Check -- 否 --> Done[直接返回结果]
    Check -- 是 --> Queue[ApprovalService]
    Queue --> Inbox[审批队列]
    Inbox --> Run[批准后执行]
    Run --> Done
```

![工具执行模块图](../diagrams/ops-agent/tool-chain.svg)

## 设计原则

- 单一入口，多条编排路径
- 读操作默认直接执行，写操作必须经过审批
- SOP 优先，AI 兜底
- 每次执行都带运行轨迹，方便回放和排障
- 公开仓库默认值全部可本地启动，不包含内网地址或密钥

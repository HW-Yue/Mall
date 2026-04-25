# ops-agent-spring-ai 静态控制台

面向 Spring AI 版运维 Agent 的轻量工作台，保留三块真实能力：

| 区域 | API |
|------|-----|
| Stream 聊天 | `POST /api/v1/chat/stream` |
| ReAct 聊天 | `POST /api/v1/chat/react` |
| 工具执行 | `POST /api/v1/tools/execute` |
| 审批队列 | `GET /api/v1/approvals`、`GET /api/v1/approvals/stream` |
| 审批决策 | `POST /api/v1/approvals/{id}/approve`、`POST /api/v1/approvals/{id}/reject` |

## 使用

1. 执行 `mvn package` 后启动 **ops-agent-spring-ai** jar。
2. 浏览器打开服务端口，例如 `http://127.0.0.1:2322/`。
3. 默认 API 基址随当前页面同源访问：`http://127.0.0.1:2322/api/v1`。经 Gateway 时使用 `?api=http://127.0.0.1:8090/gw/api/v1/ops-ai`。

## 文件

- `index.html`：页面结构
- `config.js`：API 基址
- `css/app.css`：工作台样式
- `js/app.js`：启动与连接状态
- `js/chat-spring-ai.js`：Stream/ReAct 聊天
- `js/tools.js`：Skill/Tool 选择与 `/tools/execute`
- `js/inbox.js`：审批队列与 SSE

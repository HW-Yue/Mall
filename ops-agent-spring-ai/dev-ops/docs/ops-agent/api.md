# API 与运行说明

## 主要接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/chat/stream` | SSE 流式对话 |
| POST | `/api/v1/chat/react` | 父 Agent ReAct 调试入口 |
| POST | `/api/v1/tools/execute` | 执行工具 |
| POST | `/api/v1/alert/receive` | Alertmanager webhook |
| GET | `/api/v1/approvals/pending` | 待审批列表 |
| POST | `/api/v1/approvals/{id}/approve` | 审批通过 |
| POST | `/api/v1/approvals/{id}/reject` | 拒绝审批 |
| POST | `/api/v1/ops/route-text` | 纯文本路由 |
| GET | `/api/v1/ops/runs/{runId}` | 查看运行状态 |
| GET | `/api/v1/ops/runs/{runId}/events` | 运行事件流 |
| POST | `/api/v1/ops/runs/{runId}/cancel` | 取消运行 |

## 控制台页面

- 前端目录：`dev-ops/frontend/`
- `index.html`：运行工作台
- `runs.html`：运行历史
- `approvals.html`：审批队列
- `tools.html`：工具调用
- 默认 API 基址：`http://127.0.0.1:8090/gw/api/v1/ops-ai`

## 常见调用

### 触发对话

```bash
curl -X POST http://127.0.0.1:8096/api/v1/chat/react \
  -H 'Content-Type: application/json' \
  -d '{"message":"帮我排查 Redis 延迟升高"}'
```

### 执行工具

```bash
curl -X POST http://127.0.0.1:8096/api/v1/tools/execute \
  -H 'Content-Type: application/json' \
  -d '{"skill":"redis_inspect","tool":"redis_info","args":{}}'
```

### 接收告警

```bash
curl -X POST http://127.0.0.1:8096/api/v1/alert/receive \
  -H 'Content-Type: application/json' \
  -d @alertmanager-payload.json
```

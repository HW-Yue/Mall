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
| GET | `/api/v1/ops/config/routing-policy` | 查看当前路由策略 |
| PUT | `/api/v1/ops/config/routing-policy` | 动态更新预警自主规划开关 |
| GET | `/api/v1/ops/runs/recent` | 查看当前进程内最近运行摘要 |
| GET | `/api/v1/ops/runs/{runId}` | 查看运行状态 |
| GET | `/api/v1/ops/runs/{runId}/events` | 运行事件流 |
| GET | `/api/v1/ops/runs/{runId}/timeline` | 查看 ES 中归档的运行时间线 |
| POST | `/api/v1/ops/runs/{runId}/cancel` | 取消运行 |

## 控制台页面

- 前端目录：`dev-ops/frontend/`
- `index.html`：运行工作台
- 运行工作台支持动态切换“预警仅硬匹配 / 预警参考 SOP 自主规划”；纯文本请求固定允许自主规划。
- `runs.html`：运行历史，优先展示 `recent`，再补充 DB `history`
- `approvals.html`：审批队列
- `tools.html`：工具调用
- 默认 API 基址：`http://127.0.0.1:8090/gw/api/v1/ops-ai`

## 路由策略接口说明

- `GET /api/v1/ops/config/routing-policy`
  返回当前运行时路由策略快照。`alertAutonomousPlanningEnabled=false` 表示结构化预警仅允许硬匹配；纯文本请求固定允许自主规划。
- `PUT /api/v1/ops/config/routing-policy`
  请求体：
  ```json
  {
    "alertAutonomousPlanningEnabled": true
  }
  ```
  立即更新当前进程内的预警自主规划开关。该配置为运行时内存态，服务重启后会回到 `application.yml` 默认值。

## 运行历史接口说明

- `GET /api/v1/ops/runs/recent?size=50`
  返回当前服务进程内可见的最近运行摘要，适合前端直接列出“刚刚跑过的 run”。服务重启后这部分会丢失。
- `GET /api/v1/ops/runs/history?size=50`
  返回来自 MySQL `ops_run_summary` 的历史摘要，适合跨重启的长期回溯与 `runId` 可发现性。
- `GET /api/v1/ops/runs/{runId}/timeline`
  返回 ES 中单个 run 的归档事件明细；如果 ES 中尚无该 run，前端应回退到 `GET /api/v1/ops/runs/{runId}` 的实时事件或展示“仅有摘要”。

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

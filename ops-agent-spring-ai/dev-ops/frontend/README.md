# ops-agent 前端控制台

本目录存放 `ops-agent-spring-ai` 的独立静态前端。

## 页面入口

- `index.html`：运行工作台
- `runs.html`：运行历史
- `approvals.html`：审批队列
- `tools.html`：工具调用

## API 基址

默认请求：

```text
http://localhost:8090/gw/api/v1/ops-ai
```

覆盖优先级：

1. URL 参数 `?api=...`
2. `localStorage['yue-ops-agent.api-base']`
3. `config.js` 默认值

示例：

```text
http://localhost:8089/index.html?api=http://127.0.0.1:8090/gw/api/v1/ops-ai
```

## 本地打开

直接用静态文件服务器或 Docker/Nginx 打开本目录即可。后端 API 仍需单独启动。

## Docker

前端镜像由仓库根目录的 `dev-ops/docker-compose-apps-test.yml` 统一构建与启动，对应服务名：

- `ops-agent-frontend`

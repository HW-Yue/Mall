# ops-agent DevOps 前端

ops-agent 自带的单页控制台，把「和 Agent 对话」「审批 Nacos 变更」放在同一个页面，
纯 HTML + 原生 JS，零构建、零依赖，可以用 `file://` 双击直接打开。

```
devops/
├── index.html             单页入口：顶部 Tab 切换「对话 / 审批收件箱」
├── config.js              注入 window.API_BASE，所有 fetch/SSE/AguiClient 都用它拼 URL
├── css/
│   └── app.css            浅色主题（暖灰 + 靛紫），所有组件样式都在这里
└── js/
    ├── app.js             外壳：Tab 切换、api-chip、pending 徽章、连接状态灯
    ├── chat.js            对话视图：对接 AG-UI（后续会换成 MCP + Skill 的 Agent）
    ├── inbox.js           审批收件箱：SSE 实时推送、IDEA 风格并排 diff、approve/reject
    └── agui-client.js     AG-UI 协议极简客户端（通用，不和业务耦合）
```

## 怎么用

### 1. 启动 ops-agent 后端

```bash
cd ops-agent
mvn spring-boot:run
# 或者
java -jar target/ops-agent-*.jar
```

默认监听 `http://localhost:8098`。

### 2. 打开前端

**方式一：双击打开（推荐）**

- 文件管理器里直接双击 `index.html`
- 浏览器会以 `file://` 协议打开，通过 CORS 访问后端 API
- URL 带 hash 可指定默认 tab：`index.html#inbox` / `index.html#chat`

**方式二：本地 http-server（如果嫌 `file://` 观感差）**

```bash
cd ops-agent/devops
python3 -m http.server 5173
# 然后访问 http://localhost:5173/index.html
```

### 3. 切换后端地址

默认后端地址：`http://localhost:8098`。有三种方式覆盖：

1. **URL 参数**（一次性 + 持久化）：
   `index.html?api=http://10.0.0.5:8098`
   访问后会写入 `localStorage`，下次再打开仍然用这个地址。
2. **localStorage**（浏览器 DevTools Console）：
   ```js
   localStorage.setItem('ops-agent.api-base', 'http://10.0.0.5:8098');
   ```
3. **清除记忆**：
   ```js
   localStorage.removeItem('ops-agent.api-base');
   ```

## 布局与交互

顶部状态条：
- 左侧 `Nexus Ops` 品牌 + 副标题
- 中间 Tab：`对话` / `审批收件箱`；收件箱 Tab 有待审批数量徽章
- 右侧 api-chip：显示当前 `window.API_BASE` 与 SSE 连接状态灯（绿=在线 / 红=断线 / 灰=初始）

对话视图（`view-chat`）：
- 顶部提示卡提供几个快捷指令 chip（点击填入输入框）
- 右上角「新对话」按钮重置 `threadId` 和历史
- 输入框支持 `Enter` 发送 / `Shift+Enter` 换行 / 自动适应行数
- 生成中会显示"停止"按钮，点了会调 `AguiClient.abort()` 中断 SSE

审批收件箱（`view-inbox`）：
- 左侧列表：支持按「状态」与「域（sentinel / dtp / notify / ...）」双重过滤
- 右侧详情：IDEA 风格并排 diff + 告警原文 + Agent 理由 + 决策按钮
- 通知型任务（domain=notify）不展示 diff，按钮文案改为「知道了 / 忽略」

## 后端 CORS 配合

后端的 [`CorsConfig.java`](../src/main/java/com/yue/opsagent/config/CorsConfig.java) 已经给
`/api/**` 和 `/agui/**` 放行所有 Origin（包括 `file://` 的 `null` Origin）；不启用
credentials，无 Cookie 需求。

如果遇到 CORS 报错，优先检查：

- 后端是否确实在 `8098` 端口（`application.yml` 里 `server.port`）
- 浏览器 DevTools Network 里的 OPTIONS 预检是否返回 200
- `Access-Control-Allow-Origin` 响应头是否回填了请求的 Origin

## 后续扩展：聊天 Agent 换成 MCP + Skill

`js/chat.js` 里预留了 `Chat.config.endpoint`，后续接 MCP Agent 只需要两步：

1. 在 `chat.js` 顶部 `CONFIG` 里改 `endpoint`（或整体替换 `AguiClient` 为你的 MCP 客户端）；
2. UI 结构（消息气泡、快捷 chip、Composer）都复用 `index.html` 里的 DOM，不用改 CSS。

收件箱 (`js/inbox.js`) 是纯 REST + SSE，和聊天协议完全解耦，不受影响。

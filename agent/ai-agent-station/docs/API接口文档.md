# AI Agent Station 接口文档

本文档描述当前项目所有 HTTP 接口的请求与响应规范。除特别说明外，接口基础路径为应用部署地址（如 `http://localhost:8080`）。

---

## 一、通用说明

### 1.1 请求头

| Header        | 说明                    |
|---------------|-------------------------|
| Content-Type  | 见各接口说明（JSON 一般为 `application/json`） |
| Accept        | 建议 `application/json`（SSE 接口见说明）   |

### 1.2 统一响应结构（非 SSE 接口）

除「智能体对话（SSE）」外，其余接口均返回如下 JSON 结构：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {}
}
```

| 字段  | 类型   | 说明         |
|-------|--------|--------------|
| code  | String | 业务状态码   |
| info  | String | 提示信息     |
| data  | Object | 业务数据，可为 null |

### 1.3 状态码说明

| code  | 含义     |
|-------|----------|
| 0000  | 成功     |
| 0001  | 未知失败 |
| 0002  | 非法参数 |
| 0003  | 登录失败 |

### 1.4 跨域

所有接口均支持跨域：各 Controller 配置 `@CrossOrigin`，并启用全局 `CorsFilter`（`WebCorsConfig`），允许任意来源、任意头、GET/POST/PUT/DELETE/OPTIONS，便于前端（如 `http://localhost:3000`）访问后端（如 `http://127.0.0.1:8099`）。

---

## 二、智能体接口（/api/v1/agent）

基础路径：`/api/v1/agent`  
Controller：`AiAgentController`

### 2.1 智能体对话（SSE 流式）

**POST** `/api/v1/agent/auto_agent`

智能体对话，服务端以 SSE（Server-Sent Events）流式返回内容。

**请求体** `application/json`：

| 参数        | 类型    | 必填 | 说明                    |
|-------------|---------|------|-------------------------|
| aiAgentId   | String  | 是   | 智能体 ID               |
| message     | String  | 是   | 用户消息                |
| sessionId   | String  | 是   | 会话 ID                 |
| maxStep     | Integer | 否   | 最大执行步数，默认 5     |

**请求示例**：

```json
{
  "aiAgentId": "00000001",
  "message": "你好",
  "sessionId": "session-001",
  "maxStep": 5
}
```

**响应**：

- **Content-Type**：`text/event-stream`
- **格式**：每行 `data: <JSON 或 [DONE]>`，流结束为 `data: [DONE]`
- **错误**：参数错误时返回 HTTP 400 + 上述统一 JSON（code 为 0002）

---

### 2.2 装配智能体

**POST** `/api/v1/agent/armory_agent`

按智能体 ID 执行装配逻辑。

**请求体** `application/json`：

| 参数   | 类型   | 必填 | 说明      |
|--------|--------|------|-----------|
| agentId | String | 是   | 智能体 ID |

**响应 data**：`Boolean`（是否装配成功）

---

### 2.3 查询可用智能体列表

**GET** `/api/v1/agent/query_available_agents`

查询当前可用的智能体列表。

**请求参数**：无

**响应 data**：`List<AiAgentResponseDTO>`

| 字段        | 类型    | 说明                    |
|-------------|---------|-------------------------|
| agentId     | String  | 智能体 ID               |
| agentName   | String  | 智能体名称              |
| description | String  | 描述                    |
| channel     | String  | 渠道，如 agent、chat_stream |
| strategy    | String  | 执行策略，如 auto、flow |
| status      | Integer | 状态，0 禁用 1 启用     |

---

## 三、管理端 - 数据统计

**GET** `/api/v1/admin/data/statistics/get-data-statistics`

**说明**：获取系统数据统计（智能体、客户端、MCP 工具、系统提示、知识库、顾问、模型等数量及部分模拟指标）。

**请求参数**：无

**响应 data**：`DataStatisticsResponseDTO`

| 字段               | 类型    | 说明           |
|--------------------|---------|----------------|
| activeAgentCount   | Long    | 活跃智能体数   |
| clientCount        | Long    | 客户端数       |
| mcpToolCount       | Long    | MCP 工具数     |
| systemPromptCount  | Long    | 系统提示词数   |
| ragOrderCount      | Long    | 知识库数       |
| advisorCount       | Long    | 顾问数         |
| modelCount         | Long    | 模型数         |
| todayRequestCount  | Long    | 今日请求数     |
| successRate        | Double  | 成功率         |
| runningTaskCount   | Long    | 运行中任务数   |

---

## 四、管理端 - 管理员用户（/api/v1/admin/admin-user）

| 方法   | 路径                          | 说明                     |
|--------|-------------------------------|--------------------------|
| POST   | /create                       | 创建管理员用户           |
| PUT    | /update-by-id                 | 按主键 ID 更新           |
| PUT    | /update-by-user-id             | 按 userId 更新           |
| DELETE | /delete-by-id/{id}            | 按主键 ID 删除           |
| DELETE | /delete-by-user-id/{userId}   | 按 userId 删除           |
| GET    | /query-by-id/{id}             | 按主键 ID 查询           |
| GET    | /query-by-user-id/{userId}    | 按 userId 查询           |
| GET    | /query-by-username/{username} | 按用户名查询             |
| GET    | /query-enabled                | 查询启用状态用户列表     |
| GET    | /query-by-status/{status}     | 按状态查询用户列表       |
| POST   | /query-list                   | 分页/条件查询用户列表    |
| GET    | /query-all                    | 查询所有管理员用户       |
| POST   | /login                        | 管理员登录               |
| POST   | /validate-login               | 校验登录（用户名+密码）  |

**创建/更新** 请求体：`AdminUserRequestDTO`（与库表字段一致，如 id、userId、username、password、status 等）。  
**登录/校验** 请求体：`AdminUserLoginRequestDTO`（username、password）。  
**分页查询** 请求体：`AdminUserQueryRequestDTO`（userId、username、status、pageNum、pageSize 等）。  
**查询类** 响应 data：单条为 `AdminUserResponseDTO`，列表为 `List<AdminUserResponseDTO>`；登录成功 data 为用户信息。

---

## 五、管理端 - AI 客户端（/api/v1/admin/ai-client）

| 方法   | 路径                        | 说明                 |
|--------|-----------------------------|----------------------|
| POST   | /create                     | 创建 AI 客户端配置   |
| PUT    | /update-by-id               | 按主键 ID 更新       |
| PUT    | /update-by-client-id        | 按 clientId 更新     |
| DELETE | /delete-by-id/{id}          | 按主键 ID 删除       |
| DELETE | /delete-by-client-id/{clientId} | 按 clientId 删除 |
| GET    | /query-by-id/{id}           | 按主键 ID 查询       |
| GET    | /query-by-client-id/{clientId} | 按 clientId 查询 |
| GET    | /query-enabled              | 查询启用的客户端     |
| POST   | /query-list                 | 分页/条件查询列表    |
| GET    | /query-all                  | 查询所有             |
| GET    | /strategy-slots             | 按策略返回必选/可选 client_type 槽位（二级联动） |
| GET    | /list-by-type               | 按 clientType 返回该类型下客户端简要列表（走索引） |
| GET    | /query-by-client-type       | 按 client_type 查询该类型下客户端完整列表（二级联动第二步，含 clientType） |

请求体：`AiClientRequestDTO` / `AiClientQueryRequestDTO`（query-list）。  
响应 data：`AiClientResponseDTO` 或 `List<AiClientResponseDTO>`；创建/更新/查询单条/列表含字段：id、clientId、clientName、description、**clientType**、status、createTime、updateTime。

### 5.1 按策略查询槽位（strategy-slots）

**GET** `/api/v1/admin/ai-client/strategy-slots`

根据执行策略返回该策略定义的必选/可选 `client_type` 列表，供前端二级联动（先选策略得槽位类型，再按类型选具体客户端）。

**请求参数**（Query）：

| 参数     | 类型   | 必填 | 说明 |
|----------|--------|------|------|
| strategy | String | 是   | 策略标识，如 `flowAgentExecuteStrategy`、`autoAgentExecuteStrategy`、`fixedAgentExecuteStrategy` |

**响应 data**：`AiClientStrategySlotsResponseDTO`

| 字段          | 类型          | 说明 |
|---------------|---------------|------|
| strategy      | String        | 策略标识 |
| requiredSlots | List\<String\> | 必选 client_type 列表（如 FLOW 策略：TOOL_MCP_CLIENT、PLANNING_CLIENT、EXECUTOR_CLIENT） |
| optionalSlots | List\<String\> | 可选 client_type 列表 |

### 5.2 按类型查询客户端列表（list-by-type）

**GET** `/api/v1/admin/ai-client/list-by-type`

按 `clientType` 过滤，利用 `ai_client.client_type` 索引高效返回该类型下所有客户端实例（仅 clientId、clientName），用于二级联动下拉。

**请求参数**（Query）：

| 参数       | 类型   | 必填 | 说明 |
|------------|--------|------|------|
| clientType | String | 是   | 客户端类型，如 TOOL_MCP_CLIENT、PLANNING_CLIENT、EXECUTOR_CLIENT |

**响应 data**：`List<AiClientSimpleItemDTO>`

| 字段       | 类型   | 说明     |
|------------|--------|----------|
| clientId   | String | 客户端 ID |
| clientName | String | 客户端名称 |

### 5.3 按 client_type 查询客户端完整列表（query-by-client-type）

**GET** `/api/v1/admin/ai-client/query-by-client-type`

二级联动第二步：按槽位类型（client_type）拉取该类型下的客户端实例完整列表，供前端下拉选择。响应中每条记录含 `clientType`，便于前端按类型筛选/降级。

**完整示例**：`http://127.0.0.1:8099/api/v1/admin/ai-client/query-by-client-type?clientType=TASK_ANALYZER_CLIENT`

**请求参数**（Query）：

| 参数       | 类型   | 必填 | 说明 |
|------------|--------|------|------|
| clientType | String | 是   | client_type 编码，如 TASK_ANALYZER_CLIENT、PRECISION_EXECUTOR_CLIENT、QUALITY_SUPERVISOR_CLIENT、RESPONSE_ASSISTANT 等 |

**响应 data**：`List<AiClientResponseDTO>`（与现有客户端接口一致，含 clientType 便于前端降级过滤）

| 字段        | 类型    | 说明           |
|-------------|---------|----------------|
| id          | Long    | 主键 ID        |
| clientId    | String  | 客户端 ID      |
| clientName  | String  | 客户端名称     |
| description | String  | 描述（可选）   |
| clientType  | String  | 客户端类型编码（可选，建议返回便于前端按类型筛选） |
| status      | Integer | 状态           |
| createTime  | String  | 创建时间       |
| updateTime  | String  | 更新时间       |

**响应示例**：

```json
{
  "code": "0000",
  "info": "success",
  "data": [
    {
      "id": 1,
      "clientId": "3101",
      "clientName": "任务分析和状态判断",
      "clientType": "TASK_ANALYZER_CLIENT",
      "status": 1,
      "createTime": "...",
      "updateTime": "..."
    }
  ]
}
```

前端约定：若该接口 404 或失败，前端可降级为调用「查询启用客户端」接口，并在结果中按 clientType 过滤；此时需「查询启用」接口在每条记录里返回 clientType。

---

## 六、管理端 - AI 客户端模型（/api/v1/admin/ai-client-model）

| 方法   | 路径                            | 说明                   |
|--------|---------------------------------|------------------------|
| POST   | /create                         | 创建模型配置           |
| PUT    | /update-by-id                   | 按主键 ID 更新         |
| PUT    | /update-by-model-id             | 按 modelId 更新        |
| DELETE | /delete-by-id/{id}              | 按主键 ID 删除         |
| DELETE | /delete-by-model-id/{modelId}   | 按 modelId 删除        |
| GET    | /query-by-id/{id}               | 按主键 ID 查询         |
| GET    | /query-by-model-id/{modelId}    | 按 modelId 查询        |
| GET    | /query-by-api-id/{apiId}        | 按 apiId 查询          |
| GET    | /query-by-model-type/{modelType}| 按模型类型查询        |
| GET    | /query-enabled                 | 查询启用的模型         |
| POST   | /query-list                    | 分页/条件查询          |
| GET    | /query-all                     | 查询所有               |

请求体：`AiClientModelRequestDTO` / `AiClientModelQueryRequestDTO`。  
响应 data：`AiClientModelResponseDTO` 或 `List<AiClientModelResponseDTO>`。

---

## 七、管理端 - 系统提示词（/api/v1/admin/ai-client-system-prompt）

| 方法   | 路径                              | 说明                     |
|--------|-----------------------------------|--------------------------|
| POST   | /create                           | 创建系统提示词配置       |
| PUT    | /update-by-id                     | 按主键 ID 更新           |
| PUT    | /update-by-prompt-id              | 按 promptId 更新         |
| DELETE | /delete-by-id/{id}                | 按主键 ID 删除           |
| DELETE | /delete-by-prompt-id/{promptId}   | 按 promptId 删除         |
| GET    | /query-by-id/{id}                 | 按主键 ID 查询           |
| GET    | /query-by-prompt-id/{promptId}    | 按 promptId 查询         |
| GET    | /query-all                        | 查询所有                 |
| GET    | /query-enabled                    | 查询启用的提示词         |
| GET    | /query-by-prompt-name/{promptName}| 按提示词名称查询        |
| POST   | /query-list                       | 分页/条件查询            |

请求体：`AiClientSystemPromptRequestDTO` / `AiClientSystemPromptQueryRequestDTO`。  
响应 data：`AiClientSystemPromptResponseDTO` 或 `List<AiClientSystemPromptResponseDTO>`。

---

## 八、管理端 - MCP 工具（/api/v1/admin/ai-client-tool-mcp）

| 方法   | 路径                              | 说明                   |
|--------|-----------------------------------|------------------------|
| POST   | /create                           | 创建 MCP 客户端配置    |
| PUT    | /update-by-id                     | 按主键 ID 更新         |
| PUT    | /update-by-mcp-id                 | 按 mcpId 更新          |
| DELETE | /delete-by-id/{id}                | 按主键 ID 删除         |
| DELETE | /delete-by-mcp-id/{mcpId}         | 按 mcpId 删除          |
| GET    | /query-by-id/{id}                 | 按主键 ID 查询         |
| GET    | /query-by-mcp-id/{mcpId}          | 按 mcpId 查询          |
| GET    | /query-all                        | 查询所有               |
| GET    | /query-by-status/{status}        | 按状态查询             |
| GET    | /query-by-transport-type/{transportType} | 按传输类型查询 |
| GET    | /query-enabled                    | 查询启用的 MCP 配置    |
| POST   | /query-list                       | 分页/条件查询          |

请求体：`AiClientToolMcpRequestDTO` / `AiClientToolMcpQueryRequestDTO`。  
响应 data：`AiClientToolMcpResponseDTO` 或 `List<AiClientToolMcpResponseDTO>`。

---

## 九、管理端 - 顾问配置（/api/v1/admin/ai-client-advisor）

| 方法   | 路径                                | 说明                 |
|--------|-------------------------------------|----------------------|
| POST   | /create                             | 创建顾问配置         |
| PUT    | /update-by-id                       | 按主键 ID 更新       |
| PUT    | /update-by-advisor-id               | 按 advisorId 更新    |
| DELETE | /delete-by-id/{id}                  | 按主键 ID 删除       |
| DELETE | /delete-by-advisor-id/{advisorId}   | 按 advisorId 删除    |
| GET    | /query-by-id/{id}                   | 按主键 ID 查询       |
| GET    | /query-by-advisor-id/{advisorId}    | 按 advisorId 查询    |
| GET    | /query-enabled                      | 查询启用的顾问       |
| GET    | /query-by-status/{status}           | 按状态查询           |
| GET    | /query-by-type/{advisorType}        | 按顾问类型查询       |
| POST   | /query-list                         | 分页/条件查询        |
| GET    | /query-all                          | 查询所有             |

请求体：`AiClientAdvisorRequestDTO` / `AiClientAdvisorQueryRequestDTO`。  
响应 data：`AiClientAdvisorResponseDTO` 或 `List<AiClientAdvisorResponseDTO>`。

---

## 十、管理端 - 知识库配置（/api/v1/admin/ai-client-rag-order）

| 方法   | 路径                                  | 说明                     |
|--------|---------------------------------------|--------------------------|
| POST   | /create                               | 创建知识库配置           |
| PUT    | /update-by-id                         | 按主键 ID 更新           |
| PUT    | /update-by-rag-id                     | 按 ragId 更新            |
| DELETE | /delete-by-id/{id}                    | 按主键 ID 删除           |
| DELETE | /delete-by-rag-id/{ragId}             | 按 ragId 删除            |
| GET    | /query-by-id/{id}                     | 按主键 ID 查询           |
| GET    | /query-by-rag-id/{ragId}              | 按 ragId 查询            |
| GET    | /query-enabled                        | 查询启用的知识库         |
| GET    | /query-by-knowledge-tag/{knowledgeTag}| 按知识标签查询          |
| GET    | /query-by-status/{status}             | 按状态查询               |
| POST   | /query-list                           | 分页/条件查询            |
| GET    | /query-all                            | 查询所有                 |
| POST   | file/upload                           | 上传知识库文件（见下）   |

**file/upload**  
- **Content-Type**：`multipart/form-data`  
- **参数**：`name`（名称）、`tag`（标签）、`files`（文件列表，可多文件）  
- **响应 data**：`Boolean`

其余请求体：`AiClientRagOrderRequestDTO` / `AiClientRagOrderQueryRequestDTO`。  
响应 data：`AiClientRagOrderResponseDTO` 或 `List<AiClientRagOrderResponseDTO>`。

---

## 十一、管理端 - 拖拉拽流程图配置（/api/v1/admin/ai-agent-draw）

基础路径：`/api/v1/admin/ai-agent-draw`  
Controller：`AiAgentDrawAdminController`

| 方法   | 路径                    | 说明                   |
|--------|-------------------------|------------------------|
| GET    | /client-types-by-strategy | 按策略返回 client_type 槽位编码数组（二级联动第一步） |
| POST   | /query-list             | 分页/条件查询配置列表  |
| POST   | /save-config             | 保存流程图配置（含创建/更新） |
| GET    | /get-config/{configId}   | 根据 configId 获取配置 |
| DELETE | /delete-config/{configId}| 根据 configId 删除配置 |

### 11.1 根据策略获取 client_type 槽位列表（client-types-by-strategy）

**GET** `/api/v1/admin/ai-agent-draw/client-types-by-strategy`

用途：二级联动第一步，按 Agent 策略拉取可选的客户端类型（槽位）。

**完整示例**：`http://127.0.0.1:8099/api/v1/admin/ai-agent-draw/client-types-by-strategy?strategy=flowAgentExecuteStrategy`

**请求参数**（Query）：

| 参数     | 类型   | 必填 | 说明 |
|----------|--------|------|------|
| strategy | String | 是   | 策略枚举，如 `flowAgentExecuteStrategy`、`autoAgentExecuteStrategy`、`fixedAgentExecuteStrategy` |

**响应体**：标准 ApiResponse\<string[]\>，成功时 **data 为 client_type 编码数组**。

**响应示例**：

```json
{
  "code": "0000",
  "info": "success",
  "data": [
    "TASK_ANALYZER_CLIENT",
    "PRECISION_EXECUTOR_CLIENT",
    "QUALITY_SUPERVISOR_CLIENT",
    "RESPONSE_ASSISTANT"
  ]
}
```

前端约定：若后端暂未实现，前端可用本地兜底映射（同上数组）仍可正常联动。

### 11.2 保存智能体/流程图配置（save-config）

**POST** `/api/v1/admin/ai-agent-draw/save-config`

保存智能体及其流程图配置。一次请求会完成：创建/更新流程图配置、解析并写入智能体（Agent）信息、解析节点关系并写入 agent-client 流程配置及客户端配置关系。若传入已存在的 `configId` 则执行更新，否则生成新 `configId` 并创建新配置；服务端会为本次保存生成唯一的 `agentId` 并回写使用。

**请求体** `application/json`：`AiAgentDrawConfigRequestDTO`

| 参数        | 类型   | 必填 | 说明 |
|-------------|--------|------|------|
| configId    | String | 否   | 配置唯一标识；不传则服务端自动生成 UUID |
| configName  | String | 是   | 配置名称 |
| configData  | String | 是   | 完整拖拉拽画布 JSON，须包含 `nodes`、`edges`（结构见下） |
| description | String | 否   | 配置描述 |
| agentId     | String | 否   | 关联智能体 ID（服务端保存时会生成并覆盖，仅作预留） |
| createBy    | String | 否   | 创建人 |
| updateBy    | String | 否   | 更新人 |

**configData 结构说明**（JSON 字符串，内为对象）：

- **nodes**（数组）：画布节点列表。每个节点需包含：
  - `type`：节点类型，`"agent"` 表示智能体节点，`"client"` 表示客户端节点
  - `id`：节点唯一 ID（用于 edges 引用）
  - `data`：节点数据
    - `data.inputsValues`：表单项键值
      - **agent 节点** 建议包含：`agentName`、`description`、`channel`、`strategy`
      - **client 节点** 建议包含：`clientType`、`clientId`、`clientName`、`sequence`、`stepPrompt`
  - **client 节点** 需具备有效 `refId`（或通过 inputsValues 等能解析出 client 引用），否则该节点参与的关系可能被跳过并打 WARN 日志
- **edges**（数组）：连线关系。每条边需包含 `source`、`target`（或等价 sourceNodeId/targetNodeId），用于解析 agent 与 client 的对应关系及端口信息。

**响应**：统一 JSON，成功时 `code` 为 `0000`，`data` 为本次保存的 **configId**（String）。

**错误情况**：

- `configName` 为空：返回 `0002`，info 为「配置名称不能为空」
- `configData` 为空：返回 `0002`，info 为「配置数据不能为空」
- 其它异常：返回 `0001`，info 为「保存失败」或具体异常信息

**请求示例**：

```json
{
  "configName": "我的流程配置",
  "description": "用于演示的智能体流程图",
  "configData": "{\"nodes\":[{\"id\":\"agent-1\",\"type\":\"agent\",\"data\":{\"inputsValues\":{\"agentName\":\"客服助手\",\"description\":\"自动回复\",\"channel\":\"agent\",\"strategy\":\"flowAgentExecuteStrategy\"}}},{\"id\":\"client-1\",\"type\":\"client\",\"data\":{\"refId\":\"48376249\",\"inputsValues\":{\"clientType\":\"EXECUTOR_CLIENT\",\"clientId\":\"48376249\",\"clientName\":\"执行器\"}}}],\"edges\":[{\"source\":\"agent-1\",\"target\":\"client-1\"}]}"
}
```

**响应示例**：

```json
{
  "code": "0000",
  "info": "成功",
  "data": "a1b2c3d4e5f6789012345678"
}
```

### 11.3 其余接口说明

**query-list**  
- 请求体：`AiAgentDrawConfigQueryRequestDTO`（configId、configName、agentId、status、pageNum、pageSize）  
- 响应 data：`List<AiAgentDrawConfigResponseDTO>`

**get-config**  
- 路径参数：`configId`  
- 响应 data：`AiAgentDrawConfigResponseDTO`

**delete-config**  
- 路径参数：`configId`  
- 会级联删除关联的智能体、流程配置等  
- 响应 data：`String`（如 "删除成功"）

---

## 十二、接口清单汇总（按路径排序）

| 方法   | 完整路径 |
|--------|----------|
| POST   | /api/v1/agent/auto_agent |
| POST   | /api/v1/agent/armory_agent |
| GET    | /api/v1/agent/query_available_agents |
| GET    | /api/v1/admin/data/statistics/get-data-statistics |
| POST   | /api/v1/admin/admin-user/create |
| PUT    | /api/v1/admin/admin-user/update-by-id |
| PUT    | /api/v1/admin/admin-user/update-by-user-id |
| DELETE | /api/v1/admin/admin-user/delete-by-id/{id} |
| DELETE | /api/v1/admin/admin-user/delete-by-user-id/{userId} |
| GET    | /api/v1/admin/admin-user/query-by-id/{id} |
| GET    | /api/v1/admin/admin-user/query-by-user-id/{userId} |
| GET    | /api/v1/admin/admin-user/query-by-username/{username} |
| GET    | /api/v1/admin/admin-user/query-enabled |
| GET    | /api/v1/admin/admin-user/query-by-status/{status} |
| POST   | /api/v1/admin/admin-user/query-list |
| GET    | /api/v1/admin/admin-user/query-all |
| POST   | /api/v1/admin/admin-user/login |
| POST   | /api/v1/admin/admin-user/validate-login |
| POST   | /api/v1/admin/ai-client/create |
| PUT    | /api/v1/admin/ai-client/update-by-id |
| PUT    | /api/v1/admin/ai-client/update-by-client-id |
| DELETE | /api/v1/admin/ai-client/delete-by-id/{id} |
| DELETE | /api/v1/admin/ai-client/delete-by-client-id/{clientId} |
| GET    | /api/v1/admin/ai-client/query-by-id/{id} |
| GET    | /api/v1/admin/ai-client/query-by-client-id/{clientId} |
| GET    | /api/v1/admin/ai-client/query-enabled |
| POST   | /api/v1/admin/ai-client/query-list |
| GET    | /api/v1/admin/ai-client/query-all |
| GET    | /api/v1/admin/ai-client/strategy-slots |
| GET    | /api/v1/admin/ai-client/list-by-type |
| GET    | /api/v1/admin/ai-client/query-by-client-type |
| POST   | /api/v1/admin/ai-client-model/create |
| PUT    | /api/v1/admin/ai-client-model/update-by-id |
| PUT    | /api/v1/admin/ai-client-model/update-by-model-id |
| DELETE | /api/v1/admin/ai-client-model/delete-by-id/{id} |
| DELETE | /api/v1/admin/ai-client-model/delete-by-model-id/{modelId} |
| GET    | /api/v1/admin/ai-client-model/query-by-id/{id} |
| GET    | /api/v1/admin/ai-client-model/query-by-model-id/{modelId} |
| GET    | /api/v1/admin/ai-client-model/query-by-api-id/{apiId} |
| GET    | /api/v1/admin/ai-client-model/query-by-model-type/{modelType} |
| GET    | /api/v1/admin/ai-client-model/query-enabled |
| POST   | /api/v1/admin/ai-client-model/query-list |
| GET    | /api/v1/admin/ai-client-model/query-all |
| POST   | /api/v1/admin/ai-client-system-prompt/create |
| PUT    | /api/v1/admin/ai-client-system-prompt/update-by-id |
| PUT    | /api/v1/admin/ai-client-system-prompt/update-by-prompt-id |
| DELETE | /api/v1/admin/ai-client-system-prompt/delete-by-id/{id} |
| DELETE | /api/v1/admin/ai-client-system-prompt/delete-by-prompt-id/{promptId} |
| GET    | /api/v1/admin/ai-client-system-prompt/query-by-id/{id} |
| GET    | /api/v1/admin/ai-client-system-prompt/query-by-prompt-id/{promptId} |
| GET    | /api/v1/admin/ai-client-system-prompt/query-all |
| GET    | /api/v1/admin/ai-client-system-prompt/query-enabled |
| GET    | /api/v1/admin/ai-client-system-prompt/query-by-prompt-name/{promptName} |
| POST   | /api/v1/admin/ai-client-system-prompt/query-list |
| POST   | /api/v1/admin/ai-client-tool-mcp/create |
| PUT    | /api/v1/admin/ai-client-tool-mcp/update-by-id |
| PUT    | /api/v1/admin/ai-client-tool-mcp/update-by-mcp-id |
| DELETE | /api/v1/admin/ai-client-tool-mcp/delete-by-id/{id} |
| DELETE | /api/v1/admin/ai-client-tool-mcp/delete-by-mcp-id/{mcpId} |
| GET    | /api/v1/admin/ai-client-tool-mcp/query-by-id/{id} |
| GET    | /api/v1/admin/ai-client-tool-mcp/query-by-mcp-id/{mcpId} |
| GET    | /api/v1/admin/ai-client-tool-mcp/query-all |
| GET    | /api/v1/admin/ai-client-tool-mcp/query-by-status/{status} |
| GET    | /api/v1/admin/ai-client-tool-mcp/query-by-transport-type/{transportType} |
| GET    | /api/v1/admin/ai-client-tool-mcp/query-enabled |
| POST   | /api/v1/admin/ai-client-tool-mcp/query-list |
| POST   | /api/v1/admin/ai-client-advisor/create |
| PUT    | /api/v1/admin/ai-client-advisor/update-by-id |
| PUT    | /api/v1/admin/ai-client-advisor/update-by-advisor-id |
| DELETE | /api/v1/admin/ai-client-advisor/delete-by-id/{id} |
| DELETE | /api/v1/admin/ai-client-advisor/delete-by-advisor-id/{advisorId} |
| GET    | /api/v1/admin/ai-client-advisor/query-by-id/{id} |
| GET    | /api/v1/admin/ai-client-advisor/query-by-advisor-id/{advisorId} |
| GET    | /api/v1/admin/ai-client-advisor/query-enabled |
| GET    | /api/v1/admin/ai-client-advisor/query-by-status/{status} |
| GET    | /api/v1/admin/ai-client-advisor/query-by-type/{advisorType} |
| POST   | /api/v1/admin/ai-client-advisor/query-list |
| GET    | /api/v1/admin/ai-client-advisor/query-all |
| POST   | /api/v1/admin/ai-client-rag-order/create |
| PUT    | /api/v1/admin/ai-client-rag-order/update-by-id |
| PUT    | /api/v1/admin/ai-client-rag-order/update-by-rag-id |
| DELETE | /api/v1/admin/ai-client-rag-order/delete-by-id/{id} |
| DELETE | /api/v1/admin/ai-client-rag-order/delete-by-rag-id/{ragId} |
| GET    | /api/v1/admin/ai-client-rag-order/query-by-id/{id} |
| GET    | /api/v1/admin/ai-client-rag-order/query-by-rag-id/{ragId} |
| GET    | /api/v1/admin/ai-client-rag-order/query-enabled |
| GET    | /api/v1/admin/ai-client-rag-order/query-by-knowledge-tag/{knowledgeTag} |
| GET    | /api/v1/admin/ai-client-rag-order/query-by-status/{status} |
| POST   | /api/v1/admin/ai-client-rag-order/query-list |
| GET    | /api/v1/admin/ai-client-rag-order/query-all |
| POST   | /api/v1/admin/ai-client-rag-order/file/upload |
| GET    | /api/v1/admin/ai-agent-draw/client-types-by-strategy |
| POST   | /api/v1/admin/ai-agent-draw/query-list |
| POST   | /api/v1/admin/ai-agent-draw/save-config |
| GET    | /api/v1/admin/ai-agent-draw/get-config/{configId} |
| DELETE | /api/v1/admin/ai-agent-draw/delete-config/{configId} |

---

*文档根据当前代码生成，具体 DTO 字段以 `ai-agent-station-study-api` 模块下 dto 类为准。*

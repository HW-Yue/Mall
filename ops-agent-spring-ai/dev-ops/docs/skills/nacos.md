# nacos_config — Nacos 配置与服务发现 Skill

- **Skill ID**: `nacos_config`
- **父 Agent 工具名**: `nacos_skill`
- **父工具描述**: Nacos Skill：配置读/写（写可能审批）、服务实例与服务列表。传入 task 为自然语言任务说明。
- **底层实现**: `NacosToolkit` (`skill/nacos/NacosToolkit.java`)
- **Skill 注册类**: `NacosSkillRegistry` (`skill/nacos/NacosSkillRegistry.java`)
- **子 Agent**: `NacosISubAgent` (`agent/sub/NacosISubAgent.java`)

## Function Tool 清单

### `nacos_get_config`

读取 Nacos 配置内容。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `dataId` | string | 是 | — | 配置 dataId |
| `group` | string | 否 | `DEFAULT_GROUP` | 配置分组 |

**行为**: 通过 Nacos Java SDK 调用 `configService.getConfig(dataId, group, timeout)`。返回配置内容的纯文本字符串。

**返回值示例**:
```json
{"status":"ok","key":"config","value":"server:\n  port: 8080\n"}
```

---

### `nacos_publish_config`

发布/修改 Nacos 配置。⚠️ **高危写操作**。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `dataId` | string | 是 | — | 配置 dataId |
| `group` | string | 否 | `DEFAULT_GROUP` | 配置分组 |
| `content` | string | 是 | — | 配置正文（完整 YAML/Properties/JSON 内容） |

**行为**: 通过 Nacos Java SDK 调用 `configService.publishConfig(dataId, group, content)`。成功会触发 Nacos 配置推送，影响订阅该配置的所有服务。

**审批**: **必须人工审批**（`NacosSkillRegistry.requiresApproval("nacos_publish_config") == true`）。执行时由 `ToolApprovalRuleFilter` 拦截，生成审批单并挂起。等待运维人员通过审批接口（`POST /api/v1/approvals/{id}/approve`）通过后才会真正执行。

---

### `nacos_list_instances`

查询服务的健康实例列表。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `serviceName` | string | 是 | — | 服务名（注册到 Nacos 的应用名） |
| `group` | string | 否 | `DEFAULT_GROUP` | 服务分组 |

**行为**: 通过 Nacos Java SDK 查询指定服务的实例列表。返回结果区分 `healthy` 和 `unhealthy` 实例。

**关键判据**: 空实例列表是「服务未注册、名称不一致或当前无健康实例」的关键证据。排查服务不可用时优先使用此工具确认服务存在性。

---

### `nacos_list_services`

分页列出注册的服务名。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `pageNo` | number | 否 | `1` | 页码 |
| `pageSize` | number | 否 | `100` | 每页条数 |

**行为**: 通过 Nacos Java SDK 分页查询注册中心的服务名列表。

---

### `nacos_get_services`

`nacos_list_services` 的别名，参数与行为完全一致。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `pageNo` | number | 否 | `1` | 页码 |
| `pageSize` | number | 否 | `100` | 每页条数 |

**说明**: 两个工具名指向同一底层实现（`NacosToolkit.listServices(pageNo, pageSize)`），用于兼容不同调用习惯。

## 设计原则

**服务存在性优先**（由子 Agent System Prompt 保证）：

1. 排查服务错误率/不可用时，先用 `nacos_list_instances` 查询告警中的 `application`/`serviceName`。
2. 若实例列表为空或无健康实例，直接报告「服务未注册或无健康实例」，不再继续读无关配置。
3. 只有确认服务存在且问题指向配置时，才使用 `nacos_get_config`。
4. `nacos_publish_config` 必须等待人工审批，不可在未经审批的情况下执行。

## 审批标记

| 工具名 | 需审批 |
|---|---|
| `nacos_get_config` | 否 |
| `nacos_publish_config` | **是** |
| `nacos_list_instances` | 否 |
| `nacos_list_services` / `nacos_get_services` | 否 |

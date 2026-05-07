# docker_ops — Docker 诊断 Skill

- **Skill ID**: `docker_ops`
- **父 Agent 工具名**: `docker_skill`
- **父工具描述**: Docker Skill：日志、stats、inspect、受控 exec。传入 task 为自然语言任务说明。
- **底层实现**: `DockerToolkit` (`skill/docker/DockerToolkit.java`)
- **Skill 注册类**: `DockerSkillRegistry` (`skill/docker/DockerSkillRegistry.java`)
- **子 Agent**: `DockerISubAgent` (`agent/sub/DockerISubAgent.java`)

## Function Tool 清单

### `docker_logs`

拉取指定容器的 stdout/stderr 日志。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `container` | string | 是 | — | 容器 ID 或容器名 |
| `tail` | number | 否 | `100` | 拉取最近 N 行，上限 `5000` |

**行为**: 通过 Docker Engine API 执行 `logContainerCmd`，同时拉取 stdout 和 stderr。返回文本最长 `120KB`，超出截断并追加 `\n...(truncated)`。单次调用超时 `45` 秒。

**返回值示例**:
```json
{"status":"ok","key":"logs","value":"2024-01-01T00:00:00 INFO app started..."}
```

---

### `docker_stats`

获取容器资源统计（CPU、内存、网络 IO、块 IO）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `container` | string | 是 | — | 容器 ID 或容器名 |

**行为**: 通过 Docker Engine API 执行 `statsCmd`，`withNoStream(true)` 只取一次快照。超时 `15` 秒。返回 `Statistics` 对象的 `toString()` 结果。

**返回值示例**:
```json
{"status":"ok","key":"stats","value":"com.github.dockerjava.api.model.Statistics@..."}
```

---

### `docker_inspect`

获取容器元数据（配置、状态、挂载、网络等）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `container` | string | 是 | — | 容器 ID 或容器名 |

**行为**: 通过 Docker Engine API 执行 `inspectContainerCmd`。容器不存在时返回异常信息（如 `No such container`），此结果是「服务未部署/容器名不匹配」的关键证据。

**返回值示例**:
```json
{"status":"ok","key":"inspect","value":"InspectContainerResponse[...]"}
```

---

### `docker_exec`

在容器内执行单次 shell 命令（受控 exec）。

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `container` | string | 是 | — | 容器 ID 或容器名 |
| `command` | string | 是 | — | 在容器内执行的 shell 命令 |

**行为**: 通过 Docker Engine API 创建 exec 并启动，实际执行 `sh -c <command>`。同时捕获 stdout 和 stderr。单次调用超时 `60` 秒。返回 JSON 包含 `stdout` 和 `stderr` 两个字段。

**返回值示例**:
```json
{"status":"ok","key":"exec","value":{"stdout":"...","stderr":"..."}}
```

## 设计原则

**服务存在性优先**（由子 Agent System Prompt 和 `docker_inspect` 行为共同保证）：

1. 若任务是确认服务是否部署，优先调用 `docker_inspect`，传入容器名。
2. `docker_inspect` / `docker_logs` / `docker_stats` 返回 not found、No such container 或空结果时，直接报告「Docker 未发现该服务容器」，不再继续执行无关命令。
3. 只有确认容器存在后，才查 `docker_logs` / `docker_stats`。

## 审批标记

全部工具均为 **只读操作**，无需审批。

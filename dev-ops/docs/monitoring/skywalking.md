# SkyWalking

- SkyWalking 开发环境 compose：`dev-ops/docker-compose-skywalking.yml`
- 专门用于链路追踪，不与 ELK 共用存储。

## 组件与端口

| 组件 | 宿主机端口 | 说明 |
|---|---|---|
| SkyWalking UI | `9988` | Web UI |
| OAP gRPC | `11800` | Java Agent 上报 |
| OAP HTTP | `12800` | OAP HTTP |
| SkyWalking ES | `19200` | 独立 ES 7.17 |

## 应用侧约定

- 开发 profile 默认 `yue.log.trace.provider: skywalking`
- JVM 需要增加 `-javaagent`
- `service_name` 与各服务 `spring.application.name` 对齐

## 详细接入

- 详细联调步骤、IDEA 运行方式、回退方式见 `dev-ops/SKYWALKING.md`

## 事实来源

- `dev-ops/docker-compose-skywalking.yml`
- `dev-ops/SKYWALKING.md`

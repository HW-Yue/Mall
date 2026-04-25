# DynamicTp × Nacos 配置初始化

本目录存放 **DynamicTp**（`spring.dynamic.tp`）、各服务可选 **Hikari 覆盖**（`spring.datasource.hikari.*`），以及 **拼团运行时合并配置**（`group-buy-service-runtime-dev.yml`，含 DynamicTp + Agent 等）的 YAML 模板；提供 **`init-nacos-dtp.sh`** 发布到 Nacos（`DEFAULT_GROUP`）。

> 与 Sentinel JSON 为何分目录、如何一键全发，见 [上级目录 README](../README.md)。也可直接运行 **`../init-nacos-runtime.sh`** 同时发布 YAML + Sentinel。

> 脚本会匹配并发布 `*-dtp-dev.yml`、`*-datasource-dev.yml`、`*-runtime-dev.yml` 三种文件名模式（见脚本内注释）。

## 1. 架构说明

```
┌─────────────────────┐     发布      ┌─────────────────────┐
│  init-nacos-dtp.sh   │ ───────────► │  Nacos Config       │
│  （本目录 YAML）      │             │  Group=DEFAULT_GROUP │
└─────────────────────┘             └──────────┬──────────┘
                                            │ refresh
                                            ▼
                                   ┌─────────────────────┐
                                   │ mall / order / …     │
                                   │ spring.config.import │
                                   │ optional:nacos:…     │
                                   └─────────────────────┘
```

- **DynamicTp**：DataId 一般为 `*-dtp-dev.yml`，与 mall / order / seckill 等 `classpath:nacos/*-dtp-dev.yml` + `optional:nacos:…` 对齐；**拼团服务**合并了 Agent 等项，使用 **`group-buy-service-runtime-dev.yml`**。
- **Hikari 热调**：DataId 为 `*-datasource-dev.yml`，仅包含需要覆盖的 `spring.datasource.hikari` 字段；应用内 `HikariPoolDynamicRefresher` 在配置变更时应用 `maximum-pool-size` / `minimum-idle`。

## 2. 覆盖的 DataId

| DataId | 说明 |
|--------|------|
| `mall-dtp-dev.yml` | mall DynamicTp + tomcat-tp |
| `mall-datasource-dev.yml` | mall Hikari 可选覆盖 |
| `order-service-dtp-dev.yml` | 订单服务 DynamicTp |
| `order-service-datasource-dev.yml` | 订单服务 Hikari 可选覆盖 |
| `seckill-service-dtp-dev.yml` | 秒杀服务 DynamicTp |
| `seckill-service-datasource-dev.yml` | 秒杀服务 Hikari 可选覆盖 |
| `group-buy-service-runtime-dev.yml` | 拼团服务运行时（DynamicTp + Agent / Hikari / 日志 / app.agent / Feign） |
| `pay-datasource-dev.yml` | pay 服务 Hikari 可选覆盖（**ops-agent HikariTuningStrategy 目标**） |
| `shared-mysql-tuning.yml` | MySQL 实例级调优共享档案（**ops-agent MySqlTuningStrategy 目标**，application=shared） |

所有配置 **Group = `DEFAULT_GROUP`**，namespace 默认 **`public`**（与 `spring.config.import` 中未写 namespace 时的行为一致）。

## 3. 使用步骤

### 3.1 启动 Nacos（Docker Compose）

```bash
cd mall/docs/dev-ops
docker compose -f docker-compose-environment.yml up -d mysql nacos
# 或顺带拉起其它依赖：docker compose -f docker-compose-environment.yml up -d
```

待 Nacos 就绪后，在宿主机执行下方 **3.2 批量发布**（`docker-compose-environment.yml` 不再包含自动跑 `init-nacos-dtp.sh` 的一次性容器）。

控制台（Nacos 3.x）：<http://100.86.250.112:8080>（具体端口以 compose 为准）。

### 3.2 批量发布（宿主机）

`init-nacos-dtp.sh` 会发布本目录下所有 `*-dtp-dev.yml`、`*-datasource-dev.yml`、`*-runtime-dev.yml`（含 `group-buy-service-runtime-dev.yml`）。

```bash
cd mall/docs/dev-ops/nacos/dtp-config
chmod +x init-nacos-dtp.sh

# 未开启鉴权
NACOS_ADDR=100.86.250.112:8848 ./init-nacos-dtp.sh

# Nacos 开启鉴权（示例：NACOS_AUTH_ENABLE=true）
NACOS_ADDR=100.86.250.112:8848 NACOS_USER=nacos NACOS_PASS=nacos ./init-nacos-dtp.sh
```

### 3.3 验证

在 Nacos 控制台「配置管理」中搜索 Group `DEFAULT_GROUP`，应能看到上述 DataId。启动业务服务后，修改 Nacos 中线程池或 Hikari 数值，应在秒级内生效（需服务已配置 `refreshEnabled=true` 的 optional import）。

## 4. 与源码的对应关系

各微服务 **权威配置** 仍位于：

| 服务 | classpath 模板路径 |
|------|-------------------|
| mall | `mall/mall-app/src/main/resources/nacos/` |
| order-service | `order-service/order-service-app/src/main/resources/nacos/` |
| seckill-service | `seckill-service/seckill-service-app/src/main/resources/nacos/` |
| group-buy-service | `group-buy-service/group-buy-service-app/src/main/resources/nacos/` |

修改线程池默认值时，建议 **先改各服务 `src/main/resources/nacos/*-dtp-dev.yml`（拼团服务为 `*-runtime-dev.yml`），再同步本目录同名文件后执行脚本**，避免两处长期不一致。

> **ops-agent 相关 DataId 首次上传**：`pay-datasource-dev.yml` 已被 `init-nacos-dtp.sh` 的 `*-datasource-dev.yml` 通配覆盖（首次执行脚本即会发布）；`shared-mysql-tuning.yml` 文件名不在脚本通配内，首次请手动用 Nacos 3.x **Admin API** 发布（v1/cs/configs 已下线，一定要走 v3）：
>
> ```bash
> NACOS_ADDR=100.86.250.112:8848
> TOKEN=$(curl -s -X POST "http://${NACOS_ADDR}/nacos/v3/auth/user/login" \
>         -d "username=nacos&password=nacos" \
>         | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
> content=$(cat mall/docs/dev-ops/nacos/dtp-config/shared-mysql-tuning.yml)
> curl -sS -X POST "http://${NACOS_ADDR}/nacos/v3/admin/cs/config" \
>   -H "accessToken: ${TOKEN}" \
>   --data-urlencode "namespaceId=public" \
>   --data-urlencode "groupName=DEFAULT_GROUP" \
>   --data-urlencode "dataId=shared-mysql-tuning.yml" \
>   --data-urlencode "type=yaml" \
>   --data-urlencode "content=${content}"
> ```
>
> 发布成功后，ops-agent 处理 MySQL / Hikari 告警时 `getConfig` 就能拉到初值，避免 Agent 凭空生成 YAML。

## 5. 常见问题

- **发布后服务未拉到配置**：检查 `spring.config.import` 是否包含 `optional:nacos:<DataId>?group=DEFAULT_GROUP&refreshEnabled=true`，且 Nacos 地址与 `spring.cloud.nacos.config.server-addr` 一致。
- **Hikari 改了不生效**：确认变更的 key 以 `spring.datasource.hikari.` 开头；当前监听器仅对 `maximum-pool-size`、`minimum-idle` 调用运行时 setter。
- **与 Sentinel 脚本区别**：Sentinel 规则使用 Group `SENTINEL_GROUP`、类型 `json`；本脚本使用 Group `DEFAULT_GROUP`、类型 `yaml`。
- **`group-buy-service-runtime-dev.yml` 里 `app.agent.feign.order-service.*`**：拼团服务用其配置 OpenFeign 超时，但 Feign 的 `Request.Options` 在**启动时**从配置绑定；在 Nacos 中修改这两项后需**重启（或滚动发布）**拼团实例才会生效（与 Hikari / 日志热更行为不同）。详见 `group-buy-service/docs/AGENT_NACOS_RUNTIME.md` §6。

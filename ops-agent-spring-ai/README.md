# Ops Agent Spring AI

基于 **Spring AI Alibaba** 的运维技能模块：进程内 **七域** 工具（Docker / MySQL / RocketMQ / Prometheus 指标 / Elasticsearch / Redis / Nacos，无 MCP），支持 **SOP 驱动父 Orchestrator ReAct** 告警路径与 **deterministic** 逐步 `SopOrchestrator` 兜底；Nacos 发布走审批（内存队列）。

## 运行

```bash
# 优先 DASHSCOPE_API_KEY；未导出时使用 application.yml 中的默认占位
export DASHSCOPE_API_KEY=your-key
mvn -pl ops-agent-spring-ai spring-boot:run
```

默认端口 **2322**。**Prometheus**：启动后可访问 `http://<host>:2322/actuator/prometheus`（与 mall 相同，供 Nacos SD 的 `nacos-sd-spring-boot` 等 job 抓取）。

**Web 控制台**：源码位于 [`src/main/resources/static/`](src/main/resources/static/)（`index.html`、`approvals.html`、`tools.html` 与 `css/`、`js/`、`config.js`），随 `mvn package` 进入 `classpath:/static` 并由服务根路径提供。默认经 **Gateway** `http://<网关>:8090/gw/api/v1/ops-ai`（需 Nacos 中已注册本服务）；直连时用 `?api=http://127.0.0.1:2322/api/v1`。

**改完页面仍像旧的？** 进程读的是 `target/classes/static/`（或 JAR 内），不是只保存 `src/.../static` 就自动更新。请在本模块执行 `mvn compile`（或 **Build → Rebuild Project**）再启服务；对照 `target/classes/static/` 下文件时间戳。另可对浏览器 **Ctrl+Shift+R** 强刷。已配置 `spring.web.resources.cache.period: 0` 减轻 CSS/JS 强缓存，但「未编译进 target」时重启也没用。

## 告警与 SOP

- `ops-ai.alert.mode=react`（默认）：匹配 SOP 规则后，将 **`sop-markdown` 字段中的正文（建议纯文本）+ 告警上下文** 交给 **OrchestratorReactAgent**，父 Agent 通过 7 个子域工具名（如 `docker_ops`、`metrics_ops`，与 skill 域 id 一致）委派子域（子域内层 ReAct）。
- `ops-ai.alert.mode=deterministic`：仍使用 **SopOrchestrator** 逐步执行规则中的 `steps`（`direct_tool` / `delegate_subagent`）。
- 规则字段 **`sop-markdown`**：配置键名未改，内容为 **标准作业程序正文**，用纯文本即可（分段可用【一】【二】或 1. 2.）；`steps` 在 deterministic 模式下继续使用。

## 子域与 Skill 名

| Skill | 说明 |
|-------|------|
| `docker_ops` | 日志、stats、inspect、受控 exec |
| `mysql_inspect` | 只读 JDBC：会话、状态、锁、慢查询表 |
| `rocketmq_inspect` | NameServer 可达时的 Topic / 消费统计（只读 Admin） |
| `metrics_ops` | 统一 Prometheus：`sentinel_metrics` / `dynamictp_metrics` / `jvm_metrics` / `business_metrics`（均可传 `promql` 覆盖默认） |
| `elasticsearch_ops` | `es_*`；可选 **`cluster`**：`logs`（默认，nexus-* 日志）或 **`skywalking`**（`sw_segment-*`、`sw_metrics-*` 等）；独立 SW 集群时配 **`OPS_AI_SKYWALKING_ES_URL`** |
| `redis_inspect` | INFO、SLOWLOG、CLIENT LIST、内存段、GET / SCAN 采样；连接可用 `ops-ai.redis.uri` 或 host/port |
| `nacos_config` | 配置读/写（写需审批）、`nacos_list_instances`、`nacos_list_services` / **`nacos_get_services`**（别名） |

## 主要 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/chat/stream` | SSE 流式对话（body: `{"message":"..."}`）；`ops-ai.chat.mode=react` 时走父 Agent 委派 |
| POST | `/api/v1/chat/react` | 父 Agent ReAct + 子域工具（阻塞 JSON `{"reply":"..."}`） |
| POST | `/api/v1/tools/execute` | 执行工具 `{"skill","tool","args"}` |
| POST | `/api/v1/alert/receive` | Alertmanager webhook；`react` 返回 `reply`，`deterministic` 返回 `steps` |
| GET | `/api/v1/approvals/pending` | 待审批列表 |
| POST | `/api/v1/approvals/{id}/approve` | 审批通过并执行 |
| POST | `/api/v1/approvals/{id}/reject` | 拒绝 |

## 配置与联调（mall dev-ops）

见 `src/main/resources/application.yml`：`ops-ai.*` 与 `spring.ai.dashscope.*`。与 **mall** `docs/dev-ops` 联调时，宿主机访问可参照：MySQL `13306`、Redis `16379`、Nacos `8848`、Elasticsearch `9200`、Prometheus `9090`、RocketMQ NameServer `9876`；Docker 默认 **`unix:///var/run/docker.sock`**（Linux）。

### Nacos 注册发现（与 mall-app 一致）

- **依赖**：`spring-cloud-dependencies` **2024.0.1**（对齐 Spring Boot **3.4.x**）+ `spring-cloud-alibaba-dependencies` **2023.0.3.3**；`spring-cloud-starter-alibaba-nacos-discovery` + `nacos-config`（`import-check.enabled=false`，未配 `spring.config.import=nacos:...` 亦可启动）。
- **配置**：`spring.cloud.nacos.discovery`（`server-addr`、`ip`、`namespace`、账号等），服务名即 **`spring.application.name`**（默认 **`ops-agent-spring-ai`**）。可按环境设置 **`NACOS_SERVER_ADDR` / `NACOS_DISCOVERY_IP` / `NACOS_NAMESPACE`** 等。
- **测试**：`src/test/resources/application.yml` 中关闭 `discovery.enabled`，避免 `mvn test` 强依赖注册中心。
- **说明**：`ops-ai.nacos.*` 仍为 **NacosToolkit** 运维调用地址；若与注册中心一致，请保持与 `spring.cloud.nacos` 相同 `server-addr`（或统一用环境变量）。

### ELK 与 SkyWalking traceId（与 mall 一致）

- **依赖**：本仓库 `Dependencies/common-log-starter` 需先装入本地 Maven：`mvn -f Dependencies/common-log-starter/pom.xml install -DskipTests`
- **日志**：`logging.config=logback-spring.xml`，引入与 mall 相同的 `com/yue/common/log/logback/base.xml`（控制台 + `./data/log` 滚动文件，格式中带 `%X{trace-id}`）。
- **SkyWalking → 日志**：`yue.log.trace.provider=skywalking` 时注册 `SkyWalkingTraceMdcFilter`，把 Agent 上下文中的 **traceId** 写入 MDC（默认键 **`trace-id`**）。**请使用与 OAP 匹配的 Java Agent**（见 `mall/docs/dev-ops/docker-compose-skywalking.yml` 说明），否则 MDC 中可能无有效 traceId。
- **Logstash / Kibana**：启动 mall 的 `docs/dev-ops/docker-compose-elk.yml`（TCP `4560`、`json_lines`）。默认 **`LOGSTASH_ENABLED=true`**（与 mall `application-dev` 一致），`common-log-starter` 会向 Root 挂载 `LogstashTcpSocketAppender`；无 ELK 时设 **`LOGSTASH_ENABLED=false`**。JSON 中带 **`service":"ops-agent-spring-ai"`**，ES 索引 **`nexus-ops-agent-spring-ai-YYYY.MM.dd`**（与 `mall/docs/dev-ops/logstash/logstash.conf` 一致）。Kibana 可按 **`mdc.trace-id`** 与 SkyWalking Trace Id 对查。
- **SkyWalking 存储 ES（`sw_*`）**：`mall/docs/dev-ops/docker-compose-skywalking.yml` 里 OAP 使用**独立** `elasticsearch-skywalking`（宿主机常见 **`19200`**，与 ELK **`9200`** 分离）。**不设 SkyWalking 专用 Kibana**：链路用 **SkyWalking UI**，`sw_*` 需原始文档时用 **ES REST**（或本服务 **`elasticsearch_ops`** + `cluster=skywalking`）。本服务在 **`ops-ai.elasticsearch.skywalking`** 配第二地址；留空则与 **`ops-ai.elasticsearch.base-url`** 相同。工具示例：`{"skill":"elasticsearch_ops","tool":"es_indices","args":{"cluster":"skywalking"}}`（索引名以列表为准，如 `sw_segment-*`）。

### 客户端与服务端兼容（验收参考）

| 客户端 / 模块 | 建议对齐的服务端 |
|---------------|------------------|
| `spring-boot-starter-parent` 3.4.x | 运行环境 JDK 21 |
| `nacos-client` 2.4.x（BOM 管理） | Nacos Server 2.x / 3.x（如 mall 3.2.0） |
| `mysql-connector-j`（Boot 管理） | MySQL 8.x |
| `lettuce-core`（Boot 管理） | Redis 6/7 |
| `docker-java` 3.4.x | Docker Engine API 兼容版本 |
| `rocketmq-tools` 5.3.x | RocketMQ 5.x Broker/NameServer（Admin 只读） |
| ES：`RestClient` HTTP | Elasticsearch 8/9 dev（如 9200 无安全） |
| Prometheus HTTP `/api/v1/query` | Prometheus 2.x |

**勿将生产口令提交入库**；生产用环境变量覆盖。

### 七域连通性冒烟（可选）

不调用大模型，只测 **Prometheus / ES / MySQL / Redis / Nacos / Docker / RocketMQ** 在 `application.yml` 里配置的地址能否连通：

```bash
export OPS_AGENT_INTEGRATION_SMOKE=true
mvn -pl ops-agent-spring-ai test -Dtest=BackendConnectivitySmokeIT
```

未设置 `OPS_AGENT_INTEGRATION_SMOKE=true` 时该类整表跳过，不影响普通 `mvn test`。RocketMQ 在 `ops-ai.rocketmq.enabled=false` 时对应用例自动 `assume` 跳过。

冒烟会连 `application.yml` 里的真实地址：本机未起依赖、MySQL 账号/权限不对、Docker socket 不可用时对应用例会失败，需先对齐 mall dev-ops 或改本地配置。

覆盖范围：每个 `*Toolkit` 的 **public 方法** 都会跑到（Prometheus 仅 `queryInstant` 一项）。**Nacos `publishConfig` 有写副作用，冒烟里不测**，需人工验证。可选环境变量见 `BackendConnectivitySmokeIT` 类注释（Docker 容器 ID、Nacos dataId/服务名、MQ group/topic 等）；未设则跳过需要业务参数的用例，不判失败。

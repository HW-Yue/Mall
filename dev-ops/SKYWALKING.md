# SkyWalking 开发接入说明

版本与工程根目录下 `Dependencies/common-log-starter` 中 **`skywalking.version`（当前 9.6.0）**、本目录 `docker-compose-skywalking.yml` 中 **OAP/UI 镜像**、本机 **Java Agent 压缩包** 保持一致。

## 1. 起基础设施

1. `docker compose -f docker-compose-environment.yml up -d`（`nexus-devops` 网络）
2. `docker compose -f docker-compose-elk.yml up -d`（Elasticsearch + Logstash + Kibana）
3. `docker compose -f docker-compose-skywalking.yml up -d`（OAP + UI，存储指向同一套 ES）
4. UI：`http://100.86.250.112:9988`；Agent 上报 OAP 地址：`100.86.250.112:11800`（已映射到宿主机）

## 2. 应用侧配置

**开发 profile**：各服务 `src/main/resources/application-dev.yml` 已默认 `yue.log.trace.provider: skywalking`（与 `spring.application.name` 联动的说明见下）。

非 dev 或生产环境请在 Nacos / `application-*.yml` 中自行显式设置：

```yaml
yue:
  log:
    trace:
      provider: skywalking
```

## 2.1 IDEA 一键跑（推荐）

- 将 Java Agent 解压到仓库 [dev-tools/skywalking/README.md](../dev-tools/skywalking/README.md) 要求的路径，或只改 Run 里 `-javaagent` 的绝对路径。
- 在 IDEA 运行配置下拉里选择以 **`_SkyWalking`** 结尾的配置（项目根 [`.run/`](../../../.run/) 已预置 6 个服务），`service_name` 与各服务 `spring.application.name` 对齐（如 pay 为 `pay-service`，mall 为 `mall-service`）。
- 若提示 **Module 未找到**，在「Edit Configurations…」中把 *Use classpath of module* 选成对应子模块（如 `mall.mall-app.main`），以本机实际模块名为准，不必强求 `Nexus.*` 前缀是否一致。

## 3. JVM 增加 Java Agent

从 [SkyWalking Downloads](https://skywalking.apache.org/downloads/) 下载与 **9.6.0** 一致的 `apache-skywalking-java-agent-9.6.0.tgz`，解压后示例：

```text
-javaagent:/绝对路径/skywalking-agent/skywalking-agent.jar
-Dskywalking.collector.backend_service=100.86.250.112:11800
-Dskywalking.agent.service_name=mall-service
```

**网关、各微服务**使用不同的 `service_name`，便于拓扑区分。

## 4. 验证

1. 打请求后，SkyWalking UI 中出现服务与调用链；Elasticsearch 中除日志索引外出现 `sw_*` 等由 OAP 管理的索引。
2. 同一次请求在 Kibana 日志中 `trace-id` 字段与 UI 中 Trace 的 id 可对应（未采样时可能无 id）。

## 5. 回退

将 `yue.log.trace.provider` 设为 `legacy` 或删除该配置，并去掉 JVM 的 `-javaagent` 参数即可恢复自研 TraceId 行为。

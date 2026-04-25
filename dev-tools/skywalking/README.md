# 本地开发用 SkyWalking Java Agent（9.6.0）

> **版本说明**：OAP/UI 已升级至 10.4.0，Java Agent 9.6.0 仍是最新稳定版，两者向后兼容。

1. 从官网下载并解压 [apache-skywalking-java-agent-9.6.0.tgz](https://skywalking.apache.org/downloads/) 到**本目录下**，保证存在路径：

   `dev-tools/skywalking/apache-skywalking-java-agent-9.6.0/skywalking-agent/skywalking-agent.jar`

   （`*.jar` 已被仓库 `.gitignore` 忽略，不会提交。）

2. 在 IDEA 的 **Run/Debug Configurations** 里，可直接选用项目根下 `.run/` 中预置的 `*_SkyWalking` 配置；其中 `-javaagent` 使用宏 `$PROJECT_DIR$`，需满足上面目录结构。

3. 若你解压到别处：在 Run 配置中把 **VM options** 里 `-javaagent:...` 整段改为你的 `skywalking-agent.jar` 绝对路径即可；或在本目录做软链/拷贝到上述约定路径。

4. OAP 地址默认为 `100.86.250.112:11800`（与 `docker-compose-skywalking.yml` 端口映射一致）。

## GenAI 监控（ops-agent）

ops-agent 通过 **OpenTelemetry OTLP** 直接向 OAP 10.4 上报 GenAI Span，不依赖 Java Agent 的自动插件（AgentScope 尚无官方插件）。

- OTLP endpoint: `http://100.86.250.112:11800`（与 SkyWalking gRPC 共享端口）
- 配置见 `ops-agent/src/main/resources/application.yml` → `ops-agent.otlp.*`
- SkyWalking OAP 的 `receiver-otel` 模块默认启用 `otlp-traces`，无需额外配置

**验证**：在 SkyWalking UI → GenAI 菜单下应能看到 `ops-agent` 服务的 LLM 调用统计（CPM、延迟、Token 用量）。

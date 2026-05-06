# 配置文档

`dev-ops/docs/config/` 维护本仓库运行时配置、配置中心接入和动态刷新相关的正式说明。

## 文档导航

- [配置更新总览](./dynamic-refresh.md)
- [Sentinel 规则动态更新](./sentinel-rules.md)
- [Hikari 连接池动态更新](./hikari-refresh.md)
- [DynamicTp / Tomcat 动态更新](./dynamic-tp-refresh.md)
- [运行时属性热更新](./runtime-properties-refresh.md)

## 维护规则

- 配置动态更新、Nacos DataId 规划、运行时刷新边界统一维护在这里。
- 事实来源优先以各服务 `application-{profile}.yml`、`src/main/resources/nacos/*.yml`、`HikariPoolDynamicRefresher`、`@RefreshScope` 配置类为准。
- 如果某个服务新增了可热更新配置，除了改代码和 Nacos DataId，还要同步更新本目录文档与 `AGENTS.md` / `CLAUDE.md` 索引。

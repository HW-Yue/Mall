# DynamicTP

- DynamicTP 配置统一放在 `dev-ops/nacos/dtp-config/`，属于运行时 YAML 配置的一部分。
- 指标采集方式依赖 Micrometer，开发环境约定启用 `collector-types: [micrometer, internal_logging]`。

## 目录

- `dynamic-tp/`：线程池配置
- `datasource/`：Hikari 等数据源动态覆盖
- `runtime/`：日志、Agent、自定义线程池等运行时项
- `shared/`：共享调优模板

## 关键约定

- DynamicTP 使用 `DEFAULT_GROUP`
- 配置类型是 YAML
- 通过 `spring.config.import` + `optional:nacos:...&refreshEnabled=true` 加载
- 不和 Sentinel JSON 规则混用

## 监控关系

- DTP 告警使用 `{application="xxx"}`
- 线程池指标进入 Prometheus 后，由 `ops-agent-spring-ai` 按 `dynamictp` 类告警处理

## 相关入口

- `dev-ops/nacos/README.md`
- `dev-ops/nacos/dtp-config/README.md`
- `ops-agent-spring-ai/dev-ops/docs/alerts/sop-tool-mapping.md`

## 事实来源

- `dev-ops/nacos/README.md`
- `AGENTS.md` / `CLAUDE.md` 现有监控段落

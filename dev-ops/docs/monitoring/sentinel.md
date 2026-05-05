# Sentinel

- Sentinel 规则不和普通运行时 YAML 混放，统一走 `dev-ops/nacos/sentinel-rules/`。
- Nacos 中使用 `SENTINEL_GROUP`，数据格式是 JSON，不走 Spring Boot 主配置树。

## 规则组织

规则目录按类型拆分：

- `flow/`
- `degrade/`
- `param-flow/`
- `system/`
- `authority/`
- `gateway/`

文件名仍然就是 Nacos DataId，例如 `{app}-flow-rules.json`。

## 关键约定

- `resource` 使用纯 URI，不带 HTTP Method 前缀，例如 `/api/v1/order/create_order`
- `grade`：
  - `0` = 并发线程数
  - `1` = QPS
- `controlBehavior`：
  - `0` = 快速失败
  - `1` = Warm Up
  - `2` = 匀速排队

## 标签规范

- Sentinel 告警使用 `{app="xxx"}`
- 不与 JVM / HTTP / DTP 的 `{application="xxx"}` 混用

## 运行与发布

- Dashboard 仅用于观测，不是规则真源
- 规则发布脚本：`dev-ops/nacos/sentinel-rules/init-nacos-rules.sh`
- 一键发布入口：`dev-ops/nacos/init-nacos-runtime.sh`

## 相关入口

- 规则发布与目录说明：`dev-ops/nacos/sentinel-rules/README.md`
- 动态配置总入口：`dev-ops/nacos/README.md`
- 网关 Sentinel fallback 文档：`dev-ops/docs/api/details/springcloud-gateway.md`

## 事实来源

- `dev-ops/nacos/README.md`
- `dev-ops/nacos/sentinel-rules/README.md`
- `AGENTS.md` / `CLAUDE.md` 现有监控段落

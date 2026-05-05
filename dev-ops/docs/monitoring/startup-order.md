# 启动顺序

本仓库监控与可观测性相关组件推荐按以下顺序启动：

1. `docker-compose-environment.yml`
   - 创建 `nexus-devops` 网络
   - 启动 MySQL、Redis、Nacos、Sentinel Dashboard 等基础环境
2. `docker-compose-rocketmq.yml`
3. `docker-compose-elk.yml`
4. `docker-compose-mcp.yml`
5. `docker-compose-exporters.yml`
6. `docker-compose-grafana.yml`
7. `docker-compose-skywalking.yml`

## 说明

- `docker-compose-grafana.yml` 依赖前面已存在的 `nexus-devops` 网络和 Nacos HTTP SD
- `docker-compose-exporters.yml` 依赖 MySQL、Redis、RocketMQ 已可访问
- `docker-compose-skywalking.yml` 是独立链路追踪栈，可在 ELK 后单独启动

## 事实来源

- `AGENTS.md` / `CLAUDE.md` 现有监控段落
- `dev-ops/docker-compose-grafana.yml`
- `dev-ops/docker-compose-exporters.yml`
- `dev-ops/docker-compose-elk.yml`
- `dev-ops/docker-compose-skywalking.yml`

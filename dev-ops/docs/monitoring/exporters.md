# Exporters

- 本仓库单独维护 `mysqld-exporter`、`redis-exporter`、`rocketmq-exporter`。
- 统一 compose：`dev-ops/docker-compose-exporters.yml`

## 统一约定

- 统一网络：`nexus-devops`
- 统一标签：`application=shared`
- 分类标签：
  - MySQL：`category=mysql`
  - Redis：`category=redis`
  - RocketMQ：`category=rocketmq`

## 端口

| Exporter | 宿主机端口 | 说明 |
|---|---|---|
| `mysqld-exporter` | `9104` | MySQL 实例指标 |
| `redis-exporter` | `9121` | Redis 实例指标 |
| `rocketmq-exporter` | `5557` | RocketMQ Broker 指标 |

## 依赖关系

- 启动前提：
  - `docker-compose-environment.yml` 已启动 MySQL、Redis，并创建 `nexus-devops`
  - `docker-compose-rocketmq.yml` 已启动 `rmq-namesrv`
- Prometheus 抓取配置在 `dev-ops/prometheus/prometheus.yml`

## 事实来源

- `dev-ops/docker-compose-exporters.yml`
- `dev-ops/prometheus/prometheus.yml`

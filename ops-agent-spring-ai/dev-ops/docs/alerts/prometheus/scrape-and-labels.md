# Prometheus 抓取与标签

**权威配置**：`mall/docs/dev-ops/prometheus/prometheus.yml`  
**规则文件**：`mall/docs/dev-ops/prometheus/alert_rules.yml`

## 1. 全局

- `scrape_interval: 15s`
- `evaluation_interval: 15s`
- `rule_files: [/etc/prometheus/alert_rules.yml]`（容器内路径；宿主机对应 `mall/docs/dev-ops/prometheus/alert_rules.yml`）

## 2. Job：`nacos-sd-spring-boot`

| 项 | 值 |
|----|-----|
| `metrics_path` | `/actuator/prometheus` |
| SD | `http_sd_configs.url: http://nacos:8848/nacos/prometheus` |
| `refresh_interval` | 30s |

### Relabel（重要）

将目标地址中的 `127.0.0.1:(port)` 替换为 `host.docker.internal:${1}`，使 **运行在 Docker 内的 Prometheus** 能访问 **宿主机上监听** 的业务实例（避免「Nacos 注册成本机回环，容器内抓不到」）。

## 3. Job：`mysqld-exporter`

| 项 | 值 |
|----|-----|
| `targets` | `mysqld-exporter:9104` |
| 附加 labels | `application: shared`，`category: mysql` |

## 4. Job：`redis-exporter`

| 项 | 值 |
|----|-----|
| `targets` | `redis-exporter:9121` |
| 附加 labels | `application: shared`，`category: redis` |

## 5. Job：`rocketmq-exporter`

| 项 | 值 |
|----|-----|
| `targets` | `rocketmq-exporter:5557` |
| 附加 labels | `application: shared`，`category: rocketmq` |

## 6. 标签在告警规则中的使用

- **业务 Micrometer**：常见 **`application`**（Spring 应用名）、Sentinel 规则中同时使用 **`app`**（SentinelMetricsBinder）与注释中说明的 **`application`**（DTP 等）。
- **Exporter 全局**：显式打上 **`application: shared`** + **`category: mysql|redis|rocketmq`**，便于 Agent 按域路由（与 `ops-agent` 策略配置一致）。

## 7. 相关文档

- 告警分组目录：[`../by-category/README.md`](../by-category/README.md)
- Alertmanager：[`../alertmanager/webhook-and-routing.md`](../alertmanager/webhook-and-routing.md)

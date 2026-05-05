# Nacos 运行时 YAML 配置

本目录统一放置五个业务服务的运行时 YAML 模板，并按职责拆成 4 类：

- `dynamic-tp/`：`spring.dynamic.tp.*` / `tomcat-tp`
- `datasource/`：`spring.datasource.hikari.*`
- `runtime/`：日志、`app.agent.*`、Feign、自定义线程池等非 Hikari / 非 DynamicTp 参数
- `shared/`：跨服务共享配置

> 脚本 `init-nacos-dtp.sh` 会递归发布 `*-dtp-dev.yml`、`*-datasource-dev.yml`、`*-runtime-dev.yml` 到 Nacos `DEFAULT_GROUP`。

## 目录结构

```text
dtp-config/
├── dynamic-tp/
│   ├── mall-service-dtp-dev.yml
│   ├── order-service-dtp-dev.yml
│   ├── group-buy-service-dtp-dev.yml
│   └── seckill-service-dtp-dev.yml
├── datasource/
│   ├── mall-service-datasource-dev.yml
│   ├── order-service-datasource-dev.yml
│   ├── group-buy-service-datasource-dev.yml
│   ├── seckill-service-datasource-dev.yml
│   └── pay-service-datasource-dev.yml
├── runtime/
│   ├── group-buy-service-runtime-dev.yml
│   └── pay-service-runtime-dev.yml
├── shared/
│   └── shared-mysql-tuning.yml
├── init-nacos-dtp.sh
└── README.md
```

## 服务与 DataId 对应

| 服务 | DynamicTp | Datasource | Runtime |
|------|-----------|------------|---------|
| `mall-service` | `mall-service-dtp-dev.yml` | `mall-service-datasource-dev.yml` | 无 |
| `order-service` | `order-service-dtp-dev.yml` | `order-service-datasource-dev.yml` | 无 |
| `group-buy-service` | `group-buy-service-dtp-dev.yml` | `group-buy-service-datasource-dev.yml` | `group-buy-service-runtime-dev.yml` |
| `seckill-service` | `seckill-service-dtp-dev.yml` | `seckill-service-datasource-dev.yml` | 无 |
| `pay-service` | 无 | `pay-service-datasource-dev.yml` | `pay-service-runtime-dev.yml` |

说明：

- `group-buy-service-runtime-dev.yml` 现在只保留日志、`app.agent.*`、Feign 超时，不再混放 DynamicTp / Hikari。
- `pay-service-runtime-dev.yml` 放的是 `thread.pool.executor.config` 这类自定义线程池参数；它不是 DynamicTp。
- datasource 文件均只承载 `spring.datasource.hikari.*` 可调项，基础 JDBC 地址、用户名、密码仍保留在各服务 `application-*.yml`。

## 导入约定

- `dynamic-tp`：一般由 `classpath:nacos/*.yml` 提供本地默认值，并通过 `optional:nacos:` 同名 DataId 覆盖
- `datasource`：通常只走 `optional:nacos:` 覆盖，本地默认值保留在 `application-*.yml`
- `runtime`：对需要本地默认模板的服务，也走 `classpath:nacos/*.yml` + `optional:nacos:` 同名覆盖

## 常见参数

### DynamicTp

- `core-pool-size`：常驻线程数
- `maximum-pool-size`：线程上限
- `queue-capacity`：队列容量
- `rejected-handler-type`：拒绝策略

### Hikari

- `maximum-pool-size`：最大连接数
- `minimum-idle`：最小空闲连接数
- `connection-timeout`：获取连接超时
- `max-lifetime`：连接生命周期

### Runtime

- `logging.level.*`：日志级别，通常可热更
- `app.agent.*`：Agent 运行时参数，按 `@RefreshScope` 生效
- `app.agent.feign.*`：Feign 超时，通常需要重启实例才会完全生效
- `thread.pool.executor.config.*`：`pay-service` 自定义线程池参数，需重启或滚动发布后生效

## 发布

```bash
cd dev-ops/nacos/dtp-config
NACOS_ADDR=100.86.250.112:8848 ./init-nacos-dtp.sh
```

若开启鉴权：

```bash
NACOS_ADDR=100.86.250.112:8848 NACOS_USER=nacos NACOS_PASS=nacos ./init-nacos-dtp.sh
```

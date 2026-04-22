# Nacos 运行时配置（YAML + Sentinel）

这里放的是**运行期可调**的配置模板与发布脚本：线程池、数据源、Agent、以及 Sentinel 流控规则等。概念上都属于「runtime」，但仓库里分成两块，原因**不是**业务上非要拆开，而是 **Spring / Nacos 的接入方式不同**：

| 维度 | `dtp-config/`（YAML） | `sentinel-rules/`（JSON） |
|------|------------------------|---------------------------|
| **Nacos Group** | `DEFAULT_GROUP` | **`SENTINEL_GROUP`**（与 `application-*.yml` 里 `spring.cloud.sentinel.datasource.*.nacos.groupId` 一致） |
| **配置类型** | `type=yaml` | **`type=json`**（Sentinel 数据源要求 JSON） |
| **应用如何加载** | `spring.config.import` + `optional:nacos:...&refreshEnabled=true` | **Sentinel 扩展**：按 `dataId` + `groupId` 拉取，不走 Spring Boot 主配置树 |
| **文件形态** | `*-dtp-dev.yml`、`*-datasource-dev.yml`、`*-runtime-dev.yml` | `*-rules.json`（如 `group-buy-service-flow-rules.json`） |

因此**不能**把 Sentinel 规则塞进 `group-buy-service-runtime-dev.yml` 里当一段 YAML 用——除非改代码，让 Sentinel 从 YAML 解析或从 `DEFAULT_GROUP` 读，那会和官方 `sentinel-datasource-nacos` 的约定不一致，维护成本高。

若你希望「运维只跑一次脚本」，用仓库根下 **`init-nacos-runtime.sh`**：内部依次调用 `dtp-config/init-nacos-dtp.sh` 与 `sentinel-rules/init-nacos-rules.sh`。

## 子目录

- **[dtp-config](./dtp-config/README.md)**：DynamicTp、Hikari 覆盖、拼团 `group-buy-service-runtime-dev.yml` 等 YAML。
- **[sentinel-rules](./sentinel-rules/README.md)**：各服务 `*-rules.json`，发布到 `SENTINEL_GROUP`。

## 一键发布（推荐）

在宿主机（或 CI）中：

```bash
cd mall/docs/dev-ops/nacos
chmod +x init-nacos-runtime.sh
NACOS_ADDR=127.0.0.1:8848 ./init-nacos-runtime.sh
# 若 Nacos 开启鉴权：
# NACOS_ADDR=127.0.0.1:8848 NACOS_USER=nacos NACOS_PASS=nacos ./init-nacos-runtime.sh
```

仍可按需单独执行 `dtp-config/init-nacos-dtp.sh` 或 `sentinel-rules/init-nacos-rules.sh`。

# Nacos 运行时配置（YAML + Sentinel）

这里放的是**运行期可调**的配置模板与发布脚本：线程池、数据源、Agent、以及 Sentinel 流控规则等。

为了避免一堆 YAML / JSON 平铺在同一级目录里，现在按“**配置用途**”和“**规则类型**”拆了子目录；但 **Nacos DataId 仍然等于文件名本身**，所以线上订阅关系不变。

概念上都属于「runtime」，但仓库里仍然分成两块，原因**不是**业务上非要拆开，而是 **Spring / Nacos 的接入方式不同**：

| 维度 | `dtp-config/`（YAML） | `sentinel-rules/`（JSON） |
|------|------------------------|---------------------------|
| **Nacos Group** | `DEFAULT_GROUP` | **`SENTINEL_GROUP`**（与 `application-*.yml` 里 `spring.cloud.sentinel.datasource.*.nacos.groupId` 一致） |
| **配置类型** | `type=yaml` | **`type=json`**（Sentinel 数据源要求 JSON） |
| **应用如何加载** | `spring.config.import` + `optional:nacos:...&refreshEnabled=true` | **Sentinel 扩展**：按 `dataId` + `groupId` 拉取，不走 Spring Boot 主配置树 |
| **文件形态** | `*-dtp-dev.yml`、`*-datasource-dev.yml`、`*-runtime-dev.yml` | `*-rules.json`（如 `group-buy-service-flow-rules.json`） |

因此**不能**把 Sentinel 规则塞进 `group-buy-service-runtime-dev.yml` 里当一段 YAML 用。除非改代码，让 Sentinel 从 YAML 解析或从 `DEFAULT_GROUP` 读，那会和官方 `sentinel-datasource-nacos` 的约定不一致，维护成本高。

若你希望「运维只跑一次脚本」，用仓库根下 **`init-nacos-runtime.sh`**：内部依次调用 `dtp-config/init-nacos-dtp.sh` 与 `sentinel-rules/init-nacos-rules.sh`。

## 目录导航

```text
dev-ops/nacos/
├── dtp-config/
│   ├── dynamic-tp/   # DynamicTp / Tomcat 线程池
│   ├── datasource/   # Hikari 动态覆盖
│   ├── runtime/      # 日志 / Agent / 自定义线程池等运行时配置
│   ├── shared/       # 跨服务共享配置（如 shared-mysql-tuning）
│   ├── init-nacos-dtp.sh
│   └── README.md
├── sentinel-rules/
│   ├── flow/         # FlowRule
│   ├── degrade/      # DegradeRule
│   ├── param-flow/   # ParamFlowRule
│   ├── system/       # SystemRule
│   ├── authority/    # AuthorityRule
│   ├── gateway/      # GatewayFlowRule / ApiDefinition
│   ├── init-nacos-rules.sh
│   └── README.md
├── init-nacos-runtime.sh
└── README.md
```

- **[dtp-config](dtp-config/README.md)**：按用途拆分 YAML，适合找线程池、数据源、运行时参数。
- **[sentinel-rules](sentinel-rules/README.md)**：按规则类型拆分 JSON，适合找限流、熔断、热点参数等规则。

## 一键发布（推荐）

在宿主机（或 CI）中：

```bash
cd dev-ops/nacos
chmod +x init-nacos-runtime.sh
NACOS_ADDR=100.86.250.112:8848 ./init-nacos-runtime.sh
# 若 Nacos 开启鉴权：
# NACOS_ADDR=100.86.250.112:8848 NACOS_USER=nacos NACOS_PASS=nacos ./init-nacos-runtime.sh
```

仍可按需单独执行 `dtp-config/init-nacos-dtp.sh` 或 `sentinel-rules/init-nacos-rules.sh`。

## 查找建议

- 想改线程池：先看 `dtp-config/dynamic-tp/`
- 想改 Hikari：先看 `dtp-config/datasource/`
- 想改拼团 Agent / Feign / 日志运行时项：看 `dtp-config/runtime/group-buy-service-runtime-dev.yml`
- 想改 pay 自定义线程池或日志：看 `dtp-config/runtime/pay-service-runtime-dev.yml`
- 想改 MySQL 共享调优档案：看 `dtp-config/shared/shared-mysql-tuning.yml`
- 想改 Sentinel 限流：看 `sentinel-rules/flow/`
- 想改熔断：看 `sentinel-rules/degrade/`
- 想改热点参数限流：看 `sentinel-rules/param-flow/`
- 想改系统保护阈值：看 `sentinel-rules/system/`
- 想改来源黑白名单：看 `sentinel-rules/authority/`
- 想改网关 Route / API 组限流：看 `sentinel-rules/gateway/`

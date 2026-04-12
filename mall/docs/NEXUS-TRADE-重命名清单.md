# 项目改名为 Nexus-Trade 需修改清单

目标：将项目整体名称从 `group-buy-market` / `group_buy_market` 改为 **Nexus-Trade**。

命名约定建议：
- **项目/应用/镜像**：`nexus-trade`（小写+连字符）
- **数据库名**：`nexus_trade`（小写+下划线）
- **MQ/Redis 等**：`nexus_trade_*` 或 `nexus-trade-*`（与现有风格一致）

---

## 一、Maven 项目（必须改）

| 文件 | 当前值 | 改为 |
|-----|--------|------|
| **根目录** `pom.xml` | `artifactId`: group-buy-market-yue | nexus-trade |
| | `<module>`: group-buy-market-api 等 6 个 | nexus-trade-api, nexus-trade-app, nexus-trade-domain, nexus-trade-trigger, nexus-trade-infrastructure, nexus-trade-types |
| | dependencyManagement 里各 artifactId | 同上 |
| | profile 里 HeapDumpPath / gc log 路径中的 group-buy-market-boot | nexus-trade-boot |
| `group-buy-market-api/pom.xml` | parent artifactId、本模块 artifactId、finalName | 对应改为 nexus-trade* |
| `group-buy-market-app/pom.xml` | 同上 + 依赖的 trigger、infrastructure | 同上 |
| `group-buy-market-domain/pom.xml` | 同上 | 同上 |
| `group-buy-market-trigger/pom.xml` | 同上 | 同上 |
| `group-buy-market-infrastructure/pom.xml` | 同上 | 同上 |
| `group-buy-market-types/pom.xml` | 同上 | 同上 |

**说明**：改完 pom 后，需要把**目录名**也重命名（如 `group-buy-market-app` → `nexus-trade-app`），否则 Maven 模块路径要对上。

---

## 二、数据库（必须改）

### 2.1 应用配置里的库名

| 文件 | 当前值 | 改为 |
|-----|--------|------|
| `group-buy-market-app/src/main/resources/application-dev.yml` | `jdbc:mysql://.../group_buy_market?` | `nexus_trade` |
| `group-buy-market-app/src/main/resources/application-prod.yml` | 同上 | `nexus_trade` |

### 2.2 所有建库 SQL

以下文件中：注释 `# 数据库: group_buy_market`、`CREATE database ... group_buy_market`、`use group_buy_market` 均改为 `nexus_trade`。

- `docs/dev-ops/mysql/sql/group_buy_market.sql`
- `docs/tag/v1.0/mysql/sql/2-26-group_buy_market.sql`
- `docs/tag/v1.0/mysql/sql/group_buy_market.sql`
- `docs/dev-ops/mysql/sql-bak/` 下所有 `*group_buy_market*.sql`（2-3~2-28, 3-3, group_buy_market.sql 等）

**SQL 文件名**可一并改为如：`2-29-nexus_trade.sql`，便于识别（非必须）。

---

## 三、应用名 / Spring / 中间件（必须改）

| 文件 | 当前值 | 改为 |
|-----|--------|------|
| `group-buy-market-app/.../application.yml` | `spring.application.name: group-buy-market-app` | nexus-trade-app |
| `group-buy-market-app/.../application-dev.yml` | RabbitMQ exchange: `group_buy_market_exchange` | nexus_trade_exchange |
| | queue: `group_buy_market_queue_2_topic_*` | nexus_trade_queue_2_topic_* |
| | tracing system: `group-buy-market` | nexus-trade |

---

## 四、Redis / 分布式锁 Key（必须改）

| 文件 | 当前值 | 改为 |
|-----|--------|------|
| `TradeLockRuleFilterFactory.java` | `group_buy_market_team_stock_key_` | nexus_trade_team_stock_key_ |
| `GroupBuyNotifyJob.java` | `group_buy_market_notify_job_exec` | nexus_trade_notify_job_exec |
| `ApiTest.java`（测试） | 同上 | 同上 |

---

## 五、缓存 Key 前缀（建议改）

| 文件 | 当前值 | 改为 |
|-----|--------|------|
| `GroupBuyDiscount.java` | `group_buy_market_cn.bugstack.infrastructure...` | 建议改为 `nexus_trade_...`（或你方命名空间） |
| `GroupBuyActivity.java` | 同上 | 同上 |

---

## 六、DevOps / 部署（必须改）

| 文件 | 当前值 | 改为 |
|-----|--------|------|
| `docs/dev-ops/prometheus/prometheus.yml` | job_name / app: `group-buy-market-app` | nexus-trade-app |
| `docs/dev-ops/logstash/logstash.conf` | index: `group-buy-market-log-%{+YYYY.MM.dd}` | nexus-trade-log-%{+YYYY.MM.dd} |
| `docs/dev-ops/docker-compose-app.yml` | 服务名、image、container_name: group-buy-market | nexus-trade |
| `docs/dev-ops/docker-compose-app-v1.0.yml`（或 tag 下） | 服务名、镜像、容器名、JDBC URL 中 group_buy_market | nexus_trade / nexus-trade |
| `docs/dev-ops/app/start.sh` | CONTAINER_NAME, IMAGE_NAME: group-buy-market | nexus-trade |
| `docs/dev-ops/app/stop.sh` | 容器名 group-buy-market | nexus-trade |
| `group-buy-market-app/build.sh` | 镜像名 `hongweiyue/group-buy-market-app:1.0` | 如 `hongweiyue/nexus-trade-app:1.0` |
| `.gitignore` | `/group-buy-market-app/data/` | `/nexus-trade-app/data/`（若目录改名） |

---

## 七、API 路径与前端（可选，影响接口）

| 文件 | 当前值 | 改为 |
|-----|--------|------|
| `MarketIndexController.java` | `query_group_buy_market_config` | 如 `query_nexus_trade_config` 或保留兼容 |
| `docs/tag/v1.0/nginx/html/index.html` | `/api/v1/gbm/index/query_group_buy_market_config` | 与后端一致 |
| `docs/dev-ops/nginx/html/index.html` | 同上 | 同上 |

说明：若对外已暴露接口，可保留旧路径并做兼容，或同时提供新路径。

---

## 八、Java 类名/接口名（可选，改动大）

以下为业务标识，仅当希望代码里也统一为 “Nexus-Trade” 时再改（会涉及重命名类、接口、变量及所有引用）：

- `IIndexGroupBuyMarketService` / `IndexGroupBuyMarketServiceImpl`
- `AbstractGroupBuyMarketSupport` 及其子类（EndNode, TagNode, SwitchRoot, RootNode, MarketNode, ErrorNode）
- 方法名：`queryGroupBuyMarketConfig`、`queryGroupBuyMarketConfigFallBack` 等
- 注释中的 “group-buy-market 拼团服务端” 等（如 `TradePort.java`）

---

## 九、文档与说明（建议改）

| 文件 | 说明 |
|-----|------|
| `docs/api/backend-config-api.md` | 文中 `2-26-group_buy_market.sql`、库名描述改为 nexus_trade |
| `docs/tag/v1.0/tag-v1.0.md` | 文档里的 group-buy-market 目录/仓库名说明可改为 nexus-trade |
| `README.md` | 若当前是通用脚手架说明，可增加一句“本项目现名 Nexus-Trade（原 group-buy-market）”或单独写 Nexus-Trade 说明 |

---

## 十、目录重命名（改完 pom 后做）

建议在改完所有 pom 的 module 与路径后，对目录重命名，保持与 Maven 一致：

- `group-buy-market-api` → `nexus-trade-api`
- `group-buy-market-app` → `nexus-trade-app`
- `group-buy-market-domain` → `nexus-trade-domain`
- `group-buy-market-trigger` → `nexus-trade-trigger`
- `group-buy-market-infrastructure` → `nexus-trade-infrastructure`
- `group-buy-market-types` → `nexus-trade-types`

根目录文件夹 `group-buy-market-yue` 可改为 `nexus-trade`（本地或 Git 仓库名，按你们约定）。

---

## 建议执行顺序

1. 数据库：先改 SQL 建库脚本，新建/迁移到 `nexus_trade` 库后再改应用配置。
2. 应用配置：改 `application*.yml`（库名、应用名、MQ、tracing）。
3. Maven：改根 pom 与各子模块 pom（artifactId、module、finalName、依赖、profile 路径）。
4. 目录：重命名各模块目录与根目录。
5. 代码：Redis/MQ key、缓存 key、注释。
6. DevOps：prometheus、logstash、docker-compose、start/stop、build.sh、.gitignore。
7. 文档与 API/前端路径：按需改。
8. （可选）Java 接口/类名与方法名统一为 Nexus-Trade。

如果你希望，我可以按上述顺序帮你逐类改具体文件内容（从配置和 pom 开始）。

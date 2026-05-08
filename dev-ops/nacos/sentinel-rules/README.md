# Sentinel × Nacos 动态限流接入说明

与 DynamicTp/YAML 分目录的原因（Group、格式、加载方式不同）及**一键发布 YAML + Sentinel**，见 [上级目录 README](../README.md)。

为了避免所有 JSON 平铺在同一级目录里，现在按**规则类型**拆分子目录；**文件名仍然是 Nacos DataId**，初始化脚本会递归扫描子目录并按文件名发布。

## 0. 目录结构

```text
sentinel-rules/
├── flow/         # 普通流控 FlowRule
├── degrade/      # 熔断 DegradeRule
├── param-flow/   # 热点参数 ParamFlowRule
├── system/       # 系统保护 SystemRule
├── authority/    # 来源黑白名单 AuthorityRule
├── gateway/      # GatewayFlowRule / ApiDefinition
├── init-nacos-rules.sh
└── README.md
```

查找建议：

- 想找某个服务的普通限流：去 `flow/`
- 想找慢调用 / 异常熔断：去 `degrade/`
- 想找按 SKU / 参数维度限流：去 `param-flow/`
- 想找 CPU / RT / 并发线程保护：去 `system/`
- 想找调用来源黑白名单：去 `authority/`
- 想找网关 route / API 组规则：去 `gateway/`

## 1. 方案架构

```text
┌──────────────────────┐       规则发布       ┌──────────────────────┐
│  Nacos 3.x           │ ◄─────────────────── │  运维 / init 脚本    │
│  (Config Center)     │                       └──────────────────────┘
│  Group=SENTINEL_GROUP│
│  DataId=xxx-rules.json
└──────────┬───────────┘
           │ listen + long-polling
           ▼
┌──────────────────────┐        心跳 + 指标     ┌──────────────────────┐
│ 业务服务 (Sentinel)   │ ───────────────────► │ Sentinel Dashboard   │
│ sentinel-datasource- │                       │ (:8858, 仅观测/调试) │
│ nacos                │                       └──────────────────────┘
└──────────────────────┘
```

**规则流向**：`规则 JSON -> Nacos -> 各服务 Sentinel 内存 -> 立即生效`。

> Dashboard 只做**实时观测**（QPS、RT、通过/拒绝/异常、簇点链路、机器列表）。
> 在 Dashboard 上临时改的规则不会回写 Nacos，重启会丢。**规则以 Nacos 中 JSON 为准**。

## 2. 覆盖的服务

| 服务 | application.name | Dashboard transport 端口 | 规则 DataId 前缀 |
|------|------------------|--------------------------|------------------|
| mall | `mall-service` | 8724 | `mall-service-*-rules.json` |
| order-service | `order-service` | 8720 | `order-service-*-rules.json` |
| group-buy-service | `group-buy-service` | 8721 | `group-buy-service-*-rules.json` |
| seckill-service | `seckill-service` | 8722 | `seckill-service-*-rules.json` |
| pay | `pay-service` | 8723 | `pay-service-*-rules.json` |
| springcloud-gateway | `springcloud-gateway` | 8718 | `springcloud-gateway-gw-*-rules.json` |

所有 DataId 统一放在 Nacos Group = `SENTINEL_GROUP`，namespace = `public`。

## 3. 规则类型（每个服务对应 5 个 JSON + 网关 2 个）

| 子目录 | 类型 | DataId 后缀 | 说明 |
|--------|------|-------------|------|
| `flow/` | 流控规则 FlowRule | `-flow-rules.json` | QPS / 并发线程数限流 |
| `degrade/` | 熔断规则 DegradeRule | `-degrade-rules.json` | 慢调用/异常比例/异常数熔断 |
| `param-flow/` | 热点参数 ParamFlowRule | `-param-flow-rules.json` | 按入参维度限流（如秒杀按 SKU） |
| `system/` | 系统规则 SystemRule | `-system-rules.json` | Load/CPU/RT/线程数全局保护 |
| `authority/` | 授权规则 AuthorityRule | `-authority-rules.json` | 基于 origin 的黑白名单 |
| `gateway/` | 网关流控 GatewayFlowRule | `-gw-flow-rules.json` | 针对 Gateway Route / API 组 |
| `gateway/` | 网关 API 分组 ApiDefinition | `-gw-api-rules.json` | 把多 URL 聚合成逻辑 API |

## 4. 启动步骤

### 4.1 启动基础设施（含 Sentinel Dashboard）

```bash
cd dev-ops
docker-compose -f docker-compose-environment.yml up -d nacos mysql sentinel-dashboard
```

Nacos 就绪后执行 **4.2** 发布规则（compose 不自动执行 `init-nacos-rules.sh`）。

访问 Sentinel Dashboard：<http://100.86.250.112:8858>，默认账号 `sentinel / sentinel`。

### 4.2 批量发布规则到 Nacos

```bash
cd dev-ops/nacos/sentinel-rules
# Nacos 未开启鉴权
NACOS_ADDR=100.86.250.112:8848 ./init-nacos-rules.sh

# Nacos 3.x 开启鉴权（NACOS_AUTH_ENABLE=true）
NACOS_ADDR=100.86.250.112:8848 NACOS_USER=nacos NACOS_PASS=nacos ./init-nacos-rules.sh
```

发布后在 Nacos 控制台 <http://100.86.250.112:8080>（3.x）搜索 Group `SENTINEL_GROUP` 可看到所有规则。

### 4.3 启动业务服务

所有服务启动后会：
1. 订阅 Nacos 对应 DataId，加载规则；
2. 向 Dashboard 注册并持续上报指标；
3. 任何 Nacos 规则变更 -> 秒级热更新。

### 4.4 验证动态生效

压测命令（以 mall 商品详情为例）：

```bash
hey -n 5000 -c 50 -m POST -H "Content-Type: application/json" \
  -d '{"goodsId":"P001"}' \
  http://100.86.250.112:8091/api/v1/mall/index/query_sku_detail
```

在 Nacos 中把 `mall-service-flow-rules.json` 里 `query_sku_detail` 的 `count` 从 500 改成 10，保存后：

- 观察 Sentinel Dashboard 中「实时监控」的 `Blocked QPS` 立刻上升
- `hey` 输出中状态码会出现 429（或返回 Sentinel 默认 `BlockException` JSON）

## 5. 规则 JSON 字段速查

### FlowRule（流控）

```json
{
  "resource": "/api/v1/xxx",
  "limitApp": "default",
  "grade": 1,
  "count": 100,
  "strategy": 0,
  "controlBehavior": 0,
  "maxQueueingTimeMs": 500
}
```

- `resource`：规则绑定的资源名。当前仓库约定优先写纯 URI。
- `grade`：`0=线程数`，`1=QPS`
- `count`：阈值本体
- `strategy`：`0=直接`，`1=关联`，`2=链路`
- `controlBehavior`：`0=快速失败`，`1=Warm Up`，`2=排队等待`，`3=Warm Up+排队`

### DegradeRule（熔断）

```json
{
  "resource": "/api/v1/xxx",
  "grade": 0,
  "count": 500,
  "timeWindow": 10,
  "minRequestAmount": 5,
  "statIntervalMs": 1000,
  "slowRatioThreshold": 0.5
}
```

- `grade`：`0=慢调用比例`，`1=异常比例`，`2=异常数`
- `count`：随 `grade` 改变含义。慢调用模式下是 RT 毫秒，异常比例模式下是 `0~1`
- `timeWindow`：熔断持续时间，单位秒
- `minRequestAmount`：统计窗口内最少请求数，低于它通常不会触发

### ParamFlowRule（热点参数）

```json
{
  "resource": "/api/v1/seckill/trade/create_pay_order",
  "grade": 1,
  "count": 50,
  "durationInSec": 1,
  "paramIdx": 0,
  "controlBehavior": 0
}
```

- `paramIdx`：取第几个参数做热点键。写错了规则基本等于没挂上。
- `count`：单个参数值允许的阈值，常用于按 `skuId`、`goodsId` 限流。

### GatewayFlowRule（网关）

```json
{
  "resource": "seckill-service",
  "resourceMode": 0,
  "grade": 1,
  "count": 5000,
  "intervalSec": 1,
  "controlBehavior": 0
}
```

- `resource`：Route ID 或 API 分组名
- `resourceMode`：`0=Route ID`，`1=API 分组`
- `intervalSec`：统计周期，单位秒

## 6. 常用字段怎么判断

- `resource`：规则挂在哪个资源上。普通服务通常写 URI；网关规则写 route id 或 apiName。
- `grade`：阈值口径。`flow` 里通常是线程数/QPS，`degrade` 里是慢调用/异常比例/异常数。
- `count`：阈值本体。它的含义跟规则类型和 `grade` 强相关，不能脱离上下文看。
- `controlBehavior`：超限后怎么处理。多数场景用 `0=快速失败`；只有明确需要削峰时才考虑排队。
- `limitApp`：对谁生效。`default` 表示所有来源；做来源隔离时再配合 `authority` 或自定义 origin 使用。
- `paramIdx`：热点参数规则里，取第几个参数做限流键。用错了会导致规则“看起来有配置，实际上打不到点”。

## 7. 常见问题

- **Dashboard 显示不了机器**：检查服务端 `spring.cloud.sentinel.transport.port` 是否被宿主机其他进程占用，同机多服务务必分开端口（本工程已 8718~8724 预留）。
- **规则改了不生效**：确认 `dataId` 与 `application-dev.yml` 一致、Group=`SENTINEL_GROUP`、`data-type: json`；Nacos 日志里应能看到 client long-polling。
- **想要修改规则自动回写 Nacos**：本工程未启用该模式（官方 Dashboard 只推送到客户端内存）。如需改 Dashboard，可以集成 [Sentinel AHAS](https://help.aliyun.com/zh/ahas/) 或自行 fork `sentinel-dashboard`。
- **Feign / RestTemplate 自动埋点**：`spring-cloud-starter-alibaba-sentinel` 已默认集成，资源名形如 `GET:http://order-service/api/v1/order/xxx`。如果你这里配的是纯 URI 规则，要先确认应用侧实际埋点命名是否一致。
- **网关规则**：`gw-flow` 和 `gw-api-group` 是 Sentinel Gateway 专属，普通 `flow` / `degrade` 等 rule-type 对网关不生效。

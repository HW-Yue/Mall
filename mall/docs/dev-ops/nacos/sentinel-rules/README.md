# Sentinel × Nacos 动态限流接入说明

与 DynamicTp/YAML 分目录的原因（Group、格式、加载方式不同）及**一键发布 YAML + Sentinel**，见 [上级目录 README](../README.md)。

## 1. 方案架构

```
┌──────────────────────┐       规则发布       ┌──────────────────────┐
│  Nacos 3.x           │ ◄─────────────────── │  运维 / init 脚本     │
│  (Config Center)     │                       └──────────────────────┘
│  Group=SENTINEL_GROUP│
│  DataId=xxx-rules.json
└──────────┬───────────┘
           │ listen + long-polling
           ▼
┌──────────────────────┐        心跳 + 指标     ┌──────────────────────┐
│ 业务服务 (Sentinel)   │ ───────────────────► │ Sentinel Dashboard   │
│ sentinel-datasource- │                       │ (:8858, 仅观测/调试)  │
│ nacos                │                       └──────────────────────┘
└──────────────────────┘
```

**规则流向**：`规则 JSON → Nacos → 各服务 Sentinel 内存 → 立即生效`。

> Dashboard 只做**实时观测**（QPS、RT、通过/拒绝/异常、簇点链路、机器列表）。
> 在 Dashboard 上临时改的规则不会回写 Nacos，重启会丢。**规则以 Nacos 中 JSON 为准**。

## 2. 覆盖的服务

| 服务 | application.name | Dashboard transport 端口 | 规则 DataId 前缀 |
|------|------------------|--------------------------|------------------|
| mall | `mall` | 8724 | `mall-*-rules.json` |
| order-service | `order-service` | 8720 | `order-service-*-rules.json` |
| group-buy-service | `group-buy-service` | 8721 | `group-buy-service-*-rules.json` |
| seckill-service | `seckill-service` | 8722 | `seckill-service-*-rules.json` |
| pay | `login-pay` | 8723 | `login-pay-*-rules.json` |
| springcloud-gateway | `xfg-dev-tech-springcloud-gateway` | 8718 | `xfg-dev-tech-springcloud-gateway-gw-*-rules.json` |

所有 DataId 统一放在 Nacos Group = `SENTINEL_GROUP`，namespace = `public`。

## 3. 规则类型（每个服务对应 5 个 JSON + 网关 2 个）

| 类型 | DataId 后缀 | 说明 |
|------|-------------|------|
| 流控规则 FlowRule | `-flow-rules.json` | QPS / 并发线程数限流 |
| 熔断规则 DegradeRule | `-degrade-rules.json` | 慢调用/异常比例/异常数熔断 |
| 热点参数 ParamFlowRule | `-param-flow-rules.json` | 按入参维度限流（如秒杀按 SKU） |
| 系统规则 SystemRule | `-system-rules.json` | Load/CPU/RT/线程数全局保护 |
| 授权规则 AuthorityRule | `-authority-rules.json` | 基于 origin 的黑白名单 |
| 网关流控 GatewayFlowRule | `-gw-flow-rules.json` | 针对 Gateway Route / API 组 |
| 网关 API 分组 ApiDefinition | `-gw-api-rules.json` | 把多 URL 聚合成逻辑 API |

## 4. 启动步骤

### 4.1 启动基础设施（含 Sentinel Dashboard）

```bash
cd mall/docs/dev-ops
docker-compose -f docker-compose-environment.yml up -d nacos mysql sentinel-dashboard
```

Nacos 就绪后执行 **4.2** 发布规则（compose 不自动执行 `init-nacos-rules.sh`）。

访问 Sentinel Dashboard：<http://100.86.250.112:8858>，默认账号 `sentinel / sentinel`。

### 4.2 批量发布规则到 Nacos

```bash
cd mall/docs/dev-ops/nacos/sentinel-rules
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
3. 任何 Nacos 规则变更 → 秒级热更新。

### 4.4 验证动态生效

压测命令（以 mall 商品详情为例）：

```bash
# 持续请求
hey -n 5000 -c 50 -m POST -H "Content-Type: application/json" \
  -d '{"goodsId":"P001"}' \
  http://100.86.250.112:8077/api/v1/mall/index/query_sku_detail
```

在 Nacos 中把 `mall-flow-rules.json` 里 `query_sku_detail` 的 `count` 从 500 改成 10，保存后：
- 观察 Sentinel Dashboard 中「实时监控」的 `Blocked QPS` 立刻上升
- hey 输出中 Status 429（或返回 Sentinel 默认 BlockException JSON）

## 5. 规则 JSON 字段速查

### FlowRule（流控）
```json
{
  "resource": "POST:/api/v1/xxx",
  "limitApp": "default",
  "grade": 1,              // 0=线程数 1=QPS
  "count": 100,            // 阈值
  "strategy": 0,           // 0=直接 1=关联 2=链路
  "controlBehavior": 0,    // 0=快速失败 1=Warm Up 2=排队等待 3=Warm Up+排队
  "maxQueueingTimeMs": 500 // controlBehavior=2 时排队超时
}
```

### DegradeRule（熔断）
```json
{
  "resource": "POST:/api/v1/xxx",
  "grade": 0,              // 0=慢调用比例 1=异常比例 2=异常数
  "count": 500,            // grade=0 时为慢调用 RT ms；grade=1 时为比例 0~1
  "timeWindow": 10,        // 熔断时长秒
  "minRequestAmount": 5,   // 最小请求数
  "statIntervalMs": 1000,
  "slowRatioThreshold": 0.5
}
```

### ParamFlowRule（热点参数，秒杀常用）
```json
{
  "resource": "POST:/api/v1/seckill/trade/create_pay_order",
  "grade": 1,
  "count": 50,             // 单参数 QPS
  "durationInSec": 1,
  "paramIdx": 0,           // 方法入参下标，或配合 @SentinelResource 注解指定
  "controlBehavior": 0
}
```

### GatewayFlowRule（网关）
```json
{
  "resource": "seckill-service",  // route id 或 apiName
  "resourceMode": 0,              // 0=Route ID, 1=API 分组
  "grade": 1,
  "count": 5000,
  "intervalSec": 1,
  "controlBehavior": 0
}
```

## 6. 常见问题

- **Dashboard 显示不了机器**：检查服务端 `spring.cloud.sentinel.transport.port` 是否被宿主机其他进程占用，同机多服务务必分开端口（本工程已 8718~8723 预留）。
- **规则改了不生效**：确认 `dataId` 与 application-dev.yml 一致、Group=`SENTINEL_GROUP`、`data-type: json`；Nacos 日志里应能看到 client long-polling。
- **想要修改规则自动回写 Nacos**：本工程未启用该模式（官方 Dashboard 只推送到客户端内存）。如需改 Dashboard，可以集成 [Sentinel AHAS](https://help.aliyun.com/zh/ahas/) 或自行 fork `sentinel-dashboard`。
- **Feign/RestTemplate 自动埋点**：`spring-cloud-starter-alibaba-sentinel` 已默认集成，资源名形如 `GET:http://order-service/api/v1/order/xxx`，直接在 Nacos 里配相应 FlowRule 即可。
- **网关规则**：`rule-type: gw-flow` 和 `gw-api-group` 是 Sentinel Gateway 专属，普通 flow/degrade 等 rule-type 对网关不生效。

# Mall Test Plan

## Scope

- `domain`
  - `SkuDetailService`
- `trigger/service`
  - `IndexAppServiceImpl`
  - `SkuStockAppService`
  - `NormalOrderAntiFraudService`
  - `BackendConfigAppServiceImpl`
  - `DCCAppServiceImpl`
- `trigger/http`
  - `IndexController`
  - `SkuController`
  - `OrderTradeController`
  - `BackendConfigController`
  - `DCCController`
- `infrastructure`
  - `SkuDetailRepository`
  - `ConfigRepository`
  - `IOrderServiceForMallFeign`

## Required Scenarios

- 商品详情查询与空结果处理
- 首页聚合数据装配
- 普通商品下单前反作弊与前置校验
- 锁库成功后调用 `order-service` 创建普通单
- `order-service` 返回失败、空响应、异常时的解锁补偿
- 后台配置增删改查
- DCC 配置读取与下发
- controller 参数校验、返回包装、异常路径

## Dependency Rules

- MySQL：真实测试库
- Redis：真实测试 Redis
- MQ：如果触达相关配置或 producer，统一 mock
- Feign：
  - `IOrderServiceForMallFeign` 必须 mock
  - `OrderTradeController` 重点验证下单请求 DTO、失败补偿、解锁逻辑

## Priority

- `P0`：`OrderTradeController`、`NormalOrderAntiFraudService`、`SkuDetailService`
- `P1`：后台配置、首页聚合、库存服务、Feign 下游异常路径
- `P2`：DCC 和管理类查询接口

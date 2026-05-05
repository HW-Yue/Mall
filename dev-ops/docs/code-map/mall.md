# mall 代码地图

## 启动与配置

- 启动模块：`mall/mall-app`
- 启动类：`com.yue.MallApplication`
- 配置文件：
  - `mall/mall-app/src/main/resources/application-dev.yml`
  - `mall/mall-app/src/main/resources/application-test.yml`
  - `mall/mall-app/src/main/resources/application-prod.yml`
  - `mall/mall-app/src/main/resources/application-test-mock.yml`

## HTTP 入口

- 首页与商品查询：
  - `mall/mall-trigger/src/main/java/com/yue/trigger/http/IndexController.java`
- 普通商品下单：
  - `mall/mall-trigger/src/main/java/com/yue/trigger/http/OrderTradeController.java`
- 后台配置：
  - `mall/mall-trigger/src/main/java/com/yue/trigger/http/admin/BackendConfigController.java`

## 核心业务

- 领域服务：
  - `mall/mall-domain/src/main/java/com/yue/domain/`
- 商品查询、库存锁定、活动配置等核心逻辑从这里进入

## 下游调用与适配

- 调 `order-service` 的 Feign：
  - `mall/mall-infrastructure/src/main/java/com/yue/infrastructure/feign/IOrderServiceForMallFeign.java`
- 库存 / repository / mapper：
  - `mall/mall-infrastructure/src/main/java/com/yue/infrastructure/`

## MQ

- `mall` 当前不承担主交易 MQ 消费角色
- 普通单异步落单是 `mall` 调 `order-service`，再由 `order-service` 投递 `normal-order-create`

## 前端联动

- 商城前端主脚本：
  - `dev-ops/nginx/html/js/mall.js`
- API 路径配置：
  - `dev-ops/nginx/html/js/api-config.js`

## 什么时候先看这里

- 商城首页商品查询
- 普通商品下单
- mall 到 order-service 的 Feign 调用
- 后台商品 / 活动配置

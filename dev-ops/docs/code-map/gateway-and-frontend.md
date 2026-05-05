# 网关与前端

## 网关路由

- 路由配置：
  - `springcloud-gateway/app/src/main/resources/application-dev.yml`
  - `springcloud-gateway/app/src/main/resources/application-test.yml`
  - `springcloud-gateway/app/src/main/resources/application-prod.yml`
- 启动模块：`springcloud-gateway/app`
- 启动类：`cn.bugstack.gateway.SpringcloudGatewayApplication`

## 网关 Java 入口

- Sentinel gateway 配置：
  - `springcloud-gateway/app/src/main/java/cn/bugstack/gateway/config/SentinelGatewayDataSourceConfig.java`
  - `springcloud-gateway/app/src/main/java/cn/bugstack/gateway/config/GatewayConfiguration.java`
- fallback 入口：
  - `springcloud-gateway/app/src/main/java/cn/bugstack/gateway/controller/GatewayController.java`

## 前端接口配置

- 前端统一接口路径：
  - `dev-ops/nginx/html/js/api-config.js`
- 关键对象：
  - `AppApiPaths`

## 前端主要业务脚本

- 商城与下单：
  - `dev-ops/nginx/html/js/mall.js`
- 支付页：
  - `dev-ops/nginx/html/js/payment.js`
- 订单列表：
  - `dev-ops/nginx/html/js/order-list.js`

## 什么时候先看这里

- 接口路径变更
- 网关转发异常
- 前端调用错服务或错路径
- Sentinel gateway fallback

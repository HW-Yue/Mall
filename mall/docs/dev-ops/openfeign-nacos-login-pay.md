# OpenFeign + Nacos 对接 login-pay 配置说明

本文档说明如何把支付下单请求从固定 URL 改为 Nacos 服务发现调用，目标服务为 `login-pay`。

## 1. 改造目标

- `group-buy-market` 不再依赖 `app.config.ali-pay.api-url` 固定地址。
- 通过 OpenFeign 调用 `login-pay`，并由 Spring Cloud LoadBalancer 做客户端负载均衡。
- 远程接口保持不变：`POST /api/v1/alipay/create_pay_order`。

## 2. 已完成代码改造点

- 增加依赖：
  - `spring-cloud-starter-openfeign`
  - `spring-cloud-starter-loadbalancer`
- 启动类开启 Feign 扫描：
  - `@EnableFeignClients(basePackages = {"com.yue.infrastructure.gateway"})`
- `IAliPayService` 从 Retrofit 接口改为 Feign 接口：
  - `@FeignClient(name = "login-pay")`
- `PayPort` 从 `Call.execute()` 改为 Feign 直接同步调用。
- 删除 Retrofit 配置类：
  - `group-buy-market-app/src/main/java/com/yue/config/Retrofit2Config.java`

## 3. 本服务（group-buy-market）配置

### 3.1 Nacos 注册发现

`group-buy-market-app/src/main/resources/application-dev.yml` 至少要包含：

```yaml
spring:
  application:
    name: group-buy-market
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        username: nacos
        password: nacos
```

### 3.2 Feign 建议配置（可选但推荐）

可增加超时与日志配置，避免默认参数在联调时排障困难：

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 3000
        readTimeout: 5000
        loggerLevel: basic
```

如果你希望按客户端单独配置，也可以把 `default` 改成 `login-pay`。

## 4. 对端服务（login-pay）配置

### 4.1 服务名必须一致

`login-pay` 服务需注册到同一个 Nacos，名称必须是 `login-pay`：

```yaml
spring:
  application:
    name: login-pay
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        username: nacos
        password: nacos
```

### 4.2 接口契约需一致

确保 `login-pay` 暴露以下接口并与请求/响应结构兼容：

- 方法：`POST`
- 路径：`/api/v1/alipay/create_pay_order`
- 请求体：`CreatePayRequestDTO` 对应字段
- 响应体：`Response<String>` 格式（`code/info/data`）

## 5. 启动与联调检查

1. 启动 Nacos。
2. 启动 `login-pay`，确认已注册到 Nacos。
3. 启动 `group-buy-market`。
4. 在 Nacos 控制台确认服务列表中存在：
   - `login-pay`
   - `group-buy-market`
5. 调用 `group-buy-market` 的支付相关入口，验证是否成功返回支付链接。

## 6. 常见问题排查

### 6.1 `No instances available for login-pay`

- `login-pay` 未注册或服务名不一致（最常见）。
- 两端 Nacos 地址、命名空间、分组不一致。

### 6.2 404 或方法不匹配

- `@FeignClient` 路径与对端 Controller 实际路径不一致。
- 对端 context-path 未考虑（例如服务配置了 `server.servlet.context-path`）。

### 6.3 反序列化失败

- 两边 `Response` 结构不一致（字段名、泛型 data 类型、日期格式）。

## 7. 关于 `lb://login-pay`

- `lb://` 主要用于 Spring Cloud Gateway 路由配置。
- 在 Feign 场景中，直接使用 `@FeignClient(name = "login-pay")` 即可走 Nacos + LoadBalancer，不需要写 `lb://`。

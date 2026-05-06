# DynamicTp / Tomcat 动态更新

本文说明线程池相关配置更新的实现口径。

## 支持范围

当前已拆出 `*-dtp-dev.yml` 的服务：

- `mall`
- `order-service`
- `group-buy-service`
- `seckill-service`

这些服务都通过：

- `classpath:nacos/*-dtp-dev.yml`
- `optional:nacos:* -dtp-dev.yml?group=DEFAULT_GROUP&refreshEnabled=true`

的方式接入运行时线程池配置。

## 实现原理

DynamicTp 本身负责接管线程池配置与运行时刷新：

- Spring 启动时绑定 `spring.dynamic.tp.*`
- Nacos 配置刷新后，DynamicTp 重新装配线程池参数
- `tomcat-tp` 也纳入同一配置树，用于调整 Web 容器工作线程

和 Hikari 不同，这一类能力主要由 DynamicTp 框架本身完成，项目代码不需要自己写 `ApplicationListener<EnvironmentChangeEvent>` 去逐项调用 setter。

## DataId 拆分目的

将线程池单独拆到 `*-dtp-dev.yml`，是为了：

- 不和数据源参数混在一起
- 不和业务开关、日志级别混在一起
- 压测、突发流量、限流回调排障时可以单独改这一份配置

## 生效边界

- 只对已接入 DynamicTp 管理的线程池和 `tomcat-tp` 生效。
- 没有挂到 DynamicTp 管理的自定义线程池，不会因为改 `*-dtp-dev.yml` 自动刷新。
- `pay` 当前没有同类 `*-dtp-dev.yml` 拆分入口，至少从当前仓库事实看不是这一套实现。

## 事实来源

- `mall/mall-app/src/main/resources/application-dev.yml`
- `order-service/order-service-app/src/main/resources/application-dev.yml`
- `group-buy-service/group-buy-service-app/src/main/resources/application-dev.yml`
- `seckill-service/seckill-service-app/src/main/resources/application-dev.yml`
- 各服务 `src/main/resources/nacos/*-dtp-dev.yml`
- `dev-ops/docs/monitoring/dynamic-tp.md`

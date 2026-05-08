# Docker Apps

这一目录统一管理 Nexus 应用相关的 Docker 构建与启动入口。

## 默认入口

当前镜像构建统一走一个入口脚本，运行时再由 compose 决定 `dev` / `test` / `test-mock`。

- `build-app-images.sh`
- `up-apps-test.sh`
- `down-apps-test.sh`
- `logs-apps-test.sh`
- `dev-ops/docker-compose-apps-test.yml`

## 文件说明

- `build-app-images.sh`
  - 统一构建入口
  - 先执行 Maven 打包，再调用 `dev-ops/docker-compose-apps-test-mock.yml` 执行 `docker compose build`
- `up-apps-test.sh`
  - 默认启动入口
  - 启动 `dev-ops/docker-compose-apps-test.yml` 里的所有应用容器
- `down-apps-test.sh`
  - 默认停止入口
  - 停止并移除 `dev-ops/docker-compose-apps-test.yml` 里的应用容器
- `logs-apps-test.sh`
  - 默认日志入口
  - 实时查看 `dev-ops/docker-compose-apps-test.yml` 的应用日志
- `dev-ops/docker-compose-apps-dev.yml`
  - 本地开发应用栈
  - 用于显式需要 `dev` profile 的场景
- `dev-ops/docker-compose-apps-test-mock.yml`
  - Docker mock 联调应用栈
  - 额外包含 `mock-alipay`

## 默认使用方式

在仓库根目录执行：

```bash
./docker-apps/build-app-images.sh
./docker-apps/up-apps-test.sh
./docker-apps/down-apps-test.sh
./docker-apps/logs-apps-test.sh
```

## 构建流程

`build-app-images.sh` 默认会做两步：

1. 打包以下模块的 Spring Boot JAR：
   - `mall/mall-app`
   - `order-service/order-service-app`
   - `group-buy-service/group-buy-service-app`
   - `seckill-service/seckill-service-app`
   - `pay/pay-app`
   - `springcloud-gateway/app`
   - `ops-agent-spring-ai`
2. 使用 `dev-ops/docker-compose-apps-test-mock.yml` 构建镜像

如果你已经手动打过包，可以跳过 Maven：

```bash
SKIP_MAVEN=true ./docker-apps/build-app-images.sh
```

## 镜像与容器

默认 test compose 里定义了这些服务：

- `mock-alipay`
- `mall-frontend`
- `ops-agent-frontend`
- `mall`
- `order-service`
- `group-buy-service`
- `seckill-service`
- `pay`
- `gateway`
- `ops-agent-spring-ai`

默认端口：

- `mock-alipay`: `7000`
- `mall-frontend`: `8088`
- `ops-agent-frontend`: `8089`
- `gateway`: `8090`
- `mall`: `8091`
- `order-service`: `8092`
- `group-buy-service`: `8093`
- `seckill-service`: `8094`
- `pay`: `8095`
- `ops-agent-spring-ai`: `8096`

默认镜像 tag 为 `local`，也可以通过环境变量覆盖：

```bash
IMAGE_TAG=dev ./docker-apps/build-app-images.sh
IMAGE_TAG=dev ./docker-apps/up-apps-test.sh
```

## 常用环境变量

- `IMAGE_TAG`
  - 镜像标签，默认 `local`
- `SKIP_MAVEN`
  - 设为 `true` 时跳过 Maven 打包
- `TAIL`
  - `logs-apps-test.sh` 显示的日志行数，默认 `200`
- `MALL_SPRING_PROFILES_ACTIVE` / `ORDER_SERVICE_SPRING_PROFILES_ACTIVE` / `GROUP_BUY_SERVICE_SPRING_PROFILES_ACTIVE`
  - 如需临时覆盖 profile，可单独设置；默认联调口径是 `test`

## 备注

- 默认 compose 文件是 `dev-ops/docker-compose-apps-test.yml`。
- `ops-agent-spring-ai` 依赖本机的 SkyWalking agent 目录，默认路径见 compose 文件中的 `SKYWALKING_AGENT_CONTEXT`。
- `ops-agent-frontend` 从 `ops-agent-spring-ai/dev-ops/frontend/` 构建，默认端口 `8089`。
- `build-app-images.sh` 会把 Docker/Buildx 配置写到 `docker-apps/.docker/`，避免构建时写入用户 `~/.docker`。

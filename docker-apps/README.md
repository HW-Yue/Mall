# Docker Apps

这一目录统一管理 Nexus 应用相关的 Docker 构建与启动入口。

## 文件说明

- `build-app-images.sh`
  - 先执行 Maven 打包，再调用 `docker compose build`
  - 用于构建本仓库各个应用的 Docker 镜像
- `docker-compose-apps.yml`
  - 统一定义所有应用服务的镜像、构建上下文、端口和运行参数
- `up-apps.sh`
  - 启动 `docker-compose-apps.yml` 里的所有应用容器
- `down-apps.sh`
  - 停止并移除这些应用容器
- `logs-apps.sh`
  - 实时查看这些应用容器的日志

## 使用方式

在仓库根目录执行：

```bash
./docker-apps/build-app-images.sh
./docker-apps/up-apps.sh
./docker-apps/down-apps.sh
./docker-apps/logs-apps.sh
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
2. 使用 `docker-apps/docker-compose-apps.yml` 构建镜像

如果你已经手动打过包，可以跳过 Maven：

```bash
SKIP_MAVEN=true ./docker-apps/build-app-images.sh
```

## 镜像与容器

compose 文件里定义了这些服务：

- `mall`
- `order-service`
- `group-buy-service`
- `seckill-service`
- `pay`
- `gateway`
- `ops-agent-spring-ai`

默认镜像 tag 为 `local`，也可以通过环境变量覆盖：

```bash
IMAGE_TAG=dev ./docker-apps/build-app-images.sh
IMAGE_TAG=dev ./docker-apps/up-apps.sh
```

## 常用环境变量

- `IMAGE_TAG`
  - 镜像标签，默认 `local`
- `SKIP_MAVEN`
  - 设为 `true` 时跳过 Maven 打包
- `JAVA_OPTS`
  - 覆盖普通服务的 JVM 参数
- `SPRING_PROFILES_ACTIVE`
  - Spring profile，默认 `dev`
- `ROCKETMQ_NAME_SERVER`
  - RocketMQ 地址，默认 `100.86.250.112:9876`
- `TAIL`
  - `logs-apps.sh` 显示的日志行数，默认 `200`

## 备注

- `docker-compose-apps.yml` 中各服务的 `build context` 都是相对这个目录写的，因此这些脚本建议在仓库根目录下执行。
- `ops-agent-spring-ai` 依赖本机的 SkyWalking agent 目录，默认路径见 compose 文件中的 `SKYWALKING_AGENT_CONTEXT`。
- `build-app-images.sh` 会把 Docker/Buildx 配置写到 `docker-apps/.docker/`，避免构建时写入用户 `~/.docker`。

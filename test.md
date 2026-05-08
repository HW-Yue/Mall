# Test Flow

## 目标

统一使用 Docker 环境跑拼团、订单、支付测试链路。`order-service`、`group-buy-service`、`pay` 默认使用 `test` profile，测试库、Redis、Nacos、RocketMQ、Sentinel、Logstash 都通过 `nexus-devops` Docker 网络内的服务名访问。

从构建镜像、启动 `docker-compose-apps.yml` 到自动化测试的完整流程，见 `dev-ops/full-flow-test/README.md`。

## 环境依赖

先启动基础环境，确保创建了外部网络 `nexus-devops`：

```bash
docker compose -f dev-ops/docker-compose-environment.yml up -d
docker compose -f dev-ops/docker-compose-rocketmq.yml up -d
docker compose -f dev-ops/docker-compose-elk.yml up -d
```

关键服务名：

- MySQL: `mysql:3306`
- Redis: `redis:6379`
- Nacos: `nacos:8848`
- RocketMQ NameServer: `rmq-namesrv:9876`
- Sentinel Dashboard: `sentinel-dashboard:8858`
- Logstash: `logstash:4560`

## MySQL 初始化

MySQL compose 已挂载 `dev-ops/mysql/sql` 到容器 `/docker-entrypoint-initdb.d`。MySQL 官方镜像只会在数据目录为空时执行初始化文件；顶层 `.sql` 会自动执行，`dev-ops/mysql/sql/zz-init-test-sql.sh` 负责继续执行 `dev-ops/mysql/sql/test/*.sql`。

如果改了 SQL 并希望重新初始化，需要先停止依赖 MySQL 的容器，再清空 `dev-ops/mysql/data`，然后重新启动基础环境。只执行 `docker compose down` 不会触发重新导入，因为 `dev-ops/mysql/data` 是宿主机绑定目录。

## 构建与启动

重新打包并构建应用镜像，`pay/pay-app/src/main/resources/application-test.yml` 会随 JAR 一起进入 `pay` 镜像：

```bash
./docker-apps/build-app-images.sh
./docker-apps/up-apps.sh
```

当前 `dev-ops/stacks/apps/test.compose.yml` 默认：

- `order-service`: `SPRING_PROFILES_ACTIVE=test`
- `group-buy-service`: `SPRING_PROFILES_ACTIVE=test`
- `pay`: `SPRING_PROFILES_ACTIVE=test`
- `pay` 宿主机端口: `8095`
- 所有应用容器加入外部网络 `nexus-devops`

如需临时覆盖：

```bash
ORDER_SERVICE_SPRING_PROFILES_ACTIVE=test \
GROUP_BUY_SERVICE_SPRING_PROFILES_ACTIVE=test \
PAY_SPRING_PROFILES_ACTIVE=test \
./docker-apps/up-apps.sh
```

## Mock 支付宝

mock 支付宝运行在宿主机，pay 容器通过 `host.docker.internal:7000` 访问它：

```bash
cd loadtest
make init
make seckill-mock
```

mock 支付宝支付成功后回调 pay 的宿主机映射端口：

```text
http://127.0.0.1:8095/api/v1/alipay/alipay_notify_url
```

## 验证入口

查看应用状态：

```bash
./docker-apps/up-apps.sh
./docker-apps/logs-apps.sh order-service group-buy-service pay
```

检查关键端点：

```bash
curl http://127.0.0.1:8092/actuator/health
curl http://127.0.0.1:8093/actuator/health
curl http://127.0.0.1:8095/actuator/health
```

跑全链路前确认测试库已有拼团、订单、支付所需初始化数据，并确认 RocketMQ topic 可自动创建或已创建。

拼团下单全链路脚本：

```bash
bash dev-ops/app/group-buy-full-flow-test.sh
```

脚本默认通过 gateway 访问 `http://127.0.0.1:8090/gw/api/v1/group-buy`，流程是先开团下单，读取接口返回的 `teamId`，再带同一个 `teamId` 发起第二笔参团下单。脚本只校验接口返回结果，不直接查库；测试库是否命中由各服务的 `application-test.yml` 保证。

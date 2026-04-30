# Test Flow

## 目标

统一使用 Docker 环境跑拼团、订单、支付测试链路。`order-service`、`group-buy-service`、`pay` 默认使用 `test` profile，测试库、Redis、Nacos、RocketMQ、Sentinel、Logstash 都通过 `nexus-devops` Docker 网络内的服务名访问。

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

## 构建与启动

重新打包并构建应用镜像，`pay/pay-app/src/main/resources/application-test.yml` 会随 JAR 一起进入 `pay` 镜像：

```bash
./docker-apps/build-app-images.sh
./docker-apps/up-apps.sh
```

当前 `docker-apps/docker-compose-apps.yml` 默认：

- `order-service`: `SPRING_PROFILES_ACTIVE=test`
- `group-buy-service`: `SPRING_PROFILES_ACTIVE=test`
- `pay`: `SPRING_PROFILES_ACTIVE=test`
- `pay` 宿主机端口: `18080`
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
http://127.0.0.1:18080/api/v1/alipay/alipay_notify_url
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
curl http://127.0.0.1:18080/actuator/health
```

跑全链路前确认测试库已有拼团、订单、支付所需初始化数据，并确认 RocketMQ topic 可自动创建或已创建。

# Full Flow Test

本目录记录从构建应用镜像、启动 `docker-compose-apps.yml`，到执行拼团下单和支付 HTML 获取自动化测试的完整流程。

## 1. 前置环境

在仓库根目录启动基础环境：

```bash
docker compose -f dev-ops/docker-compose-environment.yml up -d
docker compose -f dev-ops/docker-compose-rocketmq.yml up -d
docker compose -f dev-ops/docker-compose-elk.yml up -d
```

关键约定：

- 基础环境统一来自根目录 `dev-ops/`。
- MySQL、Redis、Nacos、RocketMQ、Sentinel、Logstash 都通过 Docker 网络内服务名访问。
- MySQL 初始化 SQL 位于 `dev-ops/mysql/sql/`。
- 测试库 SQL 位于 `dev-ops/mysql/sql/test/`，由 `dev-ops/mysql/sql/zz-init-test-sql.sh` 在 MySQL 首次初始化时导入。
- 如果需要重新导入 SQL，必须先清空 `dev-ops/mysql/data/` 后再启动 MySQL；只执行 compose down 不会重新初始化。

## 2. 构建应用镜像

完整构建所有应用镜像：

```bash
./docker-apps/build-app-images.sh
```

如果 Maven 包已经手动构建过，只重建 Docker 镜像：

```bash
SKIP_MAVEN=true ./docker-apps/build-app-images.sh
```

只重建某个服务时，可以直接使用 compose：

```bash
mvn -pl group-buy-service/group-buy-service-app -am clean package -DskipTests
docker compose -f docker-apps/docker-compose-apps.yml build group-buy-service
```

## 3. 启动应用容器

启动所有业务应用：

```bash
./docker-apps/up-apps.sh
```

等价命令：

```bash
docker compose -f docker-apps/docker-compose-apps.yml up -d
```

当前全链路测试相关服务默认 profile：

- `gateway`: `test`
- `order-service`: `test`
- `group-buy-service`: `test`
- `pay`: `test`

重建某个服务后，强制重启对应容器：

```bash
docker compose -f docker-apps/docker-compose-apps.yml up -d --force-recreate group-buy-service
```

## 4. 健康检查

```bash
curl http://127.0.0.1:8090/actuator/health
curl http://127.0.0.1:8092/actuator/health
curl http://127.0.0.1:8093/actuator/health
curl http://127.0.0.1:18080/actuator/health
```

网关路由检查：

```bash
curl http://127.0.0.1:8090/gw/api/v1/group-buy/market/query_goods_list
```

## 5. 自动化测试

执行拼团全链路脚本：

```bash
bash dev-ops/app/group-buy-full-flow-test.sh
```

脚本默认流程：

1. 等待 gateway、group-buy-service、order-service、pay 健康。
2. 通过 gateway 调用拼团开团下单接口。
3. 从第一单响应读取 `teamId`。
4. 通过订单服务 `get_pay_url` 获取第一单支付 HTML。
5. 使用同一个 `teamId` 创建第二笔参团订单。
6. 通过订单服务 `get_pay_url` 获取第二单支付 HTML。

成功示例：

```text
[2/5] create first group-buy order...
first orderId=015816533131 teamId=42393472
[3/5] request first payment html from order-service...
first order pay html length=1105
[4/5] create second group-buy order with returned teamId...
second orderId=318593642152 teamId=42393472
[5/5] request second payment html from order-service...
second order pay html length=1097
[done] success
```

## 6. Mock 支付

支付 HTML 会提交到 mock 支付宝地址：

```text
http://host.docker.internal:7000/gateway.do
```

pay 服务的回调地址配置为 Docker 网络内地址：

```text
http://pay:18080/api/v1/alipay/alipay_notify_url
```

启动 mock：

```bash
cd loadtest
make init
make seckill-mock
```

脚本只负责拿到支付 HTML，不打开浏览器、不手动提交表单；后续由 mock 支付链路处理。

## 7. 日志排查

查看应用日志：

```bash
./docker-apps/logs-apps.sh gateway group-buy-service order-service pay
```

或按服务过滤：

```bash
docker compose -f docker-apps/docker-compose-apps.yml logs --tail=200 group-buy-service
docker compose -f docker-apps/docker-compose-apps.yml logs --tail=200 order-service
docker compose -f docker-apps/docker-compose-apps.yml logs --tail=200 pay
```

重点观察：

- Nacos 注册地址应是 Docker 网络内 IP，不应是 `localhost`。
- gateway 应能通过 `lb://group-buy-service` 和 `lb://order-service` 路由。
- 拼团超时消息应按 RocketMQ Timer Message 延迟消费，不能刚发出就立即消费。
- 订单服务 `get_pay_url` 返回 `code=0000` 且 `data` 为支付 HTML。

## 8. 常见问题

### SQL 改了但库没变

MySQL 官方镜像只在数据目录为空时执行 `/docker-entrypoint-initdb.d`。需要重新初始化时，先停止依赖 MySQL 的容器，再清空 `dev-ops/mysql/data/`，然后重新启动基础环境。

### get_pay_url 返回订单状态不可支付 CLOSE

通常是订单在请求支付前已经被关单。最近一次修复点是拼团超时退款消息发送方式：必须使用 `rocketMQTemplate.syncSendDeliverTimeMills(...)`，否则 Timer Message 可能被当成普通消息立即投递。

### gateway 健康失败或路由失败

确认 `docker-apps/docker-compose-apps.yml` 中 gateway 默认使用 `test` profile，并确认 `springcloud-gateway/app/src/main/resources/application-test.yml` 使用 Docker 服务名：

- `nacos:8848`
- `sentinel-dashboard:8858`
- `logstash:4560`

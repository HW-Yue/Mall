# Full Flow Test

本目录记录基于 `dev-ops/docker-compose-apps-test.yml` 的完整联调流程：准备基础环境、构建应用镜像、启动业务容器、验证 `ops-agent` 前后端与网关链路、执行拼团全链路脚本。

## 使用约定

- 当前联调统一以 `dev-ops/docker-compose-apps-test.yml` 为准。
- 业务服务在 Docker 中统一按 `test` profile 运行。
- `ops-agent-spring-ai` 的前端和后端都纳入同一套应用构建与启动流程：
  - 前端：`ops-agent-frontend`，访问端口 `8089`
  - 后端：`ops-agent-spring-ai`，访问端口 `8096`
  - 网关入口：`8090`
- IDEA 直接点击 Markdown 代码块 Run 时，工作目录是本文件所在目录 `dev-ops/full-flow-test/`，不是项目根目录。
- 因此本文每一步都给出两种命令：
  - `IDEA 一键执行版`：可以直接点 Run
  - `项目根目录命令行版`：在仓库根目录执行

## 1. 启动基础环境

启动 MySQL、Redis、Nacos、RocketMQ、ELK 等基础组件。

### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && docker compose -f dev-ops/docker-compose-environment.yml up -d && docker compose -f dev-ops/docker-compose-rocketmq.yml up -d && docker compose -f dev-ops/docker-compose-elk.yml up -d
```

### 项目根目录命令行版

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
- 如果修改业务库初始化 SQL，必须同步修改对应测试 SQL。
- 如果需要重新导入 SQL，必须先清空 `dev-ops/mysql/data/` 后再启动 MySQL；只执行 compose down 不会重新初始化。

## 2. 构建所有应用镜像

使用统一脚本构建后端镜像和前端镜像。该脚本已经包含：

- `mall-frontend`
- `ops-agent-frontend`
- `mall`
- `order-service`
- `group-buy-service`
- `seckill-service`
- `pay`
- `gateway`
- `ops-agent-spring-ai`

### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && ./docker-apps/build-app-images-test.sh
```

### 项目根目录命令行版

```bash
./docker-apps/build-app-images-test.sh
```

如果 Maven 包已经手动构建过，只重建 Docker 镜像：

### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && SKIP_MAVEN=true ./docker-apps/build-app-images-test.sh
```

### 项目根目录命令行版

```bash
SKIP_MAVEN=true ./docker-apps/build-app-images-test.sh
```

## 3. 只重建单个服务或单个前端

适合改完某个模块后局部验证。

### 3.1 只重建 `ops-agent-spring-ai` 后端

#### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && mvn -pl ops-agent-spring-ai -am clean package -DskipTests && docker compose -f dev-ops/docker-compose-apps-test.yml build ops-agent-spring-ai
```

#### 项目根目录命令行版

```bash
mvn -pl ops-agent-spring-ai -am clean package -DskipTests
docker compose -f dev-ops/docker-compose-apps-test.yml build ops-agent-spring-ai
```

### 3.2 只重建 `ops-agent-frontend` 前端

#### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && docker compose -f dev-ops/docker-compose-apps-test.yml build ops-agent-frontend
```

#### 项目根目录命令行版

```bash
docker compose -f dev-ops/docker-compose-apps-test.yml build ops-agent-frontend
```

### 3.3 只重建单个业务服务示例：`group-buy-service`

#### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && mvn -pl group-buy-service/group-buy-service-app -am clean package -DskipTests && docker compose -f dev-ops/docker-compose-apps-test.yml build group-buy-service
```

#### 项目根目录命令行版

```bash
mvn -pl group-buy-service/group-buy-service-app -am clean package -DskipTests
docker compose -f dev-ops/docker-compose-apps-test.yml build group-buy-service
```

## 4. 启动所有业务应用

统一使用 test compose 启动。

### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && docker compose -f dev-ops/docker-compose-apps-test.yml up -d
```

### 项目根目录命令行版

```bash
docker compose -f dev-ops/docker-compose-apps-test.yml up -d
```

当前关键服务与端口：

- `mall-frontend`: `8088`
- `ops-agent-frontend`: `8089`
- `gateway`: `8090`
- `mall`: `8091`
- `order-service`: `8092`
- `group-buy-service`: `8093`
- `seckill-service`: `8094`
- `pay`: `8095`
- `ops-agent-spring-ai`: `8096`

## 5. 强制重建并重启单个容器

改完代码后，如果要确保使用最新镜像，使用 `--build --force-recreate`。

### 5.1 重启 `ops-agent-spring-ai` 后端

#### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && docker compose -f dev-ops/docker-compose-apps-test.yml up -d --build --force-recreate ops-agent-spring-ai
```

#### 项目根目录命令行版

```bash
docker compose -f dev-ops/docker-compose-apps-test.yml up -d --build --force-recreate ops-agent-spring-ai
```

### 5.2 重启 `ops-agent-frontend` 前端

#### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && docker compose -f dev-ops/docker-compose-apps-test.yml up -d --build --force-recreate ops-agent-frontend
```

#### 项目根目录命令行版

```bash
docker compose -f dev-ops/docker-compose-apps-test.yml up -d --build --force-recreate ops-agent-frontend
```

### 5.3 重启 `gateway` 与 `ops-agent-spring-ai`

#### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && docker compose -f dev-ops/docker-compose-apps-test.yml up -d --build --force-recreate gateway ops-agent-spring-ai
```

#### 项目根目录命令行版

```bash
docker compose -f dev-ops/docker-compose-apps-test.yml up -d --build --force-recreate gateway ops-agent-spring-ai
```

## 6. 健康检查与入口验证

### 6.1 基础健康检查

#### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && curl http://127.0.0.1:8090/actuator/health && echo && curl http://127.0.0.1:8092/actuator/health && echo && curl http://127.0.0.1:8093/actuator/health && echo && curl http://127.0.0.1:8095/actuator/health && echo && curl http://127.0.0.1:8096/actuator/health
```

#### 项目根目录命令行版

```bash
curl http://127.0.0.1:8090/actuator/health
curl http://127.0.0.1:8092/actuator/health
curl http://127.0.0.1:8093/actuator/health
curl http://127.0.0.1:8095/actuator/health
curl http://127.0.0.1:8096/actuator/health
```

### 6.2 前端入口检查

- 商城前端：`http://127.0.0.1:8088/`
- Ops Agent 前端：`http://127.0.0.1:8089/`

### 6.3 网关路由检查

#### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && curl http://127.0.0.1:8090/gw/api/v1/group-buy/market/query_goods_list && echo && curl -H 'Content-Type: application/json' -d '{"alerts":[]}' http://127.0.0.1:8090/gw/api/v1/ops-ai/alert/receive
```

#### 项目根目录命令行版

```bash
curl http://127.0.0.1:8090/gw/api/v1/group-buy/market/query_goods_list
curl -H 'Content-Type: application/json' -d '{"alerts":[]}' http://127.0.0.1:8090/gw/api/v1/ops-ai/alert/receive
```

预期：

- `group-buy` 路由能返回业务响应
- `ops-agent` 路由能返回 `{"status":"ok",...}` 一类 JSON，而不是 `503`

## 7. 执行拼团全链路测试

执行自动化脚本：

### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && bash dev-ops/app/group-buy-full-flow-test.sh
```

### 项目根目录命令行版

```bash
bash dev-ops/app/group-buy-full-flow-test.sh
```

脚本默认流程：

1. 等待 `gateway`、`group-buy-service`、`order-service`、`pay` 健康。
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

## 8. Mock 支付

支付 HTML 会提交到 mock 支付宝地址：

```text
http://host.docker.internal:7000/gateway.do
```

pay 服务的回调地址配置为 Docker 网络内地址：

```text
http://pay:8095/api/v1/alipay/alipay_notify_url
```

启动 mock：

### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)"/loadtest && make init && make seckill-mock
```

### 项目根目录命令行版

```bash
cd loadtest
make init
make seckill-mock
```

脚本只负责拿到支付 HTML，不打开浏览器、不手动提交表单；后续由 mock 支付链路处理。

## 9. 查看日志

### 9.1 查看关键业务日志

#### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && docker compose -f dev-ops/docker-compose-apps-test.yml logs --tail=200 gateway group-buy-service order-service pay
```

#### 项目根目录命令行版

```bash
docker compose -f dev-ops/docker-compose-apps-test.yml logs --tail=200 gateway group-buy-service order-service pay
```

### 9.2 查看 `ops-agent` 前后端相关日志

#### IDEA 一键执行版

```bash
cd "$(git rev-parse --show-toplevel)" && docker compose -f dev-ops/docker-compose-apps-test.yml logs --tail=200 ops-agent-spring-ai ops-agent-frontend gateway
```

#### 项目根目录命令行版

```bash
docker compose -f dev-ops/docker-compose-apps-test.yml logs --tail=200 ops-agent-spring-ai ops-agent-frontend gateway
```

重点观察：

- `ops-agent-spring-ai` 激活的 profile 应为 `test`
- Nacos 注册地址应是 Docker 网络内 IP，不应是 `127.0.0.1`
- gateway 应能订阅到 `ops-agent-spring-ai`
- `POST /gw/api/v1/ops-ai/alert/receive` 不应返回 `503`
- `group-buy-service` 和 `order-service` 路由应正常

## 10. 常见问题

### SQL 改了但库没变

MySQL 官方镜像只在数据目录为空时执行 `/docker-entrypoint-initdb.d`。需要重新初始化时，先停止依赖 MySQL 的容器，再清空 `dev-ops/mysql/data/`，然后重新启动基础环境。

### get_pay_url 返回订单状态不可支付 CLOSE

通常是订单在请求支付前已经被关单。最近一次修复点是拼团超时退款消息发送方式：必须使用 `rocketMQTemplate.syncSendDeliverTimeMills(...)`，否则 Timer Message 可能被当成普通消息立即投递。

### gateway 路由失败或 `ops-agent` 返回 503

优先检查：

- `dev-ops/docker-compose-apps-test.yml` 中 `ops-agent-spring-ai` 是否显式使用 `test`
- `ops-agent-spring-ai` 是否注册成服务名 `ops-agent-spring-ai`
- Nacos 中注册的实例 IP 是否为 Docker 网络地址，而不是 `127.0.0.1`
- `springcloud-gateway` 是否已经订阅到 `ops-agent-spring-ai`

### `ops-agent` 直连健康正常，但网关转发异常

先区分两类路径：

- 直连后端健康：`http://127.0.0.1:8096/actuator/health`
- 网关业务路径：`/gw/api/v1/ops-ai/**`

`/gw/api/v1/ops-ai/actuator/health` 不是标准业务路由验证路径，优先使用：

```bash
curl -H 'Content-Type: application/json' -d '{"alerts":[]}' http://127.0.0.1:8090/gw/api/v1/ops-ai/alert/receive
```

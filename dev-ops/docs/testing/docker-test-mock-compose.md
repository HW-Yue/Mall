# Docker Mock 联调

## 用途

这套链路用于在 Docker 中启动一组尽量脱离 Nacos/Sentinel 的商城 mock 环境：

- `mock-alipay`
- `mall`
- `order-service`
- `group-buy-service`
- `seckill-service`
- `pay`
- `gateway`
- `mall-frontend`
- `ops-agent-frontend`
- `ops-agent-spring-ai`

其中：

- `mock-alipay` 会随 compose 一起启动，接管支付表单提交并回调 `pay-service`
- 5 个业务服务统一走各自的 `application-test-mock.yml`
- `gateway` 走新增的 `application-test-mock.yml`，使用静态 HTTP 路由直连容器
- `ops-agent-spring-ai` 仍沿用现有 `test` profile，不纳入本次 mock 化范围

## 构建

项目根目录：

```bash
./docker-apps/build-app-images.sh
```

若 Jar 已构建完成：

```bash
SKIP_MAVEN=true ./docker-apps/build-app-images.sh
```

## 启动

项目根目录：

```bash
docker compose -f dev-ops/docker-compose-apps-test-mock.yml up -d
```

## 关键约定

- 前端仍通过 `gateway` 访问后端，不改 `dev-ops/nginx/html/js/api-config.js`
- `gateway` 不依赖 Nacos 服务发现，所有路由都直连容器名
- `pay-service` 的 mock 支付网关默认指向 `http://mock-alipay:7000/gateway.do`
- `ops-agent-spring-ai` 在这套 compose 中继续使用 `test` profile，便于保留现有 Nacos/OTLP 行为
- 端口与现有 `docker-compose-apps-test.yml` 保持一致：
  - mock 支付宝：`7000`
  - 商城前端：`8088`
  - Ops Agent 前端：`8089`
  - gateway：`8090`
  - mall：`8091`
  - order-service：`8092`
  - group-buy-service：`8093`
  - seckill-service：`8094`
  - pay：`8095`
  - ops-agent-spring-ai：`8096`

## 验证

建议至少验证以下入口：

```bash
curl http://127.0.0.1:8090/actuator/health
curl http://127.0.0.1:8091/actuator/health
curl http://127.0.0.1:8092/actuator/health
curl http://127.0.0.1:8093/actuator/health
curl http://127.0.0.1:8094/actuator/health
curl http://127.0.0.1:8095/actuator/health
curl http://127.0.0.1:8096/actuator/health
curl http://127.0.0.1:7000/mock/orders
```

再从浏览器验证：

- `http://127.0.0.1:8088/`
- `http://127.0.0.1:8089/`
- 下单后应跳转到 `mock-alipay` 收银台

## 边界

- 这套 compose 的目标是提供独立可跑的 mock 联调入口，不保证业务行为完全等同现有 `apps-test`
- 若后续要把 `ops-agent-spring-ai` 也彻底 mock 化，需要单独补 `application-test-mock.yml` 和对应外部依赖策略

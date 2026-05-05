# springcloud-gateway 接口文档

## 服务职责

`springcloud-gateway` 是所有前端网关入口的统一转发层，负责：

- `/gw` 前缀路由
- `StripPrefix=1`
- `ops-agent-spring-ai` 的 `RewritePath`
- 全局 CORS
- Sentinel gateway fallback

详细路由示例和路径重写说明见：[gateway 详细接口](../details/springcloud-gateway.md)

## 对外入口

| 前端路径前缀 | 下游服务 | 过滤说明 | 备注 |
|--------------|----------|----------|------|
| `/gw/api/v1/mall/**` | `mall-service` | `StripPrefix=1` | 商城与后台配置 |
| `/gw/api/v1/pay/**` | `pay-service` | `StripPrefix=1` | 登录相关 |
| `/gw/api/v1/alipay/**` | `pay-service` | `StripPrefix=1` | 支付相关 |
| `/gw/api/v1/order/**` | `order-service` | `StripPrefix=1` | 订单服务 |
| `/gw/api/v1/group-buy/**` | `group-buy-service` | `StripPrefix=1` | 拼团服务 |
| `/gw/api/v1/seckill/**` | `seckill-service` | `StripPrefix=1` | 秒杀服务 |
| `/gw/api/v1/ops-ai/**` | `ops-agent-spring-ai` | `StripPrefix=1` + `RewritePath` | SSE 长连接禁用 route 级 response-timeout |

## 管理 / fallback

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/fallback` | RequestMapping | Sentinel fallback | Gateway 内部 | 429 响应出口之一 |

## 关键同步文件

- 路由配置：`springcloud-gateway/app/src/main/resources/application-*.yml`
- 前端路径：`dev-ops/nginx/html/js/api-config.js`
- 相关测试：`springcloud-gateway/app/src/test/java/**`

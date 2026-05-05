# springcloud-gateway 详细接口文档

## 路由规则

### `/gw/api/v1/mall/**`

- 下游：`lb://mall-service`
- 过滤器：`StripPrefix=1`
- 示例：

```text
/gw/api/v1/mall/index/query_goods_page
-> /api/v1/mall/index/query_goods_page
```

### `/gw/api/v1/pay/**`

- 下游：`lb://pay-service`
- 过滤器：`StripPrefix=1`

### `/gw/api/v1/alipay/**`

- 下游：`lb://pay-service`
- 过滤器：`StripPrefix=1`

### `/gw/api/v1/order/**`

- 下游：`lb://order-service`
- 过滤器：`StripPrefix=1`

### `/gw/api/v1/group-buy/**`

- 下游：`lb://group-buy-service`
- 过滤器：`StripPrefix=1`

### `/gw/api/v1/seckill/**`

- 下游：`lb://seckill-service`
- 过滤器：`StripPrefix=1`

### `/gw/api/v1/ops-ai/**`

- 下游：`lb://ops-agent-spring-ai`
- 过滤器：
  - `StripPrefix=1`
  - `RewritePath /api/v1/ops-ai/(.*) -> /api/v1/$1`
  - `DedupeResponseHeader`

示例：

```text
/gw/api/v1/ops-ai/chat/stream
-> /api/v1/chat/stream
```

## Sentinel fallback

### `/fallback`

说明：网关 fallback 响应入口。

典型响应：

```json
{
  "code": "429",
  "info": "Blocked by Sentinel(gateway)"
}
```

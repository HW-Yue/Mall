# order-service 详细接口文档

## `POST /api/v1/order/create_order`

说明：通用订单创建接口，供拼团、秒杀等营销服务调用。

约束：

- `orderId` 与 `outTradeNo` 都由 `order-service` 服务端生成
- 外部调用方不允许传 `outTradeNo`
- 对外响应只承诺 `orderId`

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户 ID |
| `productId` | string | 是 | 商品 ID |
| `marketType` | string | 是 | `normal / group_buy / seckill` |
| `originalPrice` | number | 否 | 原始价格 |
| `deductionPrice` | number | 否 | 折扣金额 |
| `payPrice` | number | 是 | 支付金额 |
| `source` | string | 否 | 来源 |
| `channel` | string | 否 | 渠道 |
| `goodsName` | string | 否 | 商品名称 |
| `goodsImageUrl` | string | 否 | 商品图片 URL |

请求样例：

```json
{
  "userId": "u1",
  "productId": "g1",
  "marketType": "group_buy",
  "originalPrice": 199.00,
  "deductionPrice": 20.00,
  "payPrice": 179.00,
  "source": "s01",
  "channel": "c01",
  "goodsName": "iPhone 16 128G",
  "goodsImageUrl": "https://cdn.example.com/iphone.png"
}
```

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "orderId": "OD191234567890123456"
  }
}
```

## `POST /api/v1/order/create_order_normal_from_mall`

说明：普通商品锁库后由 `mall` 调用，同步入队，异步落单。

Header：

| 名称 | 必填 | 说明 |
|------|------|------|
| `X-Internal-Token` | 否 | 服务间内部校验 token |

请求体：与 `create_order` 基本一致，`marketType` 固定会被视为 `normal`

响应体：

- 只返回 `orderId`
- `outTradeNo` 不对 `mall` 暴露

## `POST /api/v1/order/get_pay_url`

说明：根据订单获取支付链接。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户 ID |
| `orderId` | string | 是 | 订单 ID |

请求样例：

```json
{
  "userId": "u1",
  "orderId": "OID-1"
}
```

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": "https://pay.example.com/qrcode/abc"
}
```

内部数据流：

- `order-service` 查询本地订单时使用 `orderId`
- 命中 Redis `order:exists:{userId}:{orderId}` 时，会做短暂 DB 重试兜底异步落单
- 真正调用 `pay-service` 时，使用的是订单服务内部生成并持久化的 `outTradeNo`

## `POST /api/v1/order/refund`

说明：普通订单退款。

## `POST /api/v1/order/refund_execute`

说明：营销服务内部调用的退款执行接口。

这两个接口的请求体字段一致：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户 ID |
| `orderId` | string | 是 | 订单 ID |

请求样例：

```json
{
  "userId": "u1",
  "orderId": "OID-1"
}
```

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": true
}
```

## `POST /api/v1/order/query_user_order_list`

说明：查询用户订单列表，游标分页。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户 ID |
| `lastId` | long | 否 | 下一页游标 |
| `pageSize` | integer | 否 | 页大小 |

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "orderList": [
      {
        "orderId": "OD191234567890123456",
        "productId": "g1",
        "productName": "goods",
        "payPrice": 99.00,
        "orderStatus": "PAY_WAIT"
      }
    ],
    "hasMore": true,
    "lastId": 123456789
  }
}
```

## `POST /api/v1/order/query_seckill_order`

说明：秒杀前端轮询建单结果。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `seckillToken` | string | 是 | 秒杀 token |

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "status": 1,
    "orderId": "OID-9"
  }
}
```

## 说明补充

- `outTradeNo` 只在 `order-service` 与 `pay-service` 之间流转
- 营销服务退款统一走 `refund_execute(userId, orderId)`
- 拼团/秒杀收到的支付成功、关单、退款事件都只包含 `orderId`

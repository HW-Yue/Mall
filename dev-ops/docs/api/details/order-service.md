# order-service 详细接口文档

## `POST /api/v1/order/create_order`

说明：通用订单创建接口，供拼团、秒杀等营销服务调用。

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
| `outTradeNo` | string | 否 | 外部单号，不传自动生成 |
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
    "orderId": "OID-1",
    "outTradeNo": "OUT-1"
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
        "orderId": "OID-1",
        "outTradeNo": "OUT-1",
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

## `POST /api/v1/order/query_order_by_out_trade_no`

说明：按外部交易单号查询订单，供服务间幂等确认和补偿使用。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户 ID |
| `outTradeNo` | string | 是 | 外部交易单号 |

请求样例：

```json
{
  "userId": "u1",
  "outTradeNo": "OUT-1"
}
```

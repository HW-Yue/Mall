# seckill-service 详细接口文档

## `GET /api/v1/seckill/market/query_goods_list`

说明：查询秒杀商品列表。

请求参数：无

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "seckillGoodsList": [
      {
        "goodsId": "g1",
        "goodsName": "phone",
        "goodsImageUrl": "https://cdn.example.com/g1.png",
        "originalPrice": 199.00,
        "payPrice": 99.00,
        "source": "s01",
        "channel": "c01",
        "activityId": 1001
      }
    ]
  }
}
```

## `POST /api/v1/seckill/trade/create_pay_order`

说明：秒杀下单。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户 ID |
| `productId` | string | 是 | 商品 ID |
| `activityId` | long | 是 | 活动 ID |
| `source` | string | 否 | 来源 |
| `channel` | string | 否 | 渠道 |
| `goodsName` | string | 否 | 商品名称，传给 `order-service` 冗余存储 |
| `goodsImageUrl` | string | 否 | 商品图片 |
| `isTest` | boolean | 否 | 压测模式，`true` 时走 mock token |

请求样例：

```json
{
  "userId": "u1",
  "productId": "g1",
  "activityId": 1001,
  "source": "s01",
  "channel": "c01",
  "goodsName": "秒杀手机",
  "goodsImageUrl": "https://cdn.example.com/g1.png",
  "isTest": false
}
```

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "seckillToken": "token-1",
    "orderId": "OID-1",
    "outTradeNo": "SK123456"
  }
}
```

## `POST /api/v1/seckill/trade/refund`

说明：秒杀退款。

请求体：当前 controller 直接接收 JSON 字符串，实际字段如下：

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

## `GET /api/v1/seckill/admin/query_activities`

说明：查询有效活动和商品列表，返回当前 Redis 预热状态。

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "activities": [
      {
        "activityId": 1001,
        "activityName": "618 秒杀",
        "seckillPrice": 99.00,
        "remainCount": 10,
        "goodsList": [
          {
            "goodsId": "g1",
            "goodsName": "phone",
            "currentStock": "9"
          }
        ]
      }
    ]
  }
}
```

## `POST /api/v1/seckill/admin/preheat`

说明：手动预热库存到 Redis。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `activityId` | long | 是 | 活动 ID |
| `goodsId` | string | 是 | 商品 ID，传 `"all"` 表示整活动 |
| `stock` | integer | 是 | 预热库存 |
| `expireSeconds` | long | 否 | 过期秒数，不传默认 7200 |

请求样例：

```json
{
  "activityId": 1001,
  "goodsId": "all",
  "stock": 50,
  "expireSeconds": 3600
}
```

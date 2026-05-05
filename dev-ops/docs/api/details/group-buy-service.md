# group-buy-service 详细接口文档

## `GET /api/v1/group-buy/market/query_goods_list`

说明：查询拼团商品列表。

请求参数：无

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "groupBuyGoodsList": [
      {
        "goodsId": "g1",
        "goodsName": "phone",
        "goodsImageUrl": "https://cdn.example.com/g1.png",
        "originalPrice": 199.00,
        "payPrice": 179.00,
        "source": "s01",
        "channel": "c01",
        "activityId": 1001
      }
    ]
  }
}
```

## `POST /api/v1/group-buy/market/query_group_buy_market_config`

说明：聚合接口，返回拼团试算结果、进行中的团和团队统计。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户 ID |
| `source` | string | 是 | 渠道来源 |
| `channel` | string | 是 | 渠道编码 |
| `goodsId` | string | 是 | 商品 ID |

请求样例：

```json
{
  "userId": "u1",
  "source": "s01",
  "channel": "c01",
  "goodsId": "g1"
}
```

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "activityId": 1001,
    "goods": {
      "goodsId": "g1",
      "goodsImageUrl": "https://cdn.example.com/g1.png",
      "originalPrice": 199.00,
      "deductionPrice": 20.00,
      "payPrice": 179.00
    },
    "teamList": [
      {
        "userId": "u2",
        "teamId": "T1",
        "activityId": 1001,
        "targetCount": 3,
        "completeCount": 1,
        "lockCount": 1,
        "validTimeCountdown": "00:29:10",
        "outTradeNo": "OUT-1"
      }
    ],
    "teamStatistic": {
      "allTeamCount": 10,
      "allTeamCompleteCount": 2,
      "allTeamUserCount": 23
    }
  }
}
```

## `POST /api/v1/group-buy/trade/create_pay_order`

说明：拼团下单，支持开团和参团。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户 ID |
| `productId` | string | 是 | 商品 ID |
| `teamId` | string | 否 | 已有队伍 ID，开团时为空 |
| `activityId` | long | 是 | 活动 ID |
| `source` | string | 否 | 来源 |
| `channel` | string | 否 | 渠道 |
| `outTradeNo` | string | 否 | 外部交易单号 |

请求样例：

```json
{
  "userId": "u1",
  "productId": "g1",
  "activityId": 1001,
  "source": "s01",
  "channel": "c01"
}
```

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "orderId": "OID-1",
    "teamId": "T1",
    "outTradeNo": "OUT-1",
    "originalPrice": 199.00,
    "deductionPrice": 20.00,
    "payPrice": 179.00
  }
}
```

## `POST /api/v1/group-buy/trade/refund`

说明：拼团退款。

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

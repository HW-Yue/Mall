# pay 详细接口文档

## `POST /api/v1/alipay/create_pay_order`

说明：创建支付订单。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户 ID |
| `outTradeNo` | string | 是 | 外部交易单号 |
| `productId` | string | 是 | 商品 ID |
| `productName` | string | 否 | 商品名称 |
| `teamId` | string | 否 | 拼团队伍 ID |
| `originalPrice` | number | 否 | 原始价格 |
| `deductionPrice` | number | 否 | 折扣金额 |
| `payPrice` | number | 是 | 支付金额 |
| `marketType` | string | 是 | `normal / group_buy / seckill` |

请求样例：

```json
{
  "userId": "u1",
  "outTradeNo": "OUT-1",
  "productId": "g1",
  "productName": "goods",
  "originalPrice": 199.00,
  "deductionPrice": 20.00,
  "payPrice": 179.00,
  "marketType": "normal"
}
```

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": "https://openapi.alipay.com/gateway.do?... "
}
```

## `POST /api/v1/alipay/alipay_notify_url`

说明：支付宝异步回调。

请求方式：`application/x-www-form-urlencoded`

关键回调参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `trade_status` | 是 | 交易状态，成功时为 `TRADE_SUCCESS` |
| `out_trade_no` | 是 | 商户订单号 |
| `sign` | 是 | 支付宝签名 |
| `gmt_payment` | 否 | 支付时间 |
| `buyer_id` | 否 | 支付宝买家 ID |
| `buyer_pay_amount` | 否 | 买家支付金额 |

回调成功返回：

```text
success
```

回调失败返回：

```text
false
```

## `POST /api/v1/alipay/active_pay_notify`

说明：主动查询支付宝交易状态，主要用于测试或补偿确认。

请求参数：

| 参数 | 位置 | 必填 | 说明 |
|------|------|------|------|
| `outTradeNo` | query/form | 是 | 外部交易单号 |

示例：

```text
/api/v1/alipay/active_pay_notify?outTradeNo=OUT-1
```

## 登录接口

### `GET /api/v1/pay/login/weixin_qrcode_ticket`

说明：生成默认场景扫码 ticket。

### `GET /api/v1/pay/login/weixin_qrcode_ticket_scene`

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `sceneStr` | 是 | 场景标识 |

### `GET /api/v1/pay/login/check_login`

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `ticket` | 是 | 登录 ticket |

### `GET /api/v1/pay/login/check_login_scene`

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `ticket` | 是 | 登录 ticket |
| `sceneStr` | 是 | 场景标识 |

### `POST /api/v1/pay/login/register`

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `ticket` | string | 是 | 登录 ticket |
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 密码 |

请求样例：

```json
{
  "ticket": "ticket-1",
  "username": "tom",
  "password": "123456"
}
```

## 微信门户

### `GET /api/v1/pay/weixin/portal/receive`

说明：微信公众号验签。

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `signature` | 是 | 微信签名 |
| `timestamp` | 是 | 时间戳 |
| `nonce` | 是 | 随机串 |
| `echostr` | 是 | 回显串 |

### `POST /api/v1/pay/weixin/portal/receive`

说明：接收微信扫码事件和消息体。

请求体样例：

```xml
<xml>
  <MsgType>event</MsgType>
  <Event>SCAN</Event>
  <Ticket>TICKET</Ticket>
</xml>
```

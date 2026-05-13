# mall 详细接口文档

## 商品首页

### `GET /api/v1/mall/index/query_category_type_list`

说明：查询商品分类列表。

请求参数：无

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "id": 1,
      "name": "数码",
      "iconUrl": "https://cdn.example.com/icon.png",
      "sortOrder": 1
    }
  ]
}
```

### `POST /api/v1/mall/index/query_goods_page`

说明：按分类分页查询商品。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `categoryId` | integer | 否 | 分类 ID，不传查全部 |
| `pageNum` | integer | 是 | 页码，从 1 开始 |
| `pageSize` | integer | 是 | 每页条数 |

请求样例：

```json
{
  "categoryId": 1,
  "pageNum": 1,
  "pageSize": 10
}
```

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "list": [
      {
        "goodsId": "g1",
        "goodsName": "phone",
        "goodsImageUrl": "https://cdn.example.com/g1.png",
        "originalPrice": 99.00
      }
    ]
  }
}
```

### `POST /api/v1/mall/index/query_sku_detail`

说明：查询商品详情。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `goodsId` | string | 是 | 商品 ID |

请求样例：

```json
{
  "goodsId": "g1"
}
```

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "goodsId": "g1",
    "goodsName": "phone",
    "goodsImageUrl": "https://cdn.example.com/g1.png",
    "originalPrice": 99.00,
    "goodsDetail": "128G 黑色"
  }
}
```

### `GET /api/v1/mall/index/query_activity_goods`

说明：查询活动商品入口数据。

请求参数：无

响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "groupBuyList": [],
    "seckillList": []
  }
}
```

## 普通下单

### `POST /api/v1/mall/trade/create_normal_order`

说明：普通商品下单入口，内部执行防刷、锁库、调用 `order-service` 异步落单。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户 ID |
| `productId` | string | 是 | 商品 ID |
| `payPrice` | number | 是 | 支付金额 |
| `originalPrice` | number | 否 | 原始价格 |
| `deductionPrice` | number | 否 | 折扣金额 |
| `source` | string | 否 | 来源 |
| `channel` | string | 否 | 渠道 |
| `goodsName` | string | 否 | 商品名称 |
| `goodsImageUrl` | string | 否 | 商品图片 |

请求样例：

```json
{
  "userId": "u1",
  "productId": "g1",
  "originalPrice": 199.00,
  "deductionPrice": 20.00,
  "payPrice": 179.00,
  "source": "s01",
  "channel": "c01",
  "goodsName": "iPhone 16 128G",
  "goodsImageUrl": "https://cdn.example.com/iphone.png"
}
```

成功响应样例：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {
    "orderId": "OD123456789012345678"
  }
}
```

说明：

- `mall` 不生成也不接收 `outTradeNo`
- `order-service` 在内部生成 `orderId/outTradeNo`
- `mall` 对前端只返回 `orderId`，后续支付页也只用 `orderId`

失败响应样例：

```json
{
  "code": "E0009",
  "info": "锁库失败",
  "data": null
}
```

## 库存接口

### `POST /api/v1/sku/lock_stock`
### `POST /api/v1/sku/unlock_stock`

说明：锁定或释放单个 SKU 库存，主要供服务间调用。

请求体参数：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `goodsId` | string | 是 | 商品 ID |
| `count` | integer | 是 | 数量，通常为 1 |

请求样例：

```json
{
  "goodsId": "g1",
  "count": 1
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

## 后台配置

### 活动类型

- `GET /api/v1/gbm/config/activity_types`
- `GET /api/v1/gbm/config/activity_type/{id}`
- `POST /api/v1/gbm/config/activity_type`
- `PUT /api/v1/gbm/config/activity_type`
- `DELETE /api/v1/gbm/config/activity_type/{id}`

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | integer | 更新时是 | 主键 |
| `typeName` | string | 是 | 类型名称 |
| `typeCode` | string | 是 | 类型编码 |
| `status` | integer | 否 | 状态 |

请求样例：

```json
{
  "typeName": "拼团",
  "typeCode": "GROUP_BUY",
  "status": 1
}
```

### 分类

- `GET /api/v1/gbm/config/categories`
- `GET /api/v1/gbm/config/category/{id}`
- `POST /api/v1/gbm/config/category`
- `PUT /api/v1/gbm/config/category`
- `DELETE /api/v1/gbm/config/category/{id}`

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | integer | 更新时是 | 主键 |
| `name` | string | 是 | 分类名 |
| `code` | string | 是 | 分类编码 |
| `iconUrl` | string | 否 | 图标 |
| `sortOrder` | integer | 否 | 排序 |
| `status` | integer | 否 | 状态 |

### SKU

- `GET /api/v1/gbm/config/skus`
- `GET /api/v1/gbm/config/sku/{goodsId}`
- `POST /api/v1/gbm/config/sku`
- `DELETE /api/v1/gbm/config/sku/{goodsId}`

请求体字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `goodsId` | string | 是 | 商品 ID |
| `goodsName` | string | 是 | 商品名 |
| `goodsImageUrl` | string | 否 | 图片 |
| `originalPrice` | number | 否 | 原价 |
| `categoryId` | integer | 否 | 分类 ID |
| `totalStock` | integer | 否 | 总库存 |
| `lockedStock` | integer | 否 | 已锁库存 |

### DCC

#### `GET /api/v1/gbm/dcc/update_config`

查询参数：

| 参数 | 必填 | 说明 |
|------|------|------|
| `key` | 是 | 配置键 |
| `value` | 是 | 配置值 |

请求样例：

```text
/api/v1/gbm/dcc/update_config?key=switch&value=true
```

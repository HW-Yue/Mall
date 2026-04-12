# 后端配置接口文档

## 概述

- **Base URL**: `/api/v1/gbm/config/`
- **说明**: 提供活动类型、商品、商品类型（类目）、拼团活动、秒杀活动、折扣配置（discount）、渠道商品活动关联、人群标签、动态配置（DCC）等配置的查询与维护。
- **数据库**: 基于 `docs/tag/v1.0/mysql/sql/2-26-group_buy_market.sql`；部分接口依赖扩展表（如 `activity_type`、`category`、`seckill_activity`），若使用 2-26 库未包含这些表，对应接口会返回空列表或需先执行 2-29 等版本 SQL。

---

## 统一响应结构

```json
{
  "code": "0000",
  "info": "成功",
  "data": { ... }
}
```

- `code`: `0000` 成功，其他为错误码（见 types 模块 ResponseCode）。
- `info`: 提示信息。
- `data`: 业务数据，列表或单条对象。

---

## 一、活动类型

### 1.1 查询活动类型列表

- **接口**: `GET /api/v1/gbm/config/activity_types`
- **说明**: 活动类型字典（如：拼团、秒杀）。表 `activity_type` 在 2-29 中存在，2-26 无此表时返回空数组。
- **响应 data**: 数组

| 字段       | 类型    | 说明         |
|------------|---------|--------------|
| id         | Integer | 自增ID       |
| typeName   | String  | 活动名称     |
| typeCode   | String  | 活动编码     |
| status     | Integer | 状态         |

### 1.2 查询单个活动类型
- **接口**: `GET /api/v1/gbm/config/activity_type/{id}`

### 1.3 新增活动类型
- **接口**: `POST /api/v1/gbm/config/activity_type`
- **请求体**: `typeName`、`typeCode` 必填；`status` 选填（默认 1）

### 1.4 更新活动类型
- **接口**: `PUT /api/v1/gbm/config/activity_type`
- **请求体**: `id` 必填；`typeName`、`typeCode`、`status` 选填

### 1.5 删除活动类型
- **接口**: `DELETE /api/v1/gbm/config/activity_type/{id}`

---

## 二、商品类目（商品类型）

### 2.1 查询类目列表

- **接口**: `GET /api/v1/gbm/config/categories`
- **说明**: 商品类目。表 `category` 在 2-29 中存在，2-26 无此表时返回空数组。
- **响应 data**: 数组

| 字段      | 类型    | 说明     |
|-----------|---------|----------|
| id        | Integer | 自增ID   |
| name      | String  | 类目名称 |
| code      | String  | 类目编码 |
| iconUrl   | String  | 图标     |
| sortOrder | Integer | 排序     |
| status    | Integer | 状态     |

### 2.2 查询单个类目
- **接口**: `GET /api/v1/gbm/config/category/{id}`

### 2.3 新增类目
- **接口**: `POST /api/v1/gbm/config/category`
- **请求体**: `name`、`code` 必填；`iconUrl`、`sortOrder`、`status` 选填

### 2.4 更新类目
- **接口**: `PUT /api/v1/gbm/config/category`
- **请求体**: `id` 必填；其余字段选填

### 2.5 删除类目
- **接口**: `DELETE /api/v1/gbm/config/category/{id}`

---

## 三、商品 SKU

### 3.1 查询商品列表

- **接口**: `GET /api/v1/gbm/config/skus`
- **响应 data**: 数组

| 字段          | 类型    | 说明       |
|---------------|---------|------------|
| id            | Long    | 自增ID     |
| source        | String  | 渠道       |
| channel       | String  | 来源       |
| goodsId       | String  | 商品ID     |
| goodsName     | String  | 商品名称   |
| goodsImageUrl | String  | 商品图片   |
| originalPrice | Decimal | 原价       |
| categoryId    | Integer | 类目ID     |
| totalStock    | Integer | 总库存     |
| lockedStock   | Integer | 锁定量     |

### 3.2 查询单个商品

- **接口**: `GET /api/v1/gbm/config/sku/{goodsId}`
- **路径参数**: `goodsId` 商品ID
- **响应 data**: 同上单条对象

### 3.3 新增/更新商品

- **接口**: `POST /api/v1/gbm/config/sku`
- **Content-Type**: `application/json`
- **请求体**:
  - `goodsId` *必填*
  - `goodsName` *必填*
  - `originalPrice` *必填*
  - `source`、`channel`、`goodsImageUrl`、`categoryId`、`totalStock`、`lockedStock` 选填（未填时 totalStock/lockedStock 默认为 0）

若 `goodsId` 已存在则更新，否则新增。

### 3.4 删除商品

- **接口**: `DELETE /api/v1/gbm/config/sku/{goodsId}`

---

## 四、拼团活动

### 4.1 查询拼团活动列表

- **接口**: `GET /api/v1/gbm/config/group_buy_activities`
- **响应 data**: 数组

| 字段            | 类型    | 说明                     |
|-----------------|---------|--------------------------|
| id              | Long    | 主键                     |
| activityId      | Long    | 活动ID                   |
| activityName    | String  | 活动名称                 |
| discountId      | String  | 折扣ID                   |
| groupType       | Integer | 拼团方式 0 自动成团 1 目标成团 |
| takeLimitCount  | Integer | 拼团次数限制             |
| target          | Integer | 拼团目标人数             |
| validTime       | Integer | 拼团时长（分钟）         |
| status          | Integer | 0 创建 1 生效 2 过期 3 废弃 |
| startTime       | DateTime| 开始时间                 |
| endTime         | DateTime| 结束时间                 |
| tagId           | String  | 人群标签规则             |
| tagScope        | String  | 人群范围                 |

### 4.2 查询单个拼团活动

- **接口**: `GET /api/v1/gbm/config/group_buy_activity/{activityId}`

### 4.3 新增/更新拼团活动

- **接口**: `POST /api/v1/gbm/config/group_buy_activity`
- **请求体**: 同上字段，`activityId`、`activityName`、`discountId` 必填；存在则更新，否则新增。
- **说明**: 更新/删除拼团活动时会自动失效对应活动缓存。

### 4.4 更新拼团活动状态

- **接口**: `PUT /api/v1/gbm/config/group_buy_activity/{activityId}/status?status=1`
- **参数**: `status` 0/1/2/3

### 4.5 删除拼团活动

- **接口**: `DELETE /api/v1/gbm/config/group_buy_activity/{activityId}`

---

## 五、秒杀活动

### 5.1 查询秒杀活动列表

- **接口**: `GET /api/v1/gbm/config/seckill_activities`
- **说明**: 表 `seckill_activity` 在 2-29 中存在，2-26 无此表时返回空数组。
- **响应 data**: 数组

| 字段          | 类型    | 说明           |
|---------------|---------|----------------|
| id            | Long    | 主键           |
| activityId    | Long    | 活动ID         |
| activityName  | String  | 活动名称       |
| discountId    | String  | 折扣ID（关联 discount，获取秒杀价） |
| goodsId       | String  | 商品ID         |
| status        | Integer | 状态           |
| startTime     | DateTime| 开始时间       |
| endTime       | DateTime| 结束时间       |
| tagId / tagScope | String | 人群标签相关   |

### 5.2 查询单个秒杀活动

- **接口**: `GET /api/v1/gbm/config/seckill_activity/{activityId}`

### 5.3 新增/更新秒杀活动

- **接口**: `POST /api/v1/gbm/config/seckill_activity`
- **请求体**: `activityId`、`activityName`、`discountId`、`goodsId` 必填；存在则更新，否则新增。秒杀价通过 discountId 关联折扣配置获取，不直接传金额。

### 5.4 更新秒杀活动状态

- **接口**: `PUT /api/v1/gbm/config/seckill_activity/{activityId}/status?status=1`

### 5.5 删除秒杀活动

- **接口**: `DELETE /api/v1/gbm/config/seckill_activity/{activityId}`

---

## 六、折扣配置（discount）

### 6.1 查询折扣列表

- **接口**: `GET /api/v1/gbm/config/group_buy_discounts`
- **响应 data**: 数组

| 字段         | 类型   | 说明                          |
|--------------|--------|-------------------------------|
| id           | Long   | 主键                          |
| discountId   | String | 折扣ID（8 位）                |
| discountName | String | 折扣标题                      |
| discountDesc | String | 折扣描述                      |
| discountType | Integer| 0 base 1 tag                  |
| marketPlan   | String | 营销计划 ZJ 直减 MJ 满减 N元购 |
| marketExpr   | String | 营销表达式（如 "20"）          |
| tagId        | String | 人群标签限定                  |

### 6.2 查询单个折扣

- **接口**: `GET /api/v1/gbm/config/group_buy_discount/{discountId}`

### 6.3 新增折扣

- **接口**: `POST /api/v1/gbm/config/group_buy_discount`
- **请求体**: `discountId`、`discountName`、`marketExpr` 必填；`discountId` 不可与已有重复。

### 6.4 更新折扣

- **接口**: `PUT /api/v1/gbm/config/group_buy_discount`
- **请求体**: 同上，需带 `discountId`。
- **说明**: 更新/删除折扣时会自动失效对应折扣缓存。

### 6.5 删除折扣

- **接口**: `DELETE /api/v1/gbm/config/group_buy_discount/{discountId}`

---

## 七、渠道商品活动关联（sc_sku_activity）

### 7.1 查询渠道商品活动列表

- **接口**: `GET /api/v1/gbm/config/sc_sku_activities?source=s01&channel=c01`
- **参数**: `source`、`channel` 选填，不传则查全部。
- **响应 data**: 数组

| 字段         | 类型  | 说明           |
|--------------|-------|----------------|
| id           | Long  | 主键           |
| source       | String| 渠道           |
| channel      | String| 来源           |
| activityId   | Long  | 活动ID         |
| activityType | String| 活动类型编码   |
| goodsId      | String| 商品ID         |

### 7.2 查询单条渠道商品活动
- **接口**: `GET /api/v1/gbm/config/sc_sku_activity/{id}`

### 7.3 新增渠道商品活动
- **接口**: `POST /api/v1/gbm/config/sc_sku_activity`
- **请求体**: `source`、`channel`、`activityId`、`goodsId` 必填；`activityType` 选填。

### 7.4 更新渠道商品活动
- **接口**: `PUT /api/v1/gbm/config/sc_sku_activity`
- **请求体**: `id` 必填；`activityId`、`goodsId`、`activityType` 选填。

### 7.5 删除渠道商品活动
- **接口**: `DELETE /api/v1/gbm/config/sc_sku_activity/{id}`
- **路径参数**: `id` 为主键 id。

---

## 八、人群标签

### 8.1 查询人群标签列表

- **接口**: `GET /api/v1/gbm/config/crowd_tags`
- **响应 data**: 数组

| 字段       | 类型    | 说明       |
|------------|---------|------------|
| id         | Integer | 自增ID     |
| tagId      | String  | 人群ID     |
| tagName    | String  | 人群名称   |
| tagDesc    | String  | 人群描述   |
| statistics | Integer | 统计量     |

### 8.2 查询单个人群标签
- **接口**: `GET /api/v1/gbm/config/crowd_tag/{tagId}`

### 8.3 新增人群标签
- **接口**: `POST /api/v1/gbm/config/crowd_tag`
- **请求体**: `tagId`、`tagName` 必填；`tagDesc`、`statistics` 选填（默认 0）。

### 8.4 更新人群标签统计量
- **接口**: `PUT /api/v1/gbm/config/crowd_tags/statistics?tagId=xxx&statistics=100`
- **参数**: `tagId`、`statistics`（覆盖为绝对值）。

### 8.5 删除人群标签
- **接口**: `DELETE /api/v1/gbm/config/crowd_tag/{tagId}`

---

## 九、动态配置变更（DCC）

### 9.1 更新动态配置

- **接口**: `GET /api/v1/gbm/dcc/update_config?key=xxx&value=yyy`
- **说明**: 与现有 DCC 接口一致，发布配置变更（如 `downgradeSwitch`、`cutRange` 等）。

---

## 错误码说明（部分）

| code | 说明       |
|------|------------|
| 0000 | 成功       |
| 0001 | 未知失败   |
| 0002 | 非法参数   |
| 0003 | 唯一索引冲突 |
| 0004 | 更新记录为0 |

---

## 数据库表与接口对应关系（2-26）

| 表名               | 接口前缀/说明                    |
|--------------------|----------------------------------|
| crowd_tags         | crowd_tags、crowd_tags/statistics |
| group_buy_activity | group_buy_activity*              |
| discount           | group_buy_discount*              |
| sku                | sku*                            |
| sc_sku_activity    | sc_sku_activity*                |
| activity_type      | activity_types（2-29 等）       |
| category           | categories（2-29 等）           |
| seckill_activity   | seckill_activity*（2-29 等）     |

扩展表（如 2-29）存在时，活动类型、类目、秒杀相关接口返回数据；否则返回空列表或需先执行对应建表 SQL。

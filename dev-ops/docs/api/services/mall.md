# mall 接口文档

## 服务职责

`mall` 负责普通商品展示、普通单下单入口、锁库解锁，以及后台商品配置。

详细参数和请求/响应样例见：[mall 详细接口](../details/mall.md)

## Base Path

- 前端入口：`/gw/api/v1/mall/**`
- 服务内实际路径：`/api/v1/mall/**`
- 其他辅助路径：
  - `/api/v1/sku/**`
  - `/api/v1/gbm/config/**`
  - `/api/v1/gbm/dcc/**`

## 面向前端的接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/mall/index/query_category_type_list` | GET | 分类列表 | 商城首页 | 商品导航 |
| `/api/v1/mall/index/query_goods_page` | POST | 商品分页 | 商城首页 | 分类下商品列表 |
| `/api/v1/mall/index/query_sku_detail` | POST | 商品详情 | 商品详情页 | 返回 SKU 详情 |
| `/api/v1/mall/index/query_activity_goods` | GET | 活动商品入口 | 商城首页 | 当前实现为聚合入口 |
| `/api/v1/mall/trade/create_normal_order` | POST | 普通商品下单 | 下单页 | 防刷、锁库、调用 `order-service` |

## 面向服务间调用的接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/sku/lock_stock` | POST | 锁库存 | `order-service` | 正常下单前置动作 |
| `/api/v1/sku/unlock_stock` | POST | 解锁库存 | `order-service` | 下单失败或补偿回滚 |

## 管理端接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/gbm/config/activity_types` | GET | 活动类型列表 | 后台页面 | |
| `/api/v1/gbm/config/activity_type/{id}` | GET | 活动类型详情 | 后台页面 | |
| `/api/v1/gbm/config/activity_type` | POST / PUT | 新增或更新活动类型 | 后台页面 | |
| `/api/v1/gbm/config/activity_type/{id}` | DELETE | 删除活动类型 | 后台页面 | |
| `/api/v1/gbm/config/categories` | GET | 分类列表 | 后台页面 | |
| `/api/v1/gbm/config/category/{id}` | GET | 分类详情 | 后台页面 | |
| `/api/v1/gbm/config/category` | POST / PUT | 新增或更新分类 | 后台页面 | |
| `/api/v1/gbm/config/category/{id}` | DELETE | 删除分类 | 后台页面 | |
| `/api/v1/gbm/config/skus` | GET | SKU 列表 | 后台页面 | |
| `/api/v1/gbm/config/sku/{goodsId}` | GET | SKU 详情 | 后台页面 | |
| `/api/v1/gbm/config/sku` | POST | 新增 SKU | 后台页面 | |
| `/api/v1/gbm/config/sku/{goodsId}` | DELETE | 删除 SKU | 后台页面 | |
| `/api/v1/gbm/dcc/update_config` | GET | 发布动态配置 | 后台页面 | DCC 配置下发 |

## 关键同步文件

- Controller：`mall/mall-trigger/src/main/java/com/yue/trigger/http/**`
- 前端路径：`dev-ops/nginx/html/js/api-config.js`
- 网关路由：`springcloud-gateway/app/src/main/resources/application-*.yml`

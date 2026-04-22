# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is a multi-module Java enterprise microservices mono-repo. All major backend services follow DDD architecture.

**Services:**
- `springcloud-gateway` — API Gateway，所有请求入口
- `mall` — 商城（Spring Boot 2.7.12, Java 8），含拼团/秒杀/普通三种营销类型
- `order-service` — 统一订单服务
- `group-buy-service` — 拼团服务
- `seckill-service` — 秒杀服务
- `pay` — 支付服务（Spring Boot 2.7.12, Java 8），对接支付宝
- `ops-agent` — 运维 Agent，动态调整商城参数配置（Sentinel 流控规则、DynamicTP 线程池配置等）。审批任务持久化在 mall 的 MySQL 实例（独立库 `ops_agent_db`，建表脚本 `mall/docs/dev-ops/mysql/sql/ops_agent.sql`）。详见 `ops-agent/CLAUDE.md`

## 完整接口清单

### mall 服务接口

| Controller | @RequestMapping | 方法 | 接口路径 | 说明 |
|------------|-----------------|------|----------|------|
| IndexController | `/api/v1/mall/index/` | GET | `/api/v1/mall/index/query_category_type_list` | 查询分类类型列表 |
| IndexController | `/api/v1/mall/index/` | POST | `/api/v1/mall/index/query_goods_page` | 查询商品分页 |
| IndexController | `/api/v1/mall/index/` | POST | `/api/v1/mall/index/query_sku_detail` | 查询 SKU 详情 |
| IndexController | `/api/v1/mall/index/` | GET | `/api/v1/mall/index/query_activity_goods` | 查询活动商品（拼团去 group-buy-service，秒杀去 seckill-service） |
| SkuController | `/api/v1/sku/` | POST | `/api/v1/sku/lock_stock` | 锁定库存（供 order-service Feign 调用） |
| SkuController | `/api/v1/sku/` | POST | `/api/v1/sku/unlock_stock` | 解锁库存（供 order-service Feign 调用） |
| GroupBuyMarketController | `/api/v1/group-buy/market/` | POST | `/api/v1/group-buy/market/query_group_buy_market_config` | **拼团市场配置查询**（在 group-buy-service） |
| BackendConfigController | `/api/v1/gbm/config/` | GET/POST/PUT/DELETE | `/api/v1/gbm/config/activity_type*` | 活动类型 CRUD |
| BackendConfigController | `/api/v1/gbm/config/` | GET/POST/PUT/DELETE | `/api/v1/gbm/config/category*` | 分类 CRUD |
| BackendConfigController | `/api/v1/gbm/config/` | GET/POST/DELETE | `/api/v1/gbm/config/sku*` | SKU CRUD |
| BackendConfigController | `/api/v1/gbm/config/` | GET | `/api/v1/gbm/config/dcc` | 更新 DCC 配置 |
| DCCController | `/api/v1/gbm/dcc/` | GET | `/api/v1/gbm/dcc/update_config` | 动态配置更新 |

### order-service 接口

| Controller | @RequestMapping | 方法 | 接口路径 | 说明 |
|------------|-----------------|------|----------|------|
| OrderController | `/api/v1/order/` | POST | `/api/v1/order/create_order` | 创建订单（普通/拼团/秒杀通用） |
| OrderController | `/api/v1/order/` | POST | `/api/v1/order/get_pay_url` | 获取支付 URL |
| OrderController | `/api/v1/order/` | POST | `/api/v1/order/refund` | 普通订单退款（前端直接调） |
| OrderController | `/api/v1/order/` | POST | `/api/v1/order/refund_execute` | 营销订单退款执行（拼团/秒杀服务调用） |
| OrderController | `/api/v1/order/` | POST | `/api/v1/order/query_user_order_list` | 查询用户订单列表 |

### group-buy-service 接口

| Controller | @RequestMapping | 方法 | 接口路径 | 说明 |
|------------|-----------------|------|----------|------|
| GroupBuyMarketController | `/api/v1/group-buy/market/` | GET | `/api/v1/group-buy/market/query_goods_list` | 查询拼团商品列表 |
| GroupBuyMarketController | `/api/v1/group-buy/market/` | POST | `/api/v1/group-buy/market/trial` | 拼团试算 |
| GroupBuyMarketController | `/api/v1/group-buy/market/` | POST | `/api/v1/group-buy/market/query_orders` | 查询进行中参团记录 |
| GroupBuyMarketController | `/api/v1/group-buy/market/` | POST | `/api/v1/group-buy/market/team_statistics` | 查询活动维度拼团统计 |
| GroupBuyTradeController | `/api/v1/group-buy/trade/` | POST | `/api/v1/group-buy/trade/create_pay_order` | 拼团下单 |
| GroupBuyTradeController | `/api/v1/group-buy/trade/` | POST | `/api/v1/group-buy/trade/refund` | 拼团退款 |

### seckill-service 接口

| Controller | @RequestMapping | 方法 | 接口路径 | 说明 |
|------------|-----------------|------|----------|------|
| SeckillMarketController | `/api/v1/seckill/market` | GET | `/api/v1/seckill/market/query_goods_list` | 查询秒杀商品列表 |
| SeckillTradeController | `/api/v1/seckill/trade` | POST | `/api/v1/seckill/trade/create_pay_order` | 秒杀下单 |
| SeckillTradeController | `/api/v1/seckill/trade` | POST | `/api/v1/seckill/trade/refund` | 秒杀退款 |

### pay 服务接口

| Controller | @RequestMapping | 方法 | 接口路径 | 说明 |
|------------|-----------------|------|----------|------|
| AliPayController | `/api/v1/alipay/` | POST | `/api/v1/alipay/create_pay_order` | 创建支付订单 |
| AliPayController | `/api/v1/alipay/` | POST | `/api/v1/alipay/alipay_notify_url` | 支付宝回调通知 |
| AliPayController | `/api/v1/alipay/` | POST | `/api/v1/alipay/active_pay_notify` | 主动查询支付状态（测试） |
| LoginController | `/api/v1/login-pay/login/` | GET | `/api/v1/login-pay/login/weixin_qrcode_ticket` | 获取微信登录二维码 |
| LoginController | `/api/v1/login-pay/login/` | GET | `/api/v1/login-pay/login/check_login` | 检查登录状态 |
| LoginController | `/api/v1/login-pay/login/` | POST | `/api/v1/login-pay/login/register` | 用户注册 |

---

## 前端接口调用流程

### 商品相关接口

**前端文件**: `mall/docs/dev-ops/nginx/html/js/mall.js`

#### 1. 查询商品类目
```javascript
// 前端调用
const url = AppApi.market(AppApiPaths.mallIndex.queryCategoryTypeList);
// GET /gw/api/v1/mall/index/query_category_type_list
```
- **网关路由**: `/gw/api/v1/mall/**` → mall 服务
- **后端 Controller**: `IndexController.queryCategoryTypeList()`

#### 2. 普通商品分页查询
```javascript
// 前端调用
const url = AppApi.market(AppApiPaths.mallIndex.queryGoodsPage);
const body = { categoryId, pageNum, pageSize };
// POST /gw/api/v1/mall/index/query_goods_page
```
- **网关路由**: `/gw/api/v1/mall/**` → mall 服务
- **后端 Controller**: `IndexController.queryGoodsPage()`

#### 3. 商品详情查询
```javascript
// 前端调用
const url = AppApi.market(AppApiPaths.mallIndex.querySkuDetail);
const body = { goodsId };
// POST /gw/api/v1/mall/index/query_sku_detail
```
- **网关路由**: `/gw/api/v1/mall/**` → mall 服务
- **后端 Controller**: `IndexController.querySkuDetail()`

---

### 订单查询接口

**前端文件**: `mall/docs/dev-ops/nginx/html/js/order-list.js`

```javascript
// 前端调用
const response = await fetch(AppApi.order(AppApiPaths.order.queryUserOrderList), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, lastId, pageSize })
});
// POST /gw/api/v1/order/query_user_order_list
```
- **网关路由**: `/gw/api/v1/order/**` → order-service
- **后端 Controller**: `OrderController.queryUserOrderList()`
- **分页方式**: 游标分页（cursor-based），通过 `lastId` 实现无限滚动

---

### 锁单流程（创建订单）

锁单成功后返回 `orderId`，前端带着 `orderId` 进入支付确认页。

#### 1. 普通商品锁单

**前端文件**: `mall/docs/dev-ops/nginx/html/js/mall.js`

```javascript
// 前端调用
const url = window.AppApi.order(P.order.createOrder);
const body = {
    userId,
    productId: String(selectedGoods.id),
    marketType: "normal",  // 普通商品
    originalPrice: selectedGoods.price,
    deductionPrice: 0,
    payPrice: selectedGoods.price,
    source: selectedGoods.source || "s01",
    channel: selectedGoods.channel || "c01",
    goodsName: selectedGoods.goodsName,
    goodsImageUrl: selectedGoods.imageUrl
};
// POST /gw/api/v1/order/create_order
```

**调用链路**:
```
前端 → 网关 → order-service → 订单落库（状态：LOCK）→ 返回 orderId
```

- **网关路由**: `/gw/api/v1/order/**` → order-service
- **后端 Controller**: `OrderController.createOrder()`

#### 2. 拼团商品锁单

**前端文件**: `mall/docs/dev-ops/nginx/html/js/mall.js`

```javascript
// 前端调用
const url = window.AppApi.groupBuy(P.gbm.createPayOrder);
const body = {
    userId,
    productId: String(productId),
    activityId,
    source: currentGroupBuyItem.source || "s01",
    channel: currentGroupBuyItem.channel || "c01",
    teamId  // 参团时传入，开团时不传
};
// POST /gw/api/v1/group-buy/trade/create_pay_order
```

**调用链路**:
```
前端 → 网关 → group-buy-service
  ↓
业务校验（活动有效性、用户限制、占库存）
  ↓
Feign 调用 → order-service/create_order
  ↓
返回 orderId
```

- **网关路由**: `/gw/api/v1/group-buy/**` → group-buy-service
- **后端 Controller**: `GroupBuyTradeController.createPayOrder()`
- **Feign 客户端**: `group-buy-service/.../gateway/IOrderService.java`
- **内部调用**: `POST http://order-service/api/v1/order/create_order`

#### 3. 秒杀商品锁单

**前端文件**: `mall/docs/dev-ops/nginx/html/js/mall.js`

```javascript
// 前端调用
const url = window.AppApi.seckill(P.seckill.createPayOrder);
const body = {
    userId,
    productId: String(currentSeckillItem.id),
    source: currentSeckillItem.source || "s01",
    channel: currentSeckillItem.channel || "c01"
};
// POST /gw/api/v1/seckill/trade/create_pay_order
```

**调用链路**:
```
前端 → 网关 → seckill-service
  ↓
业务校验（活动有效性、用户参与次数、扣减库存）
  ↓
Feign 调用 → order-service/create_order
  ↓
保存秒杀订单关联
  ↓
返回 orderId
```

- **网关路由**: `/gw/api/v1/seckill/**` → seckill-service
- **后端 Controller**: `SeckillTradeController.createPayOrder()`
- **Feign 客户端**: `seckill-service/.../gateway/IOrderService.java`
- **内部调用**: `POST http://order-service/api/v1/order/create_order`

---

### 支付流程

**前端文件**: `mall/docs/dev-ops/nginx/html/js/payment.js`

锁单成功后，前端带着 `orderId` 请求支付 URL：

```javascript
// 前端调用
const url = window.AppApi.order(window.AppApiPaths.order.getPayUrl);
fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId, orderId })
});
// POST /gw/api/v1/order/get_pay_url
```

**调用链路**:
```
前端 → 网关 → order-service/get_pay_url
  ↓
查询订单并校验状态（必须为 LOCK）
  ↓
Feign 调用 → login-pay/create_pay_order
  ↓
生成支付宝支付链接
  ↓
返回 payUrl 给前端
  ↓
前端跳转支付宝收银台
```

- **网关路由**: `/gw/api/v1/order/**` → order-service
- **后端 Controller**: `OrderController.getPayUrl()`
- **Feign 客户端**: `order-service/.../gateway/IPayService.java`
- **内部调用**: `POST http://login-pay/api/v1/alipay/create_pay_order`
- **支付服务 Controller**: `AliPayController.createPayOrder()`

---

### 服务间调用关系图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端 (mall.js)                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ 查询商品类目 │  │ 查询商品列表 │  │ 查询商品详情 │  │ 订单列表(order-list) │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
│         └────────────────┴────────────────┘                    │            │
│                          │                                     │            │
│                          ▼                                     ▼            │
│                  ┌───────────────┐                   ┌───────────────┐       │
│                  │  mall 服务     │                   │ order-service │       │
│                  └───────────────┘                   └───────┬───────┘       │
│                                                              │              │
│  ┌───────────────────────────────────────────────────────────┘              │
│  │                                                                          │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐                   │
│  │  │ 普通商品锁单 │  │ 获取支付URL  │  │  拼团/秒杀锁单    │                   │
│  └──┤             │  │             │  │                 │                   │
│     │ createOrder │  │  getPayUrl  │  │ createPayOrder  │                   │
│     └──────┬──────┘  └──────┬──────┘  └────────┬────────┘                   │
│            │                │                  │                           │
└────────────┼────────────────┼──────────────────┼───────────────────────────┘
             │                │                  │
             ▼                ▼                  ▼
        ┌─────────┐    ┌──────────┐    ┌─────────────────┐
        │  网关    │    │   网关    │    │       网关       │
        │  :8090  │    │  :8090   │    │      :8090      │
        └────┬────┘    └────┬─────┘    └────────┬────────┘
             │              │                   │
             ▼              ▼                   ▼
     ┌───────────────┐ ┌─────────────┐ ┌───────────────────┐
     │ order-service │ │  login-pay  │ │ group-buy-service │
     │               │ │  (pay服务)   │ │  /seckill-service │
     │               │ │             │ │                   │
     │               │◄┘             │ │                   │
     │               │  (Feign调用)   │ │                   │
     │               │               │ │                   │
     │◄──────────────┘               │ │                   │
     │    (Feign调用)                 │ │                   │
     │                               │ │                   │
     └───────────────────────────────┘ └───────────────────┘
```

### Feign 客户端配置

| 调用方 | 被调用方 | Feign 客户端文件 | 调用地址 |
|--------|----------|------------------|----------|
| group-buy-service | order-service | `IOrderService.java` + `OrderServiceFeignAgentConfig.java`（超时来自 `app.agent.feign.order-service.*`，启动时绑定） | `@FeignClient(name = "order-service", configuration = OrderServiceFeignAgentConfig.class)` |
| seckill-service | order-service | `seckill-service-infrastructure/.../gateway/IOrderService.java` | `@FeignClient(name = "order-service")`（无独立 Feign configuration） |
| order-service | login-pay | `order-service-infrastructure/.../gateway/IPayService.java` | `@FeignClient(name = "login-pay")` |

---

## 前端接口配置（api-config.js）

**文件路径**: `mall/docs/dev-ops/nginx/html/js/api-config.js`

### 当前配置（含已知错误）

```js
global.AppApiPaths = {
    mallIndex: {
        queryCategoryTypeList: "/gw/api/v1/mall/index/query_category_type_list",
        queryActivityGoods: "/gw/api/v1/mall/index/query_activity_goods",
        queryGoodsPage: "/gw/api/v1/mall/index/query_goods_page",
        querySkuDetail: "/gw/api/v1/mall/index/query_sku_detail",
    },
    gbm: {
        queryGroupBuyMarketConfig: "/gw/api/v1/group-buy/market/query_group_buy_market_config",
        queryGoodsList: "/gw/api/v1/group-buy/market/query_goods_list",
        createPayOrder: "/gw/api/v1/group-buy/trade/create_pay_order",
        refund: "/gw/api/v1/group-buy/trade/refund",
    },
    seckill: {
        queryGoodsList: "/gw/api/v1/seckill/market/query_goods_list",
        createPayOrder: "/gw/api/v1/seckill/trade/create_pay_order",
        refund: "/gw/api/v1/seckill/trade/refund",
    },
    order: {
        createOrder: "/gw/api/v1/order/create_order",
        getPayUrl: "/gw/api/v1/order/get_pay_url",
        refund: "/gw/api/v1/order/refund",
        queryUserOrderList: "/gw/api/v1/order/query_user_order_list",
    },
    login: {
        checkLogin: "/gw/api/v1/login-pay/login/check_login",
        register: "/gw/api/v1/login-pay/login/register",
        weixinQrcodeTicket: "/gw/api/v1/login-pay/login/weixin_qrcode_ticket",
    },
};
```

---

## ⚠️ 接口变更三位一体原则（MANDATORY）

**任何接口路径/参数/返回值的变更，必须同步修改以下三处，缺一不可：**

### 1. 后端 Controller（trigger 层）
路径格式：`/api/v1/{service}/{endpoint}`
- mall 服务路径示例：`@RequestMapping("/api/v1/mall/index/")` + `query_goods_page`

### 2. API 网关路由（springcloud-gateway）
文件：`springcloud-gateway/app/src/main/resources/application-dev.yml`（同时改 prod.yml/test.yml）

**路由规则**：`StripPrefix=1` 会去掉路径第一段 `/gw`
```
前端请求：  /gw/api/v1/mall/index/query_goods_page
网关去掉/gw：    /api/v1/mall/index/query_goods_page  → 转发给 mall 服务
```

当前路由表（Path → 服务）：
| 路由 ID | 前端路径前缀 | 转发到服务 |
|---------|------------|----------|
| 1 | `/gw/api/v1/mall/**` | `lb://mall` |
| 2 | `/gw/api/v1/login-pay/**` | `lb://login-pay` |
| order-service | `/gw/api/v1/order/**` | `lb://order-service` |
| group-buy-service | `/gw/api/v1/group-buy/**` | `lb://group-buy-service` |
| seckill-service | `/gw/api/v1/seckill/**` | `lb://seckill-service` |

### 3. 前端接口配置（mall 前端）
文件：`mall/docs/dev-ops/nginx/html/js/api-config.js` — `AppApiPaths` 对象

路径格式：`/gw` + 后端 Controller 完整路径
```js
// 示例：后端 /api/v1/mall/index/query_goods_page
queryGoodsPage: "/gw/api/v1/mall/index/query_goods_page",

// 各服务商品列表接口（数据冗余存储在各服务）
groupBuy: {
    queryGoodsList: "/gw/api/v1/group-buy/market/query_goods_list",      // 拼团商品列表
    createPayOrder: "/gw/api/v1/group-buy/trade/create_pay_order",
    refund: "/gw/api/v1/group-buy/trade/refund",
},
seckill: {
    queryGoodsList: "/gw/api/v1/seckill/market/query_goods_list",        // 秒杀商品列表
    createPayOrder: "/gw/api/v1/seckill/trade/create_pay_order",
    refund: "/gw/api/v1/seckill/trade/refund",
},
```

### 变更检查清单
- [ ] 修改了后端 `@RequestMapping` 路径 → 同步改 `api-config.js` 对应条目
- [ ] 新增后端接口 → 在 `api-config.js` 的对应分组下添加路径，确认网关有覆盖该服务的路由
- [ ] 新增服务/路由前缀 → 在网关 yml 添加路由规则，并在 `api-config.js` 注册路径
- [ ] 修改接口 DTO 字段名 → 检查前端 JS 文件（`mall.js`/`payment.js` 等）中的字段引用

---

## DDD Module Layout（所有后端项目统一结构）

```
*-api/           # 接口契约：DTO、Request/Response 对象，定义 Controller 实现的接口
*-app/           # 应用层：Spring Boot 启动类、yml 配置文件
*-domain/        # 领域层：核心业务 Service、Entity、ValueObject
*-trigger/       # 触发层：HTTP Controller（实现 api 层接口）、MQ 消费者、定时任务
                 #         trigger 内的 Service 是 Controller 和 domain 之间的编排层
*-infrastructure/# 基础设施层：数据库操作、发 MQ 消息、HTTP/RPC 外部调用
*-types/         # 通用组件：常量、枚举、异常
```

依赖规则：`trigger` → `app` → `domain` ← `infrastructure`，均依赖 `types` 和 `api`。

## 微服务职责划分

| 服务 | 职责 |
|------|------|
| `mall` | 普通商品展示、后台 CRUD 配置（活动类型/分类/SKU/DCC），**不处理下单，不展示拼团/秒杀商品** |
| `group-buy-service` | **独立维护拼团商品数据**，拼团页面商品加载、拼团下单、拼团状态管理，内部调用 order-service 创建/退款订单 |
| `seckill-service` | **独立维护秒杀商品数据**，秒杀页面商品加载、秒杀下单，内部调用 order-service 创建/退款订单 |
| `order-service` | 统一订单创建、支付 URL 获取、退款执行、订单查询；消费 pay 通知后发布下游事件 |
| `pay` | 对接支付宝，接收支付宝回调后按 marketType 发布 `pay-success-*` 三个 Topic |
| `ops-agent` | 动态调整商城参数配置（Sentinel 流控规则、DynamicTP 线程池配置等），详见 `ops-agent/CLAUDE.md` |

### 商品数据冗余策略

**设计原则**：拼团/秒杀商品数据**各自独立存储**，不依赖 mall 服务查询。

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   mall 服务      │     │ group-buy-service│     │ seckill-service │
│  (普通商品)      │     │  (拼团商品)      │     │  (秒杀商品)      │
│  t_sku 表       │     │  独立商品表      │     │  独立商品表      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
          ↑                        ↑                        ↑
          │                        │                        │
          └────────────────────────┴────────────────────────┘
                               ↓
                    后台管理配置时同步写入
```

**数据同步时机**：
- 后台创建/更新拼团活动时 → 同步将商品数据写入 `group-buy-service`
- 后台创建/更新秒杀活动时 → 同步将商品数据写入 `seckill-service`
- 各服务独立维护自己的商品展示字段（名称、图片、价格等）

**前端调用路径**：
| 页面 | 商品列表接口 | 服务 |
|------|-------------|------|
| 普通商品页 | `/api/v1/mall/index/query_goods_page` | mall |
| 拼团活动页 | `/api/v1/group-buy/market/query_goods_list` | **group-buy-service** |
| 秒杀活动页 | `/api/v1/seckill/market/query_goods_list` | **seckill-service** |

## 完整业务流程

> **核心原则**：order-service 发送 MQ 消息必须使用 **RocketMQ 事务消息（半消息）**，流程：`发半消息 → 执行本地事务（更新订单状态）→ 提交半消息`。确保数据库更新和消息发送原子性。

### 普通下单（marketType = normal）
1. 前端调 **order-service** `create_order` 创建订单
2. 前端调 **order-service** `get_pay_url` → order-service 调 pay 拿支付宝链接
3. 用户付款 → 支付宝回调 pay → pay 发布 `pay-success-normal`
4. **order-service** 消费 `pay-success-normal`
   - 发**半消息** → 更新订单状态（落库）→ **提交半消息**

### 拼团下单（marketType = group_buy）
1. **前端调 group-buy-service** `/api/v1/group-buy/market/query_goods_list` 加载拼团商品列表
2. 用户选择商品 → 前端调 **group-buy-service** `/api/v1/group-buy/market/query_group_buy_market_config` 获取拼团配置（含进行中的团）
3. 前端调 **group-buy-service** `/api/v1/group-buy/trade/create_pay_order` 下单 → 内部 HTTP 调 order-service `create_order`
4. 前端调 **order-service** `get_pay_url` 获取支付宝链接
5. 用户付款 → 支付宝回调 pay → pay 发布 `pay-success-group-buy`
6. **order-service** 消费 `pay-success-group-buy`
   - 发**半消息** → 更新订单状态（落库）→ **提交半消息**通知 **group-buy-service**（`order-paid-group_buy`）
7. **group-buy-service** 消费 `order-paid-group_buy`，更新组队状态
8. **成团后**：group-buy-service 调用 **order-service** 结算接口
9. **order-service** 结算完成
   - 发**半消息** → 更新结算状态（落库）→ **提交半消息**
10. 退款：前端调 **group-buy-service** → HTTP 调 order-service `refund`

### 秒杀下单（marketType = seckill）
1. **前端调 seckill-service** `/api/v1/seckill/market/query_goods_list` 加载秒杀商品列表
2. 用户选择商品 → 前端调 **seckill-service** `/api/v1/seckill/trade/create_pay_order` 下单 → 内部 HTTP 调 order-service `create_order`
3. 前端调 **order-service** `get_pay_url` 获取支付宝链接
4. 用户付款 → 支付宝回调 pay → pay 发布 `pay-success-seckill`
5. **order-service** 消费 `pay-success-seckill`
   - 发**半消息** → 更新订单状态（落库）→ **同时提交两条半消息**：
     - 通知 **seckill-service**（`order-paid-seckill`）
6. **seckill-service** 消费消息，更新秒杀订单状态
7. 退款：前端调 **seckill-service** → HTTP 调 order-service `refund`

### 退款流程
- Domain 层逻辑已写完，trigger 层接口（HTTP 入口）尚未实现

## 关键文件路径

### SQL 文件（所有建表/初始数据）
路径：`mall/docs/dev-ops/mysql/sql/`

| 文件 | 对应服务 |
|------|---------|
| `mall_db.sql` | mall 服务 |
| `order_service.sql` | order-service |
| `group_buy_service.sql` | group-buy-service |
| `seckill_service.sql` | seckill-service |
| `grafana.sql` | 监控看板 |

> 新增表或字段时，必须同步更新对应 SQL 文件。

### 网关 & 前端（改接口必看）
- **网关路由配置：`springcloud-gateway/app/src/main/resources/application-dev.yml`**
- **前端接口路径集中配置：`mall/docs/dev-ops/nginx/html/js/api-config.js`**（`AppApiPaths` 对象）

### Order Service
- Controller：`order-service/order-service-trigger/src/main/java/com/yue/order/trigger/http/OrderController.java`
- pay 回调消费（普通）：`order-service/order-service-trigger/.../listener/PaySuccessListener.java`
- pay 回调消费（拼团）：`order-service/order-service-trigger/.../listener/PaySuccessGroupBuyListener.java`
- pay 回调消费（秒杀）：`order-service/order-service-trigger/.../listener/PaySuccessSeckillListener.java`
- 发布下游事件（MQ 生产者）：`order-service/order-service-infrastructure/.../event/OrderPaidMqProducer.java`

### Group-Buy Service
- 拼团商品列表接口：`group-buy-service/group-buy-service-trigger/.../http/GroupBuyMarketController.java`
- 拼团交易 Controller：`group-buy-service/group-buy-service-trigger/.../http/GroupBuyTradeController.java`
- 调用 order-service 的 Port：`group-buy-service/group-buy-service-infrastructure/.../adapter/port/OrderServicePort.java`
- 订单支付成功消费者：`group-buy-service/group-buy-service-trigger/.../listener/OrderPaidGroupBuyListener.java`

### Seckill Service
- 秒杀商品列表接口：`seckill-service/seckill-service-trigger/.../http/SeckillMarketController.java`
- 秒杀交易 Controller：`seckill-service/seckill-service-trigger/.../http/SeckillTradeController.java`
- 秒杀市场 Service：`seckill-service/seckill-service-domain/.../activity/service/SeckillMarketServiceImpl.java`
- 秒杀交易 Service：`seckill-service/seckill-service-domain/.../trade/service/SeckillTradeServiceImpl.java`
- 秒杀活动 Repository：`seckill-service/seckill-service-infrastructure/.../adapter/repository/SeckillActivityRepository.java`
- 秒杀订单 Repository：`seckill-service/seckill-service-infrastructure/.../adapter/repository/SeckillOrderRepository.java`
- 调用 order-service 的 Port：`seckill-service/seckill-service-infrastructure/.../adapter/port/OrderServicePort.java`
- MQ 消费者（支付成功）：`seckill-service/seckill-service-trigger/.../listener/OrderPaidSeckillListener.java`
- **秒杀商品表：`mall/docs/dev-ops/mysql/sql/seckill_service.sql`**

#### seckill_service 核心表设计

| 表名 | 说明 |
|------|------|
| `sms_seckill_sku` | 秒杀商品 SKU 快照（核心商品表，见下方字段设计） |
| `sc_sku_activity` | 渠道-SKU-活动映射，`sku_id` 关联 `sms_seckill_sku.sku_id` |
| `seckill_activity` | 秒杀活动 |
| `t_order_seckill` | 秒杀订单扩展，`sku_id` 关联 `sms_seckill_sku.sku_id` |
| `discount` | 折扣配置 |
| `category` | 商品类目 |

**`sms_seckill_sku` 字段设计：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `sku_id` | varchar(16) | SKU ID，对应 mall 主库 `t_sku.sku_id`，唯一键 |
| `spu_id` | varchar(16) | SPU ID，对应 mall 主库 `t_spu.spu_id` |
| `spu_name` | varchar(128) | SPU 名称（商品主名称，如"GPT-4"） |
| `sku_spec_json` | varchar(512) | SKU 规格 JSON（如 `{"token":"100万"}`） |
| `goods_image_url` | varchar(512) | 商品图片 URL |
| `goods_detail` | text | 商品详情 |
| `original_price` | decimal(10,2) | 商品原价 |
| `category_id` | int unsigned | 所属类目 ID |

> **前端展示名称规则**：`goodsName = spuName + " " + skuSpecJson`，在接口层（Controller 或 Repository 组装时）拼接，DTO 中仍暴露 `goodsName` 字段供前端无感知使用。`goodsId` 统一用 `skuId` 替代。

### Mall 服务（商城展示 & 后台配置，不处理下单）
- 商品/活动展示 Controller：`mall/mall-trigger/src/main/java/com/yue/trigger/http/IndexController.java`
- 后台 CRUD：`mall/mall-trigger/src/main/java/com/yue/trigger/http/admin/BackendConfigController.java`

### Pay 服务
- 支付宝回调入口：`pay/pay-trigger/src/main/java/cn/bugstack/trigger/http/AliPayController.java`
- RocketMQ 按类型路由发送：`pay/pay-infrastructure/src/main/java/cn/bugstack/infrastructure/adapter/port/PaySuccessRocketMqPort.java`

## MQ 规划（RocketMQ）

### pay → order-service（支付宝回调后触发）
| Topic | 生产者 | 消费者 | 消费者 Listener |
|-------|--------|--------|----------------|
| `pay-success-normal` | pay | order-service | `PaySuccessListener` |
| `pay-success-group-buy` | pay | order-service | `PaySuccessGroupBuyListener` |
| `pay-success-seckill` | pay | order-service | `PaySuccessSeckillListener` |

### order-service → 下游（事务消息，半消息模式）

| 场景 | Topic | 生产者 | 消费者 | 说明 |
|------|-------|--------|--------|------|
| group_buy 支付成功 | `order-paid-group_buy` | order-service | **group-buy-service** | 通知 group-buy 更新组队状态 |
| seckill 支付成功 | `order-paid-seckill` | order-service | **seckill-service** | 通知 seckill 更新订单状态 |

> **事务消息保证**：order-service 发送以上消息必须使用 `RocketMQTemplate.sendMessageInTransaction()`（半消息），本地事务为更新订单状态，确保消息和数据库状态一致。

RabbitMQ 仍用于：mall 内部的退款通知（`RefundSuccessTopicListener`，group_buy_market_exchange）

## Build & Run Commands

### Backend (Maven)
```bash
mvn clean package -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn test
mvn clean package -pl <module-name> -am
```

### Docker
```bash
docker-compose -f docs/dev-ops/docker-compose-environment.yml up -d
docker-compose -f docs/dev-ops/docker-compose-app.yml up -d
```

## Key Technology Versions

| Subsystem | Java | Spring Boot |
|-----------|------|-------------|
| mall | 8 | 2.7.12 |
| pay | 8 | 2.7.12 |

ORM: MyBatis，Cache: Redisson，MQ: RocketMQ（主），RabbitMQ（mall 内部退款）

**Redis 客户端统一约定（2026 版）**：所有服务都用官方 `redisson-spring-boot-starter:3.26.0`，配置键统一在 `spring.data.redis.*`（host/port/password/client-name）+ `spring.redis.redisson.config`（YAML 片段），不再使用 Lettuce / 自研 `redis.sdk.config` / 自定义 `RedissonConfig`。每个服务的 `client-name` 必须唯一（`mall-redisson` / `order-redisson` / `seckill-redisson` / `groupbuy-redisson` / `pay-redisson`），`RedissonClient` 与 `StringRedisTemplate` 共享同一条连接。client-name → application 的反查表写在 `ops-agent/src/main/resources/application.yml` 的 `ops-agent.mapping.client-name-to-app`。

**Hikari 连接池命名**：每个服务的 `spring.datasource.hikari.pool-name` 也全局唯一（`Mall_HikariCP` / `Order_HikariCP` / `Seckill_HikariCP` / `GroupBuy_HikariCP` / `Pay_HikariCP`）。Prometheus 采集时 pool 名透传到 `hikaricp_connections_*{pool="..."}`，`HikariTuningStrategy` 据此反查 `ops-agent.mapping.pool-name-to-app` 定位 Nacos dataId。

## Spring Profiles

所有服务使用 `dev` / `test` / `prod`，配置文件在各 app 模块 `src/main/resources/application-{profile}.yml`，本地默认 `dev`。

## Application Entry Points

| Project | Main Class | 模块 |
|---------|-----------|------|
| mall | `com.yue.MallApplication` | `mall-app` |
| order-service | `com.yue.order.OrderServiceApplication` | `order-service-app` |
| group-buy-service | `com.yue.groupbuy.GroupBuyServiceApplication` | `group-buy-service-app` |
| seckill-service | `com.yue.seckill.SeckillServiceApplication` | `seckill-service-app` |
| pay | `cn.bugstack.PayApplication` | `pay-app` |
| springcloud-gateway | `cn.bugstack.xfg.dev.tech.SpringcloudGatewayApplication` | `app` |

---

## Prometheus & Sentinel & DynamicTP 技术配置

> 目标：各商城服务暴露 Sentinel / DynamicTP / JVM 指标到 Prometheus，ops-agent 根据告警动态调整配置。ops-agent 详见 `ops-agent/CLAUDE.md`。

### 指标暴露现状

| 指标来源 | 实现位置 | 暴露指标 | 状态 |
|---------|---------|---------|------|
| **Sentinel** | `Dependencies/common-log-starter/.../sentinel/SentinelMetricsBinder.java` | `sentinel_pass_qps`, `sentinel_block_qps`, `sentinel_success_qps`, `sentinel_exception_qps`, `sentinel_rt`, `sentinel_current_thread`, `sentinel_total_qps` | ✅ 自动扫描 `ClusterBuilderSlot.getClusterNodeMap()` 注册 Gauge |
| **DynamicTP** | `org.dromara.dynamictp` 内置 `MicrometerCollector` | `thread_pool_active_count`, `thread_pool_pool_size`, `thread_pool_core_pool_size`, `thread_pool_maximum_pool_size`, `thread_pool_queue_size`, `thread_pool_queue_remaining` | ✅ 各服务 `dtp-dev.yml` 中配置 `collector-types: [micrometer, internal_logging]` |
| **JVM/业务** | Spring Boot Actuator + Micrometer | `jvm_*`, `tomcat_*`, `http_server_requests_*`, `system_*` | ✅ 依赖 `spring-boot-starter-actuator` + `micrometer-registry-prometheus` |

**各服务 Actuator 端点已开启：**
- `management.endpoints.web.exposure.include: health,prometheus`
- 访问：`http://{host}:{port}/actuator/prometheus`

### Prometheus 配置

**主配置文件：** `mall/docs/dev-ops/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "/etc/prometheus/alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

scrape_configs:
  - job_name: 'nacos-sd-spring-boot'
    metrics_path: '/actuator/prometheus'
    honor_labels: true
    http_sd_configs:
      - url: 'http://nacos:8848/nacos/prometheus'
        refresh_interval: 30s
    relabel_configs:
      - source_labels: [__address__]
        regex: '127\.0\.0\.1:(.*)'
        replacement: 'host.docker.internal:${1}'
        target_label: __address__
```

> 前提：Nacos 需开启 `NACOS_PROMETHEUS_METRICS_ENABLED=true`，`curl http://nacos:8848/nacos/prometheus` 返回服务列表。

**告警规则文件：** `mall/docs/dev-ops/prometheus/alert_rules.yml`

按服务 + 按指标类型分组，详见下方「告警规则」小节。

### Alertmanager 配置

**配置文件：** `mall/docs/dev-ops/prometheus/alertmanager.yml`

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'category', 'application']
  group_wait: 10s
  group_interval: 30s
  repeat_interval: 12h
  receiver: 'ops-agent'

receivers:
  - name: 'ops-agent'
    webhook_configs:
      - url: 'http://host.docker.internal:8098/api/v1/alert/receive'
        send_resolved: true
```

> `host.docker.internal:8098` 指向宿主机的 ops-agent 服务（端口见 `ops-agent/src/main/resources/application.yml`）。生产环境替换为实际地址。

ops-agent 接收端：`ops-agent/src/main/java/com/yue/opsagent/controller/AlertReceiveController.java`。告警进入后依次交给：

1. `LoggingAlertHandler`（@Order 10）打印结构化日志。
2. `AgentTriggeringAlertHandler`（@Order 20）按 `alertname|application|resource` 做节流，再按策略路由（`@Component` 自动扫描，同时由 `AlertStrategy.order()` 排序）：
   - **Nacos Write 域**（继承 `AbstractNacosWriteStrategy`）→ 专属 Agent → `publishConfig` 工具 → 审批队列 → 人工同意后落 Nacos：
     - `SentinelFlowRuleStrategy`（`category=sentinel`，order=10）→ `sentinel-ops` Agent → `${app}-flow-rules.json` @ `SENTINEL_GROUP`
     - `DynamicTpStrategy`（`category=dynamictp`，order=20）→ `dtp-ops` Agent → `${app}-dtp-${profile}.yml` @ `DEFAULT_GROUP`
     - `MySqlTuningStrategy`（`category=mysql`，order=30）→ `mysql-ops` Agent → `shared-mysql-tuning.yml` @ `DEFAULT_GROUP`（application=shared，不分服务）
     - `HikariTuningStrategy`（`category=hikari`，order=35）→ `hikari-ops` Agent → 由 `labels.pool` 反查 `ops-agent.mapping.pool-name-to-app` 拿到 `${app}-datasource-dev.yml`
   - **NotifyOnly 域**（继承 `AbstractNotifyOnlyStrategy`）→ 不改 Nacos，直接投 `ApprovalTask` 进 inbox，`ApprovalService.approve` 对 `domain=notify/redis/rocketmq` 短路为 `APPLIED`：
     - `RedisNotifyStrategy`（`category=redis`，order=40）→ **继承 `AbstractDiagnoseAgentStrategy`**：先用占位 reasoning 落 PENDING 任务，再异步调 `DiagnoseAgent`（ES MCP 子 Agent）产出四段式诊断文本回填 reasoning
     - `RocketMqNotifyStrategy`（`category=rocketmq`，order=50）→ Broker / 消费堆积 / DLQ 类告警
     - `NotifyOnlyStrategy`（`domain=notify`，order=100）→ 兜底：`Http5xxErrorRateHigh` / `Jvm*` / `ServiceDown`

所有策略实现都在 `ops-agent/src/main/java/com/yue/opsagent/adapter/strategy/`。新接一个 exporter 领域只需：① 在 `application.yml` 加 `ops-agent.prompts.<domain>` + `ops-agent.approval.strategies.<domain>`；② 新建一个 `@Component` 继承 `AbstractNacosWriteStrategy`（写 Nacos）/ `AbstractNotifyOnlyStrategy`（仅通知）/ `AbstractDiagnoseAgentStrategy`（通知 + ES MCP 异步诊断）三选一；③ 在 `devops/css/app.css` 加一行徽章 CSS（`.badge.domain.<name>`）。详见 `ops-agent/CLAUDE.md` 中的 "Adding a new alert domain" 三步指南。

> 审批任务持久化：默认走 MySQL（`ops-agent.approval.storage.type=mysql`），落在独立库 `ops_agent_db.approval_task`，建表脚本 `mall/docs/dev-ops/mysql/sql/ops_agent.sql`；设置为 `memory` 可降级回内存 `InMemoryApprovalInbox`（仅调试）。

当前接入的 **36** 条告警按域划分：

| 域 | 告警 alertname |
|----|---------------|
| **sentinel**（15） | `SentinelBlockRateHigh`, `SentinelExceptionRateHigh`, `SentinelRtHigh`, `SentinelPeakRtHigh`, `SentinelThreadCountHigh`, `OrderServiceCreateOrderBlockHigh`, `OrderServiceCreateOrderRtHigh`, `OrderServiceGetPayUrlRtHigh`, `SeckillServiceRtHigh`, `SeckillServiceBlockHigh`, `GroupBuyServiceCreatePayOrderBlockHigh`, `PayServiceExceptionHigh`, `PayServiceCreatePayOrderRtHigh`, `MallServiceQueryGoodsPageBlockHigh`, `GatewayBlockRateHigh` |
| **dtp**（5） | `ThreadPoolActiveRatioHigh`, `ThreadPoolQueueUsageHigh`, `ThreadPoolAtMaxCapacity`, `ThreadPoolRejectedTasks`, `OrderServiceThreadPoolBusy` |
| **hikari**（3） | `HikariConnectionsSaturated`, `HikariConnectionsPending`, `HikariConnectionAcquireSlow` |
| **mysql**（4） | `MySqlDown`, `MySqlTooManyConnections`, `MySqlSlowQueriesHigh`, `MySqlInnodbRowLockWaitHigh` |
| **redis**（5） | `RedisDown`, `RedisMemoryHigh`, `RedisConnectedClientsHigh`, `RedisKeyspaceHitRateLow`, `RedisBlockedClients` |
| **rocketmq**（3） | `RocketMqBrokerDown`, `RocketMqConsumerLagHigh`, `RocketMqDlqMessageAppeared` |
| **notify**（4） | `Http5xxErrorRateHigh`, `JvmHeapUsageHigh`, `JvmGcPauseHigh`, `ServiceDown` |

> **三个 exporter（mysqld / redis / rocketmq）** 不在主 `docker-compose-grafana.yml` 里，单独放在 `mall/docs/dev-ops/docker-compose-exporters.yml`，统一落在 `nexus-devops` 网络。启动前请先确认主 compose 已启动（`nexus-devops` 网络由它创建），再 `docker compose -f docker-compose-exporters.yml up -d`。三者 Prometheus scrape 均带 `application=shared` + `category=mysql/redis/rocketmq` 标签，用于 ops-agent 路由。

### ELK 9.x 与 ES MCP 诊断子 Agent

**全链路日志**：五个微服务通过 `common-log-starter` 把结构化 JSON（含 `trace-id` / `service` / `level`）TCP 推到 Logstash（`:4560`），落到 Elasticsearch 的 `nexus-<service>-YYYY.MM.dd` 索引，Kibana 走 `:5601` 查询。

**2026 升级**：`docker-compose-elk.yml` 已从 7.17.28 整体升到 **9.2.0**（ES / Logstash / Kibana 同版本，显式 `xpack.security.enabled=false` 与 dev 的无鉴权链路对齐）。升级后数据卷从 `./data` bind-mount 换成命名卷 `nexus-esdata`；7 → 9 索引格式不兼容，首次升级需 `docker compose -f docker-compose-elk.yml down -v` 清旧数据。业务服务代码零改。

**ES MCP Server**：新增 `docker-compose-mcp.yml`，跑官方 `docker.elastic.co/mcp/elasticsearch:latest`；容器内同时提供 streamable-HTTP `:8080/mcp` 与旧版 HTTP+SSE `:8080/mcp/sse`（宿主机映射 `:8085`）。ops-agent 默认配置走 **SSE**（`/mcp/sse`），避免部分环境下 streamable-HTTP 在 GET 阶段因会话 ID 与 Java `mcp-core` 握手不一致出现 `401 Session ID is required`。工具集仍为 `list_indices / get_mappings / search / esql / get_shards`。Redis 域告警触发时 `DiagnoseAgent` 异步查 ES 日志，四段式诊断回填 inbox `reasoning`。MCP 不可用则 `AbstractDiagnoseAgentStrategy` 降级写 `[诊断失败] ...`。

启动顺序：`docker-compose-environment.yml`（建 nexus-devops 网络）→ `docker-compose-elk.yml` → `docker-compose-mcp.yml`。关闭时反向执行。

### Sentinel Nacos 规则格式

**Nacos dataId：** `{spring.application.name}-flow-rules.json`
**Nacos groupId：** `SENTINEL_GROUP`
**格式：** JSON 数组

```json
[
  {
    "resource": "/api/v1/order/create_order",
    "limitApp": "default",
    "grade": 1,
    "count": 500,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

| 字段 | 含义 |
|------|------|
| `resource` | 资源名，`sentinel-spring-webmvc-v6x-adapter` 生成的实际格式为**纯 URI**（如 `/api/v1/order/create_order`），不带 HTTP Method 前缀。以 `/actuator/prometheus` 的 `sentinel_*{resource="..."}` 为准。 |
| `grade` | 阈值类型：`0`=并发线程数，`1`=QPS |
| `count` | 阈值数值 |
| `strategy` | 流控模式：`0`=直接，`1`=关联，`2`=链路 |
| `controlBehavior` | 控制效果：`0`=快速失败，`1`=Warm Up，`2`=匀速排队 |
| `maxQueueingTimeMs` | `controlBehavior=2` 时排队等待时间 |

> 各服务规则文件位于 `mall/docs/dev-ops/nacos/sentinel-rules/`，新增/修改规则后需同步推送到 Nacos。

### DynamicTP 配置格式

**Nacos dataId：** `{spring.application.name}-dtp-dev.yml`
**Nacos groupId：** `DEFAULT_GROUP`

```yaml
spring:
  dynamic:
    tp:
      enabled: true
      enabled-collect: true
      collector-types:
        - micrometer       # 关键：开启 Micrometer 指标导出
        - internal_logging
      monitor-interval: 30
      executors:
        - thread-pool-name: threadPoolExecutor
          core-pool-size: 5
          maximum-pool-size: 20
          queue-capacity: 2000
          queue-type: LinkedBlockingQueue
          rejected-handler-type: CallerRunsPolicy
      tomcat-tp:
        core-pool-size: 20
        maximum-pool-size: 200
```

> ops-agent 修改 DynamicTP 配置时，通过 Redis Topic `DYNAMIC_THREAD_POOL_REDIS_TOPIC_{appName}` 推送 `ThreadPoolConfigEntity`，DTP 客户端自动热加载。

### 运维配置关键文件路径

| 用途 | 文件路径 |
|------|---------|
| Prometheus 主配置 | `mall/docs/dev-ops/prometheus/prometheus.yml` |
| Prometheus 告警规则 | `mall/docs/dev-ops/prometheus/alert_rules.yml` |
| Alertmanager 配置 | `mall/docs/dev-ops/prometheus/alertmanager.yml` |
| docker-compose（含 Prometheus + Alertmanager + Grafana） | `mall/docs/dev-ops/docker-compose-grafana.yml` |
| Sentinel 规则源文件 | `mall/docs/dev-ops/nacos/sentinel-rules/*-flow-rules.json` |
| DTP 配置源文件 | `mall/docs/dev-ops/nacos/dtp-config/*-dtp-dev.yml` |
| Sentinel 指标绑定（common-log-starter） | `Dependencies/common-log-starter/src/main/java/com/yue/common/log/sentinel/SentinelMetricsBinder.java` |
| Sentinel 增强自动配置 | `Dependencies/common-log-starter/src/main/java/com/yue/common/log/sentinel/SentinelEnhanceAutoConfiguration.java` |

### 监控接入检查清单

- [ ] Prometheus `alert_rules.yml` 已创建，docker-compose 中已挂载
- [ ] Alertmanager `alertmanager.yml` 已创建，docker-compose 中已添加 alertmanager 服务
- [ ] 先打一次业务请求（如 `curl http://localhost:8093/api/v1/group-buy/market/query_goods_list?...`），再 `curl http://localhost:8093/actuator/prometheus | grep sentinel_pass_qps` 确认有数据（`SentinelMetricsBinder` 只为已触发过的 resource 注册 Gauge）
- [ ] 目标服务 `/actuator/prometheus` 能访问到 Sentinel 指标（`sentinel_pass_qps`）和 DTP 指标（`thread_pool_active_count`）
- [ ] Prometheus UI `http://localhost:9090/targets` 中目标服务为 UP
- [ ] Prometheus UI `http://localhost:9090/rules` 所有 group 已加载；`http://localhost:9090/alerts` 告警规则状态为 Inactive
- [ ] Alertmanager UI `http://localhost:9093` 可访问；ops-agent 日志 grep `[Alert]` 能看到 webhook payload

---

## 告警规则（Prometheus alert_rules.yml）

文件：`mall/docs/dev-ops/prometheus/alert_rules.yml`

标签规范：
- **Sentinel 告警** 使用 `{app="xxx"}`（由 `SentinelMetricsBinder` 手动打的标签；指标上同时有 Micrometer 注入的 `application` 标签，但为保持仓库历史一致继续使用 `app`）。
- **DTP / JVM / Hikari / HTTP / system** 类告警使用 `{application="xxx"}`（Micrometer common tag，见各服务 `application-dev.yml` 的 `management.metrics.tags.application`）。
- **Sentinel resource 为纯 URI**，不含 HTTP Method 前缀。例如 `resource="/api/v1/order/create_order"`。

DTP 指标字段名速查（按实测 `/actuator/prometheus` 输出校对）：

| 用途 | 指标名 |
|------|--------|
| 活跃线程 | `thread_pool_active_count` |
| 当前线程 | `thread_pool_current_size` |
| 最大线程 | `thread_pool_maximum_size` |
| 核心线程 | `thread_pool_core_size` |
| 队列堆积 | `thread_pool_queue_size` |
| 队列剩余 | `thread_pool_queue_remaining_capacity` |
| 拒绝次数 | `thread_pool_reject_count` |

告警分组：

| group | 粒度 | 触发示例 |
|-------|------|---------|
| `sentinel-global` | 所有 Sentinel 资源 | block / 异常率 / RT / 峰值 RT / 并发线程数 |
| `dynamictp-global` | 所有 DTP 线程池 | 活跃率 / 队列使用率 / 达到 max / 出现拒绝 |
| `order-service-specific` | create_order、get_pay_url、threadPoolExecutor | 核心下单/支付链路阈值更严 |
| `seckill-service-specific` | 所有 Sentinel 资源 | RT > 200ms / block > 100 |
| `group-buy-service-specific` | `/api/v1/group-buy/trade/.*` | 拼团交易 block 频繁 |
| `pay-service-specific` | `login-pay` 所有资源 | 异常率 > 1%、`create_pay_order` RT > 500ms |
| `mall-service-specific` | `query_goods_page` | 商品查询 block 频繁 |
| `gateway-specific` | `springcloud-gateway` 所有资源 | 网关层限流 |
| `http-layer-global` | 所有服务 | HTTP 5xx 错误率 > 5% |
| `hikari-global` | 所有服务 | 连接池使用率 > 85%、存在 pending |
| `jvm-system-global` | 所有服务 | JVM 堆 / GC / `up == 0` |

> **不再使用**：基于 histogram bucket 的 P99 RT（Binder 未暴露 `sentinel_rt_bucket`，改用 `max_over_time(sentinel_rt[5m])`）；`tomcat_threads_busy_*`（Spring Boot 默认 MeterRegistry 不暴露）。

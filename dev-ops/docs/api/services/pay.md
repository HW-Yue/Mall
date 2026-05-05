# pay 接口文档

## 服务职责

`pay` 负责支付订单创建、支付宝异步回调处理、主动查询支付状态，以及登录和微信扫码相关接口。

详细参数和请求/响应样例见：[pay 详细接口](../details/pay.md)

## Base Path

- 网关前端入口：
  - `/gw/api/v1/alipay/**`
  - `/gw/api/v1/pay/**`
- 服务内实际路径：
  - `/api/v1/alipay/**`
  - `/api/v1/pay/login/**`
  - `/api/v1/pay/weixin/portal/**`

## 支付接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/alipay/create_pay_order` | POST | 创建支付订单 | `order-service` / 前端支付页 | 根据 `marketType` 创建支付单 |
| `/api/v1/alipay/alipay_notify_url` | POST | 支付宝异步回调 | 支付宝 | 支付成功后向下游发消息 |
| `/api/v1/alipay/active_pay_notify` | POST | 主动查询支付状态 | 测试 / 运维 | 调支付宝查询接口 |

## 登录接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/pay/login/weixin_qrcode_ticket` | GET | 生成扫码登录 ticket | 登录页 | 默认场景 |
| `/api/v1/pay/login/weixin_qrcode_ticket_scene` | GET | 按场景生成扫码 ticket | 登录页 | 带 `sceneStr` |
| `/api/v1/pay/login/check_login` | GET | 检查扫码登录状态 | 登录页 | |
| `/api/v1/pay/login/check_login_scene` | GET | 按场景检查扫码状态 | 登录页 | 带 `sceneStr` |
| `/api/v1/pay/login/register` | POST | 注册并绑定 ticket | 登录页 | 用户首次扫码未绑定时调用 |

## 微信门户接口

| 路径 | 方法 | 说明 | 调用方 | 备注 |
|------|------|------|--------|------|
| `/api/v1/pay/weixin/portal/receive` | GET | 微信公众号验签 | 微信平台 | 返回 `echostr` |
| `/api/v1/pay/weixin/portal/receive` | POST | 接收微信消息 / 扫码事件 | 微信平台 | 扫码登录状态回写 |

## 关键同步文件

- Controller：`pay/pay-trigger/src/main/java/cn/bugstack/trigger/http/**`
- 前端路径：`dev-ops/nginx/html/js/api-config.js`
- 网关路由：`springcloud-gateway/app/src/main/resources/application-*.yml`

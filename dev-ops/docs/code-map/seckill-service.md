# seckill-service 代码地图

## 启动与配置

- 启动模块：`seckill-service/seckill-service-app`
- 启动类：`com.yue.seckill.SeckillServiceApplication`
- 配置文件：
  - `seckill-service/seckill-service-app/src/main/resources/application-dev.yml`
  - `seckill-service/seckill-service-app/src/main/resources/application-test.yml`
  - `seckill-service/seckill-service-app/src/main/resources/application-prod.yml`
  - `seckill-service/seckill-service-app/src/main/resources/application-test-mock.yml`

## HTTP 入口

- 市场查询与后台：
  - `seckill-service/seckill-service-trigger/src/main/java/com/yue/seckill/trigger/http/SeckillMarketController.java`
- 交易入口：
  - `seckill-service/seckill-service-trigger/src/main/java/com/yue/seckill/trigger/http/SeckillTradeController.java`
- API 契约：
  - `seckill-service/seckill-service-api/src/main/java/com/yue/seckill/api/`

## 核心业务

- 市场领域服务：
  - `seckill-service/seckill-service-domain/src/main/java/com/yue/seckill/domain/market/service/SeckillMarketServiceImpl.java`
- 交易领域服务：
  - `seckill-service/seckill-service-domain/src/main/java/com/yue/seckill/domain/trade/service/SeckillTradeServiceImpl.java`

## 下游调用与适配

- 调 `order-service` 的 Port：
  - `seckill-service/seckill-service-infrastructure/src/main/java/com/yue/seckill/infrastructure/adapter/port/OrderServicePort.java`

## MQ

### 消费者

- 支付成功后真实库存扣减：
  - `seckill-service/seckill-service-trigger/src/main/java/com/yue/seckill/trigger/listener/OrderPaidSeckillListener.java`
- 关单恢复可售库存：
  - `OrderCloseSeckillListener.java`
- 退款恢复 Redis / MySQL 库存：
  - `PayRefundSeckillListener.java`
- MySQL 库存异步回写：
  - `SeckillStockDeductListener.java`

### 生产者

- 异步建单消息：
  - `seckill-service/seckill-service-infrastructure/src/main/java/com/yue/seckill/infrastructure/adapter/port/SeckillOrderTaskPort.java`
- 库存异步回写消息：
  - `SeckillStockDeductPort.java`

## 什么时候先看这里

- 秒杀下单
- 秒杀 Redis 预占 / 真实库存 / MySQL 回写
- 秒杀退款与库存恢复
- `seckill-service -> order-service` 的 Port 适配

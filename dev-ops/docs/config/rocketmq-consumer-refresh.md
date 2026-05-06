# RocketMQ 消费线程池动态更新

本文说明 RocketMQ 消费者线程池这类配置更新的实现原理。

## 支持范围

当前已接入动态更新的服务：

- `order-service`
- `group-buy-service`
- `seckill-service`
- `pay`

`mall` 当前没有 RocketMQ 消费者监听器，所以不涉及这一类配置。

## 配置入口

各服务分别使用独立 DataId：

- `order-service-mq-dev.yml`
- `group-buy-service-mq-dev.yml`
- `seckill-service-mq-dev.yml`
- `pay-service-mq-dev.yml`

配置结构按 `consumerGroup` 维度展开，例如：

```yaml
app:
  rocketmq:
    consumer-thread-pool:
      consumers:
        CG_NORMAL_ORDER_CREATE:
          consume-thread-number: 20
          consume-thread-max: 64
```

## 实现原理

当前监听器仍然使用 `@RocketMQMessageListener` 注册，注解里主要定义：

- `topic`
- `consumerGroup`

没有把线程数直接写在每个监听器类里做动态占位，而是统一追加了一层运行时刷新器。

各服务都新增了两类 Bean：

1. `RocketMqConsumerThreadPoolProperties`
   通过 `@ConfigurationProperties` + `@RefreshScope` 绑定 `app.rocketmq.consumer-thread-pool.*`
2. `RocketMqConsumerThreadPoolRefresher`
   监听启动和 `EnvironmentChangeEvent`

刷新流程：

1. Nacos 中 `*-mq-dev.yml` 变化
2. Spring Cloud Refresh 更新配置
3. `RocketMqConsumerThreadPoolRefresher` 遍历 `DefaultRocketMQListenerContainer`
4. 按容器的 `consumerGroup` 找到对应配置
5. 更新 `DefaultMQPushConsumer` 的 `consumeThreadMin` / `consumeThreadMax`
6. 反射拿到 RocketMQ 内部 `consumeExecutor`，同步调整运行中的核心线程数和最大线程数

## 为什么不用注解直接热更

`@RocketMQMessageListener` 的 `consumeThreadNumber` / `consumeThreadMax` 是注解属性，类型是 `int`，不是可刷新的字符串占位配置入口。

所以如果要做运行时动态更新，必须走容器或 consumer 级别的程序化调整，而不是单纯改注解。

## 生效边界

- 当前支持更新的字段只有：
  - `consumeThreadNumber`
  - `consumeThreadMax`
- 配置以 `consumerGroup` 为粒度，不是每个 Topic 单独一套。
- 如果多个监听器错误地复用了同一个 `consumerGroup`，它们会共用同一组线程池配置；这本身也符合 RocketMQ 对 consumer group 的语义。
- 这一实现依赖 RocketMQ Spring 容器和 RocketMQ Client 当前内部结构；如果后续升级框架版本，需要回归验证 `consumeExecutor` 反射路径是否仍然成立。

## 事实来源

- `order-service/order-service-app/src/main/java/com/yue/order/config/RocketMqConsumerThreadPoolProperties.java`
- `order-service/order-service-app/src/main/java/com/yue/order/config/RocketMqConsumerThreadPoolRefresher.java`
- `group-buy-service/group-buy-service-app/src/main/java/com/yue/groupbuy/config/RocketMqConsumerThreadPoolProperties.java`
- `group-buy-service/group-buy-service-app/src/main/java/com/yue/groupbuy/config/RocketMqConsumerThreadPoolRefresher.java`
- `seckill-service/seckill-service-app/src/main/java/com/yue/seckill/config/RocketMqConsumerThreadPoolProperties.java`
- `seckill-service/seckill-service-app/src/main/java/com/yue/seckill/config/RocketMqConsumerThreadPoolRefresher.java`
- `pay/pay-app/src/main/java/cn/bugstack/config/RocketMqConsumerThreadPoolProperties.java`
- `pay/pay-app/src/main/java/cn/bugstack/config/RocketMqConsumerThreadPoolRefresher.java`
- `~/.m2/repository/org/apache/rocketmq/rocketmq-spring-boot/2.3.1/rocketmq-spring-boot-2.3.1-sources.jar`
- `~/.m2/repository/org/apache/rocketmq/rocketmq-client/5.3.0/rocketmq-client-5.3.0-sources.jar`

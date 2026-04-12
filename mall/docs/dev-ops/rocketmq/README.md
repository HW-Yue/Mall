# RocketMQ 本地环境

## 启动

在 `docs/dev-ops` 目录：

```bash
docker compose -f docker-compose-rocketmq.yml up -d
```

首次启动会在 `docs/dev-ops/rocketmq/data` 下自动创建持久化目录。

- **NameServer**：`127.0.0.1:9876`（应用 `rocketmq.name-server` 指向此处）
- **Broker 管理端口**：`127.0.0.1:10909`
- **Broker Remoting**：`127.0.0.1:10911`（直连 `rmq-broker`）
- **Dashboard**：浏览器打开 `http://127.0.0.1:3003`（容器 `8080` 映射到宿主机 `3003`，与官方默认 CORS 白名单 `http://localhost:3003` 一致，避免 POST 被 403）

### 开发期推荐：使用宿主机固定 IP 注册 Broker

`broker.conf` 中 `brokerIP1` 需要配置成 **Dashboard 与业务应用都能访问到** 的地址。

Windows Docker Desktop 推荐：

```properties
brokerIP1 = host.docker.internal
```

如果是 Linux 宿主机，通常再改成宿主机局域网 IP（示例）：

```properties
brokerIP1 = 192.168.1.103
```

这样 Broker 向 NameServer 注册的就是可达宿主机地址，Java 客户端与 Dashboard 都可直接连 `10911`。

## 持久化说明

当前 Compose 已开启本地目录持久化（容器删除后数据仍保留）：

- `rocketmq/data/namesrv/logs` -> `/home/rocketmq/logs`
- `rocketmq/data/namesrv/store` -> `/home/rocketmq/store`
- `rocketmq/data/broker/logs` -> `/home/rocketmq/logs`
- `rocketmq/data/broker/store` -> `/home/rocketmq/store`（消息与消费进度核心数据）

清空本地 RocketMQ 数据（危险操作）：

```bash
docker compose -f docker-compose-rocketmq.yml down
rm -rf rocketmq/data
```

## Topic 与“队列”在 RocketMQ 里的含义

- **Topic**：消息主题，发送与订阅都针对 Topic。
- **队列（MessageQueue）**：Topic 在 Broker 上的分片，一般按 **读写队列数** 配置；消费者通过 **消费者组** 做负载均衡，不需要像 RabbitMQ 那样单独声明“绑定队列”。

开发环境 `broker.conf` 中已开启 `autoCreateTopicEnable = true`，**首次发送**到不存在的 Topic 时 Broker 会自动建 Topic（生产环境建议关闭，改为显式创建）。

## 创建 Topic（推荐掌握的方式）

进入 Broker 容器：

```bash
docker exec -it rmq-broker bash
```

在容器内（RocketMQ 5.x 镜像工作目录已含脚本）执行 **mqadmin**：

```bash
# 创建 Topic：8 读队列、8 写队列（可按业务量调整）
sh mqadmin updateTopic -n rmq-namesrv:9876 -t GROUP_BUY_TOPIC_TEAM_SUCCESS -c DefaultCluster -r 8 -w 8

# 再建一个
sh mqadmin updateTopic -n rmq-namesrv:9876 -t GROUP_BUY_TOPIC_TEAM_REFUND -c DefaultCluster -r 8 -w 8
```

常用查看命令：

```bash
sh mqadmin topicList -n rmq-namesrv:9876
sh mqadmin topicStatus -n rmq-namesrv:9876 -t GROUP_BUY_TOPIC_TEAM_SUCCESS
```

Topic 名称建议与 `application-dev.yml` 里 `app.rocketmq.topic.*` 保持一致。

## 消费者组（Consumer Group）

RocketMQ 中 **消费者组** 在应用侧配置，不在 Broker 上预先建“队列”：

- 同一 Group 内多实例互斥消费（集群模式），实现负载均衡。
- **不同 Group** 订阅同一 Topic 时，各自独立消费全量消息（广播语义需 `messageModel=BROADCASTING`）。

Spring 中在 `@RocketMQMessageListener` 的 `consumerGroup` 指定，例如引用配置：

`consumerGroup = "${app.rocketmq.consumer-group.team-refund}"`。

## 与 Spring 配置的对应关系

| 概念 | YAML / 代码 |
|------|-------------|
| NameServer 地址 | `rocketmq.name-server` |
| 生产者组 | `rocketmq.producer.group` |
| Topic 名 | `app.rocketmq.topic.*` |
| 消费者组 | `app.rocketmq.consumer-group.*` |

发送示例使用 `RocketMQTemplate.syncSend(topic, message)`；监听使用 `@RocketMQMessageListener(topic = "...", consumerGroup = "...")`。

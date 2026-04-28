package com.yue.order.infrastructure.event;

import com.yue.order.domain.order.service.IOrderShipTaskPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.messaging.support.MessageBuilder;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单事务消息生产者。
 * 通过 bizType 区分发货任务和退款请求，统一走同一个 RocketMQ 事务监听器。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class OrderShipTaskMqProducer implements IOrderShipTaskPublisher {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${app.rocketmq.topic.orderShipTask:order-ship-task}")
    private String orderShipTaskTopic;

    @Override
    public void publishOrderShipTask(String userId, String orderId, String outTradeNo) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("bizType", "order_ship_task");
        msg.put("userId", userId);
        msg.put("orderId", orderId);
        msg.put("outTradeNo", outTradeNo);
        try {
            Message<Map<String, Object>> message = MessageBuilder.withPayload(msg)
                    .setHeader("bizType", "order_ship_task")
                    .setHeader("userId", userId)
                    .setHeader("orderId", orderId)
                    .setHeader("outTradeNo", outTradeNo)
                    .build();
            TransactionSendResult sendResult = rocketMQTemplate.sendMessageInTransaction(
                    orderShipTaskTopic,
                    message,
                    msg);
            log.info("publishOrderShipTaskTx topic:{} orderId:{} outTradeNo:{} sendResult:{}",
                    orderShipTaskTopic, orderId, outTradeNo, sendResult != null ? sendResult.getLocalTransactionState() : null);
        } catch (Exception e) {
            log.error("publishOrderShipTask 失败 topic:{} orderId:{} outTradeNo:{}",
                    orderShipTaskTopic, orderId, outTradeNo, e);
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }
}

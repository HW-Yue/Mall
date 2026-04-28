package com.yue.order.infrastructure.event;

import com.yue.order.domain.order.service.IOrderRefundPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单退款请求事务消息生产者。
 * 通过事务消息先落 WAIT_REFUND，再提交 pay-refund-* 请求给 pay-service。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class PayRefundMqProducer implements IOrderRefundPublisher {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${app.rocketmq.topic.payRefundNormal:pay-refund-normal}")
    private String payRefundNormalTopic;

    @Value("${app.rocketmq.topic.payRefundGroupBuy:pay-refund-group-buy}")
    private String payRefundGroupBuyTopic;

    @Value("${app.rocketmq.topic.payRefundSeckill:pay-refund-seckill}")
    private String payRefundSeckillTopic;

    @Override
    public void publishPayRefund(String userId, String outTradeNo, String marketType) {
        String topic = resolveTopic(marketType);
        Map<String, Object> msg = new HashMap<>();
        msg.put("bizType", "pay_refund_request");
        msg.put("userId", userId);
        msg.put("outTradeNo", outTradeNo);
        msg.put("marketType", marketType);
        msg.put("outTradeTime", new Date());
        msg.put("source", "");
        msg.put("channel", "");

        try {
            Message<Map<String, Object>> message = MessageBuilder.withPayload(msg)
                    .setHeader("bizType", "pay_refund_request")
                    .setHeader("userId", userId)
                    .setHeader("outTradeNo", outTradeNo)
                    .setHeader("marketType", marketType)
                    .build();
            TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(topic, message, msg);
            log.info("publishPayRefundTx topic:{} outTradeNo:{} localTx:{}",
                    topic, outTradeNo, result != null ? result.getLocalTransactionState() : null);
        } catch (Exception e) {
            log.error("publishPayRefund 失败 topic:{} outTradeNo:{}", topic, outTradeNo, e);
            throw new RuntimeException(e);
        }
    }

    private String resolveTopic(String marketType) {
        if ("group_buy".equals(marketType)) return payRefundGroupBuyTopic;
        if ("seckill".equals(marketType)) return payRefundSeckillTopic;
        return payRefundNormalTopic;
    }
}

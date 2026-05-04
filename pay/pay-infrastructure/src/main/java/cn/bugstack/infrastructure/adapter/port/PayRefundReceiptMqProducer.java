package cn.bugstack.infrastructure.adapter.port;

import cn.bugstack.domain.order.adapter.port.IRefundReceiptPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 退款完成回执 MQ 生产者
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class PayRefundReceiptMqProducer implements IRefundReceiptPublisher {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.topic.pay_refund_normal_result:pay-refund-normal-result}")
    private String payRefundNormalResultTopic;

    @Value("${rocketmq.topic.pay_refund_group_buy_result:pay-refund-group-buy-result}")
    private String payRefundGroupBuyResultTopic;

    @Value("${rocketmq.topic.pay_refund_seckill_result:pay-refund-seckill-result}")
    private String payRefundSeckillResultTopic;

    @Override
    public void publishRefundReceipt(String userId, String orderId, String marketType) {
        String topic = resolveTopic(marketType);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("outTradeNo", orderId);
        payload.put("marketType", marketType);

        Message<Map<String, Object>> message = MessageBuilder.withPayload(payload)
                .setHeader("userId", userId)
                .setHeader("outTradeNo", orderId)
                .setHeader("marketType", marketType)
                .build();

        try {
            TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(topic, message, payload);
            log.info("publishRefundReceiptTx topic:{} orderId:{} localTx:{}",
                    topic, orderId, result != null ? result.getLocalTransactionState() : null);
        } catch (Exception e) {
            log.error("publishRefundReceipt 失败 topic:{} orderId:{} marketType:{}",
                    topic, orderId, marketType, e);
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    private String resolveTopic(String marketType) {
        if ("group_buy".equals(marketType)) {
            return payRefundGroupBuyResultTopic;
        }
        if ("seckill".equals(marketType)) {
            return payRefundSeckillResultTopic;
        }
        return payRefundNormalResultTopic;
    }
}

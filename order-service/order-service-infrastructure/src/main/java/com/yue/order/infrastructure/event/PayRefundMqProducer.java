package com.yue.order.infrastructure.event;

import com.alibaba.fastjson.JSON;
import com.yue.order.domain.order.service.IOrderRefundPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单退款事件 MQ 生产者
 * 发布 pay-refund-normal / pay-refund-group-buy / pay-refund-seckill
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
        msg.put("userId", userId);
        msg.put("outTradeNo", outTradeNo);
        msg.put("marketType", marketType);
        msg.put("outTradeTime", new Date());
        msg.put("source", "");
        msg.put("channel", "");

        String messageBody = JSON.toJSONString(msg);
        try {
            rocketMQTemplate.convertAndSend(topic, messageBody);
            log.info("publishPayRefund topic:{} outTradeNo:{}", topic, outTradeNo);
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

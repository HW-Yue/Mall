package com.yue.order.infrastructure.event;

import com.alibaba.fastjson.JSON;
import com.yue.order.domain.order.service.IOrderEventPublisher;
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
 * 订单支付成功事件 MQ 生产者
 * 发布 order-paid-normal / order-paid-group_buy / order-paid-seckill
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class OrderPaidMqProducer implements IOrderEventPublisher {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${app.rocketmq.topic.orderPaidNormal:order-paid-normal}")
    private String orderPaidNormalTopic;

    @Value("${app.rocketmq.topic.orderPaidGroupBuy:order-paid-group_buy}")
    private String orderPaidGroupBuyTopic;

    @Value("${app.rocketmq.topic.orderPaidSeckill:order-paid-seckill}")
    private String orderPaidSeckillTopic;

    @Value("${app.rocketmq.topic.orderCloseNormal:order-close-normal}")
    private String orderCloseNormalTopic;

    @Value("${app.rocketmq.topic.orderCloseGroupBuy:order-close-group-buy}")
    private String orderCloseGroupBuyTopic;

    @Value("${app.rocketmq.topic.orderCloseSeckill:order-close-seckill}")
    private String orderCloseSeckillTopic;

    @Override
    public void publishOrderPaid(String userId, String orderId, String outTradeNo, String marketType, Date outTradeTime) {
        String topic = resolveTopic(marketType);
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", userId);
        msg.put("orderId", orderId);
        msg.put("outTradeNo", outTradeNo);
        msg.put("marketType", marketType);
        msg.put("outTradeTime", outTradeTime != null ? outTradeTime.getTime() : null);

        String messageBody = JSON.toJSONString(msg);
        try {
            rocketMQTemplate.convertAndSend(topic, messageBody);
            log.info("publishOrderPaid topic:{} outTradeNo:{}", topic, outTradeNo);
        } catch (Exception e) {
            log.error("publishOrderPaid 失败 topic:{} outTradeNo:{}", topic, outTradeNo, e);
        }
    }

    @Override
    public void publishOrderClose(String userId, String orderId, String outTradeNo, String marketType) {
        String topic = resolveCloseTopic(marketType);
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", userId);
        msg.put("orderId", orderId);
        msg.put("outTradeNo", outTradeNo);
        msg.put("marketType", marketType);
        msg.put("outTradeTime", null);

        String messageBody = JSON.toJSONString(msg);
        try {
            rocketMQTemplate.convertAndSend(topic, messageBody);
            log.info("publishOrderClose topic:{} outTradeNo:{}", topic, outTradeNo);
        } catch (Exception e) {
            log.error("publishOrderClose 失败 topic:{} outTradeNo:{}", topic, outTradeNo, e);
        }
    }

    private String resolveTopic(String marketType) {
        if ("group_buy".equals(marketType)) return orderPaidGroupBuyTopic;
        if ("seckill".equals(marketType)) return orderPaidSeckillTopic;
        return orderPaidNormalTopic;
    }

    private String resolveCloseTopic(String marketType) {
        if ("group_buy".equals(marketType)) return orderCloseGroupBuyTopic;
        if ("seckill".equals(marketType)) return orderCloseSeckillTopic;
        return orderCloseNormalTopic;
    }
}

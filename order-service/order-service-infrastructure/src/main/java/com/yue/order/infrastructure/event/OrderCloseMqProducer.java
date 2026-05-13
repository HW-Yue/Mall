package com.yue.order.infrastructure.event;

import com.alibaba.fastjson.JSON;
import com.yue.order.domain.order.adapter.port.IOrderClosePublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单关单事件 MQ 生产者
 * 发布 order-close-normal / order-close-group-buy / order-close-seckill
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class OrderCloseMqProducer implements IOrderClosePublisher {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${app.rocketmq.topic.orderCloseNormal:order-close-normal}")
    private String orderCloseNormalTopic;

    @Value("${app.rocketmq.topic.orderCloseGroupBuy:order-close-group-buy}")
    private String orderCloseGroupBuyTopic;

    @Value("${app.rocketmq.topic.orderCloseSeckill:order-close-seckill}")
    private String orderCloseSeckillTopic;

    @Value("${app.rocketmq.topic.orderCloseGroupBuyMarket:order-close-group-buy-market}")
    private String orderCloseGroupBuyMarketTopic;

    @Value("${app.rocketmq.topic.orderCloseSeckillMarket:order-close-seckill-market}")
    private String orderCloseSeckillMarketTopic;

    @Override
    public void publishOrderClose(String userId, String orderId, String outTradeNo, String marketType) {
        String topic = resolveTopic(marketType);
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
            publishMarketOrderClose(userId, orderId, marketType);
        } catch (Exception e) {
            log.error("publishOrderClose 失败 topic:{} outTradeNo:{}", topic, outTradeNo, e);
        }
    }

    private void publishMarketOrderClose(String userId, String orderId, String marketType) {
        String marketTopic = resolveMarketTopic(marketType);
        if (marketTopic == null) {
            return;
        }

        Map<String, Object> marketMsg = new HashMap<>();
        marketMsg.put("userId", userId);
        marketMsg.put("orderId", orderId);
        marketMsg.put("marketType", marketType);
        rocketMQTemplate.convertAndSend(marketTopic, JSON.toJSONString(marketMsg));
        log.info("publishMarketOrderClose topic:{} orderId:{}", marketTopic, orderId);
    }

    private String resolveTopic(String marketType) {
        if ("group_buy".equals(marketType)) return orderCloseGroupBuyTopic;
        if ("seckill".equals(marketType)) return orderCloseSeckillTopic;
        return orderCloseNormalTopic;
    }

    private String resolveMarketTopic(String marketType) {
        if ("group_buy".equals(marketType)) return orderCloseGroupBuyMarketTopic;
        if ("seckill".equals(marketType)) return orderCloseSeckillMarketTopic;
        return null;
    }
}

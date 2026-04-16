package com.yue.groupbuy.infrastructure.event;

import com.alibaba.fastjson.JSON;
import com.yue.groupbuy.domain.trade.adapter.port.IGroupBuyRefundMqProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 拼团退款/关单 MQ 生产者（供 order-service 和 pay-service 消费）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class GroupBuyRefundMqProducer implements IGroupBuyRefundMqProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${app.rocketmq.topic.orderCloseGroupBuy:order-close-group-buy}")
    private String orderCloseGroupBuyTopic;

    @Value("${app.rocketmq.topic.payRefundGroupBuy:pay-refund-group-buy}")
    private String payRefundGroupBuyTopic;

    /**
     * 发送未支付订单关单消息
     */
    public void sendOrderCloseMessage(String outTradeNo, String userId) {
        Map<String, Object> dto = buildMessageBody(outTradeNo, userId);
        String messageBody = JSON.toJSONString(dto);
        log.info("发送拼团未支付关单消息 topic:{} outTradeNo:{}", orderCloseGroupBuyTopic, outTradeNo);
        try {
            rocketMQTemplate.convertAndSend(orderCloseGroupBuyTopic, messageBody);
        } catch (Exception e) {
            log.error("发送拼团未支付关单消息失败 topic:{} outTradeNo:{}", orderCloseGroupBuyTopic, outTradeNo, e);
        }
    }

    /**
     * 发送已支付订单退款消息
     */
    public void sendPayRefundMessage(String outTradeNo, String userId) {
        Map<String, Object> dto = buildMessageBody(outTradeNo, userId);
        String messageBody = JSON.toJSONString(dto);
        log.info("发送拼团已支付退款消息 topic:{} outTradeNo:{}", payRefundGroupBuyTopic, outTradeNo);
        try {
            rocketMQTemplate.convertAndSend(payRefundGroupBuyTopic, messageBody);
        } catch (Exception e) {
            log.error("发送拼团已支付退款消息失败 topic:{} outTradeNo:{}", payRefundGroupBuyTopic, outTradeNo, e);
        }
    }

    private Map<String, Object> buildMessageBody(String outTradeNo, String userId) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("userId", userId);
        dto.put("outTradeNo", outTradeNo);
        dto.put("marketType", "group_buy");
        dto.put("outTradeTime", new Date());
        dto.put("source", "");
        dto.put("channel", "");
        return dto;
    }
}

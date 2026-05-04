package cn.bugstack.infrastructure.adapter.port;

import cn.bugstack.domain.order.adapter.port.IOrderClosePublisher;
import cn.bugstack.infrastructure.gateway.dto.SettlementMQMessageDTO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 订单关单 MQ 生产者
 */
@Slf4j
@Service
public class OrderCloseMqProducer implements IOrderClosePublisher {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.topic.order_close_normal}")
    private String orderCloseNormalTopic;

    @Value("${rocketmq.topic.order_close_group_buy}")
    private String orderCloseGroupBuyTopic;

    @Value("${rocketmq.topic.order_close_seckill}")
    private String orderCloseSeckillTopic;

    @Value("${app.config.group-buy-market.source}")
    private String source;

    @Value("${app.config.group-buy-market.chanel}")
    private String channel;

    @Override
    public void sendOrderCloseMessage(String userId, String outTradeNo, String marketType) {
        String topic = resolveTopic(marketType);
        if (rocketMQTemplate == null) {
            log.warn("订单关单，RocketMQTemplate 未注入，跳过发送 topic:{} out_trade_no:{}",
                    topic, outTradeNo);
            return;
        }
        try {
            SettlementMQMessageDTO dto = new SettlementMQMessageDTO();
            dto.setSource(source);
            dto.setChannel(channel);
            dto.setUserId(userId);
            dto.setOutTradeNo(outTradeNo);
            dto.setMarketType(marketType);

            String messageBody = JSON.toJSONString(dto);
            log.info("订单关单，RocketMQ 准备发送 topic:{} marketType:{} out_trade_no:{} message:{}",
                    topic, marketType, outTradeNo, messageBody);
            rocketMQTemplate.convertAndSend(topic, messageBody);
            log.info("订单关单，RocketMQ 发送成功 topic:{} out_trade_no:{}",
                    topic, outTradeNo);
        } catch (Exception e) {
            log.error("订单关单，RocketMQ 发送失败 topic:{} out_trade_no:{}",
                    topic, outTradeNo, e);
        }
    }

    private String resolveTopic(String marketType) {
        if (marketType == null) {
            return orderCloseNormalTopic;
        }
        switch (marketType) {
            case "group_buy":
                return orderCloseGroupBuyTopic;
            case "seckill":
                return orderCloseSeckillTopic;
            default:
                return orderCloseNormalTopic;
        }
    }
}

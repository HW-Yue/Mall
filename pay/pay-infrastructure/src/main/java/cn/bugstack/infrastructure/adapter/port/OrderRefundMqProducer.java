package cn.bugstack.infrastructure.adapter.port;

import cn.bugstack.domain.order.adapter.port.IOrderRefundPublisher;
import cn.bugstack.infrastructure.gateway.dto.SettlementMQMessageDTO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 订单退款 MQ 生产者
 */
@Slf4j
@Service
public class OrderRefundMqProducer implements IOrderRefundPublisher {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.topic.pay_refund_normal}")
    private String payRefundNormalTopic;

    @Value("${rocketmq.topic.pay_refund_group_buy}")
    private String payRefundGroupBuyTopic;

    @Value("${rocketmq.topic.pay_refund_seckill}")
    private String payRefundSeckillTopic;

    @Value("${app.config.group-buy-market.source}")
    private String source;

    @Value("${app.config.group-buy-market.chanel}")
    private String channel;

    @Override
    public void sendPayRefundMessage(String userId, String outTradeNo, String marketType) {
        String topic = resolveTopic(marketType);
        if (rocketMQTemplate == null) {
            log.warn("关单退款，RocketMQTemplate 未注入，跳过发送 topic:{} out_trade_no:{}",
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
            log.info("关单退款，RocketMQ 准备发送 topic:{} marketType:{} out_trade_no:{} message:{}",
                    topic, marketType, outTradeNo, messageBody);
            rocketMQTemplate.convertAndSend(topic, messageBody);
            log.info("关单退款，RocketMQ 发送成功 topic:{} out_trade_no:{}",
                    topic, outTradeNo);
        } catch (Exception e) {
            log.error("关单退款，RocketMQ 发送失败 topic:{} out_trade_no:{}",
                    topic, outTradeNo, e);
        }
    }

    private String resolveTopic(String marketType) {
        if (marketType == null) {
            return payRefundNormalTopic;
        }
        switch (marketType) {
            case "group_buy":
                return payRefundGroupBuyTopic;
            case "seckill":
                return payRefundSeckillTopic;
            default:
                return payRefundNormalTopic;
        }
    }
}

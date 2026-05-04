package cn.bugstack.infrastructure.adapter.port;

import cn.bugstack.domain.order.adapter.port.IPaySuccessPublisher;
import cn.bugstack.infrastructure.gateway.dto.SettlementMQMessageDTO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 支付成功结算 MQ 生产者
 */
@Slf4j
@Service
public class PaySuccessMqProducer implements IPaySuccessPublisher {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    /** 普通商品支付成功 topic */
    @Value("${rocketmq.topic.pay_success_normal}")
    private String paySuccessNormalTopic;

    /** 拼团支付成功 topic */
    @Value("${rocketmq.topic.pay_success_group_buy}")
    private String paySuccessGroupBuyTopic;

    /** 秒杀支付成功 topic */
    @Value("${rocketmq.topic.pay_success_seckill}")
    private String paySuccessSeckillTopic;

    @Value("${app.config.group-buy-market.source}")
    private String source;

    @Value("${app.config.group-buy-market.chanel}")
    private String channel;

    @Override
    public void sendSettlementMessage(String userId, String outTradeNo, Date outTradeTime, String marketType) {
        String topic = resolveTopic(marketType);
        if (rocketMQTemplate == null) {
            log.warn("支付回调，RocketMQTemplate 未注入，跳过发送 topic:{} out_trade_no:{}",
                    topic, outTradeNo);
            return;
        }
        try {
            SettlementMQMessageDTO dto = new SettlementMQMessageDTO();
            dto.setSource(source);
            dto.setChannel(channel);
            dto.setUserId(userId);
            dto.setOutTradeNo(outTradeNo);
            dto.setOutTradeTime(outTradeTime);
            dto.setMarketType(marketType);

            String messageBody = JSON.toJSONString(dto);
            log.info("支付回调，RocketMQ 准备发送 topic:{} marketType:{} out_trade_no:{} message:{}",
                    topic, marketType, outTradeNo, messageBody);
            rocketMQTemplate.convertAndSend(topic, messageBody);
            log.info("支付回调，RocketMQ 发送成功 topic:{} out_trade_no:{}",
                    topic, outTradeNo);
        } catch (Exception e) {
            log.error("支付回调，RocketMQ 发送失败 topic:{} out_trade_no:{}",
                    topic, outTradeNo, e);
        }
    }

    private String resolveTopic(String marketType) {
        if (marketType == null) {
            log.warn("支付回调，marketType 为 null，默认路由至普通商品 topic");
            return paySuccessNormalTopic;
        }
        switch (marketType) {
            case "group_buy":
                return paySuccessGroupBuyTopic;
            case "seckill":
                return paySuccessSeckillTopic;
            default:
                return paySuccessNormalTopic;
        }
    }
}

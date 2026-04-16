package cn.bugstack.infrastructure.adapter.port;

import cn.bugstack.infrastructure.gateway.dto.SettlementMQMessageDTO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 支付宝支付成功后，向 RocketMQ 发送结算相关消息（替代原 HTTP 通知下游）。
 */
@Slf4j
@Service
public class PaySuccessRocketMqPort {

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

    /** 普通订单关单 topic */
    @Value("${rocketmq.topic.order_close_normal}")
    private String orderCloseNormalTopic;

    /** 拼团订单关单 topic */
    @Value("${rocketmq.topic.order_close_group_buy}")
    private String orderCloseGroupBuyTopic;

    /** 秒杀订单关单 topic */
    @Value("${rocketmq.topic.order_close_seckill}")
    private String orderCloseSeckillTopic;

    /** 普通订单退款 topic */
    @Value("${rocketmq.topic.pay_refund_normal}")
    private String payRefundNormalTopic;

    /** 拼团订单退款 topic */
    @Value("${rocketmq.topic.pay_refund_group_buy}")
    private String payRefundGroupBuyTopic;

    /** 秒杀订单退款 topic */
    @Value("${rocketmq.topic.pay_refund_seckill}")
    private String payRefundSeckillTopic;

    @Value("${app.config.group-buy-market.source}")
    private String source;

    @Value("${app.config.group-buy-market.chanel}")
    private String channel;

    /**
     * @param userId       用户 ID，可为 null
     * @param outTradeNo   商户订单号
     * @param outTradeTime 支付完成时间
     * @param marketType   营销类型：normal / group_buy / seckill
     */
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
            case "group_buy": return paySuccessGroupBuyTopic;
            case "seckill":   return paySuccessSeckillTopic;
            default:           return paySuccessNormalTopic;
        }
    }

    /**
     * 发送订单关单通知消息
     *
     * @param userId     用户 ID
     * @param outTradeNo 商户订单号
     * @param marketType 营销类型
     */
    public void sendOrderCloseMessage(String userId, String outTradeNo, String marketType) {
        String topic = resolveOrderCloseTopic(marketType);
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

    /**
     * 发送关单后退款通知消息
     *
     * @param userId     用户 ID
     * @param outTradeNo 商户订单号
     * @param marketType 营销类型
     */
    public void sendPayRefundMessage(String userId, String outTradeNo, String marketType) {
        String topic = resolvePayRefundTopic(marketType);
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

    private String resolveOrderCloseTopic(String marketType) {
        if (marketType == null) {
            return orderCloseNormalTopic;
        }
        switch (marketType) {
            case "group_buy": return orderCloseGroupBuyTopic;
            case "seckill":   return orderCloseSeckillTopic;
            default:           return orderCloseNormalTopic;
        }
    }

    private String resolvePayRefundTopic(String marketType) {
        if (marketType == null) {
            return payRefundNormalTopic;
        }
        switch (marketType) {
            case "group_buy": return payRefundGroupBuyTopic;
            case "seckill":   return payRefundSeckillTopic;
            default:           return payRefundNormalTopic;
        }
    }
}

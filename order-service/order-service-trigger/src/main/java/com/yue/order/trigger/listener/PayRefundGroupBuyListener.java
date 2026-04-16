package com.yue.order.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.order.domain.order.service.IOrderDomainService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 拼团订单关单后退款 MQ 消费者（pay-refund-group-buy）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.payRefundGroupBuy:pay-refund-group-buy}",
        consumerGroup = "${app.rocketmq.consumerGroup.payRefundGroupBuy:CG_PAY_REFUND_GROUP_BUY}"
)
public class PayRefundGroupBuyListener implements RocketMQListener<String> {

    @Resource
    private IOrderDomainService orderDomainService;

    @Override
    public void onMessage(String message) {
        log.info("pay-refund-group-buy 收到消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String outTradeNo = dto.getString("outTradeNo");
            if (StringUtils.isBlank(outTradeNo)) {
                log.error("消息缺少 outTradeNo: {}", message);
                return;
            }
            orderDomainService.handlePayRefund(outTradeNo);
        } catch (Exception e) {
            log.error("pay-refund-group-buy 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}

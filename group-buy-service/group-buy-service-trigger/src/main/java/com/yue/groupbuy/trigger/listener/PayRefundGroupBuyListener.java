package com.yue.groupbuy.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.groupbuy.domain.trade.service.IGroupBuyDomainService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 消费支付侧退款完成消息，更新拼团本地订单为已退款
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.orderRefundGroupBuy:order-refund-group-buy}",
        consumerGroup = "${app.rocketmq.consumerGroup.payRefundGroupBuy:CG_GROUP_BUY_PAY_REFUND}"
)
public class PayRefundGroupBuyListener implements RocketMQListener<String> {

    @Resource
    private IGroupBuyDomainService groupBuyDomainService;

    @Override
    public void onMessage(String message) {
        log.info("pay-refund-group-buy 收到消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String orderId = dto.getString("orderId");
            if (StringUtils.isBlank(orderId)) {
                log.error("消息缺少 orderId: {}", message);
                return;
            }
            groupBuyDomainService.handlePayRefund(orderId);
        } catch (Exception e) {
            log.error("pay-refund-group-buy 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}

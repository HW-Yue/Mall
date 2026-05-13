package com.yue.order.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.order.domain.order.adapter.port.IOrderRefundPublisher;
import com.yue.order.domain.order.model.entity.OrderEntity;
import com.yue.order.domain.order.service.IOrderDomainService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 拼团订单退款完成回执 MQ 消费者（pay-refund-group-buy-result）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.payRefundGroupBuyResult:pay-refund-group-buy-result}",
        consumerGroup = "${app.rocketmq.consumerGroup.payRefundGroupBuyResult:CG_PAY_REFUND_GROUP_BUY_RESULT}"
)
public class PayRefundGroupBuyListener implements RocketMQListener<String> {

    @Resource
    private IOrderDomainService orderDomainService;
    @Resource
    private IOrderRefundPublisher orderRefundPublisher;

    @Override
    public void onMessage(String message) {
        log.info("pay-refund-group-buy 收到消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String userId = dto.getString("userId");
            String outTradeNo = dto.getString("outTradeNo");
            if (StringUtils.isAnyBlank(userId, outTradeNo)) {
                log.error("消息缺少 userId/outTradeNo: {}", message);
                return;
            }
            orderDomainService.handlePayRefund(outTradeNo);
            OrderEntity order = orderDomainService.queryByUserIdAndOutTradeNo(userId, outTradeNo);
            if (order != null) {
                orderRefundPublisher.publishMarketRefund(order.getUserId(), order.getOrderId(), order.getMarketType().getCode());
            }
        } catch (Exception e) {
            log.error("pay-refund-group-buy 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}

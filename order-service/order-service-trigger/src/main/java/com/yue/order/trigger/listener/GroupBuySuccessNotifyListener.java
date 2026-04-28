package com.yue.order.trigger.listener;

import com.yue.order.domain.order.service.IOrderDomainService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 拼团成功通知 MQ 消费者（group-buy-success-notify）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.groupBuySuccessNotify:group-buy-success-notify}",
        consumerGroup = "${app.rocketmq.consumerGroup.groupBuySuccessNotify:CG_GROUP_BUY_SUCCESS_NOTIFY}"
)
public class GroupBuySuccessNotifyListener implements RocketMQListener<String> {

    @Resource
    private IOrderDomainService orderDomainService;

    @Override
    public void onMessage(String message) {
        log.info("group-buy-success-notify 收到消息: {}", message);
        try {
            orderDomainService.handleGroupBuySuccess(message);
        } catch (Exception e) {
            log.error("group-buy-success-notify 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}

package com.yue.groupbuy.infrastructure.event;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 拼团超时退款延迟消息生产者（RocketMQ 5.2 Timer Message）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class GroupBuyTimeoutRefundProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${app.rocketmq.topic.groupBuyTimeoutRefund:group-buy-timeout-refund}")
    private String groupBuyTimeoutRefundTopic;

    /**
     * 发送拼团超时退款定时消息
     *
     * @param teamId          拼团队伍ID
     * @param deliverTimeMs   消息投递时间戳（毫秒），一般设为 validEndTime.getTime()
     */
    public void sendTimeoutRefundMessage(String teamId, long deliverTimeMs) {
        Map<String, Object> body = new HashMap<>();
        body.put("teamId", teamId);
        body.put("deliverTimeMs", deliverTimeMs);

        String payload = JSON.toJSONString(body);
        log.info("发送拼团超时退款定时消息 topic:{} teamId:{} deliverTimeMs:{}", groupBuyTimeoutRefundTopic, teamId, deliverTimeMs);

        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader("TIMER_DELIVER_MS", deliverTimeMs)
                .build();

        rocketMQTemplate.syncSend(groupBuyTimeoutRefundTopic, message);
    }
}

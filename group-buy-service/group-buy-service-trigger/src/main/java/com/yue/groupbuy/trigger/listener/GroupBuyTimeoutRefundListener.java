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
 * 消费拼团超时退款定时消息，处理未成团队伍的退款/关单
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.groupBuyTimeoutRefund:group-buy-timeout-refund}",
        consumerGroup = "${app.rocketmq.consumerGroup.groupBuyTimeoutRefund:CG_GROUP_BUY_TIMEOUT_REFUND}"
)
public class GroupBuyTimeoutRefundListener implements RocketMQListener<String> {

    @Resource
    private IGroupBuyDomainService groupBuyDomainService;

    @Override
    public void onMessage(String message) {
        log.info("收到MQ：拼团超时退款消息: {}", message);
        JSONObject json;
        try {
            json = JSON.parseObject(message);
        } catch (Exception e) {
            log.error("消息 JSON 解析失败，确认消费: {}", message, e);
            return;
        }

        String teamId = json.getString("teamId");
        if (StringUtils.isBlank(teamId)) {
            log.error("消息缺少 teamId: {}", message);
            return;
        }

        try {
            groupBuyDomainService.handleTimeoutRefund(teamId);
        } catch (Exception e) {
            log.error("拼团超时退款处理失败 teamId:{}", teamId, e);
            throw new RuntimeException(e);
        }
    }
}

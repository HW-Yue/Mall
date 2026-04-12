package com.yue.groupbuy.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.groupbuy.domain.trade.model.entity.SettlementCommand;
import com.yue.groupbuy.domain.trade.service.IGroupBuyDomainService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 消费 order-service 发布的 order-paid-group_buy 消息，执行拼团结算
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.orderPaidGroupBuy: order-paid-group_buy}",
        consumerGroup = "${app.rocketmq.consumerGroup.orderPaidGroupBuy: CG_GROUP_BUY_ORDER_PAID}"
)
public class OrderPaidGroupBuyListener implements RocketMQListener<String> {

    @Resource
    private IGroupBuyDomainService groupBuyDomainService;

    @Override
    public void onMessage(String message) {
        log.info("收到MQ： 拼团支付成功消息: {}", message);
        JSONObject json;
        try {
            json = JSON.parseObject(message);
        } catch (Exception e) {
            log.error("消息 JSON 解析失败，确认消费: {}", message, e);
            return;
        }

        String userId = json.getString("userId");
        String outTradeNo = json.getString("outTradeNo");
        Long outTradeTimeMs = json.getLong("outTradeTime");

        if (StringUtils.isAnyBlank(userId, outTradeNo)) {
            log.error("消息缺少必填字段: {}", message);
            return;
        }

        Date outTradeTime = outTradeTimeMs != null ? new Date(outTradeTimeMs) : new Date();

        try {
            groupBuyDomainService.settlementGroupBuyOrder(SettlementCommand.builder()
                    .userId(userId)
                    .outTradeNo(outTradeNo)
                    .outTradeTime(outTradeTime)
                    .build());
        } catch (Exception e) {
            log.error("拼团结算失败 userId:{} outTradeNo:{}", userId, outTradeNo, e);
            throw new RuntimeException(e);
        }
    }
}

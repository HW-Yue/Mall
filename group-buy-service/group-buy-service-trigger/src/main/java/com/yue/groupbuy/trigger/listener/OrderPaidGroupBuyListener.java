package com.yue.groupbuy.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.groupbuy.domain.trade.adapter.repository.ITradeRepository;
import com.yue.groupbuy.domain.trade.model.entity.GroupBuyTeamEntity;
import com.yue.groupbuy.domain.trade.model.entity.MarketPayOrderEntity;
import com.yue.groupbuy.domain.trade.model.entity.SettlementCommand;
import com.yue.groupbuy.domain.trade.service.IGroupBuyDomainService;
import com.yue.groupbuy.infrastructure.event.GroupBuyTimeoutRefundProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
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

    @Resource
    private ITradeRepository tradeRepository;

    @Resource
    private GroupBuyTimeoutRefundProducer groupBuyTimeoutRefundProducer;

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

        // 结算成功后，发送拼团超时退款定时消息
        try {
            MarketPayOrderEntity marketPayOrder = tradeRepository.queryMarketPayOrderEntityByOutTradeNo(userId, outTradeNo);
            if (marketPayOrder == null || StringUtils.isBlank(marketPayOrder.getTeamId())) {
                log.warn("拼团结算后未找到 teamId，跳过发送定时消息 userId:{} outTradeNo:{}", userId, outTradeNo);
                return;
            }
            String teamId = marketPayOrder.getTeamId();
            GroupBuyTeamEntity team = tradeRepository.queryGroupBuyTeamByTeamId(teamId);
            if (team == null || team.getValidEndTime() == null) {
                log.warn("拼团结算后未找到团队信息，跳过发送定时消息 teamId:{}", teamId);
                return;
            }
            long deliverTimeMs = team.getValidEndTime().getTime();
            // 若 validEndTime 已过期，则立即投递（RocketMQ 允许投递过去的时间，会立即消费）
            groupBuyTimeoutRefundProducer.sendTimeoutRefundMessage(teamId, deliverTimeMs);
        } catch (Exception e) {
            log.error("发送拼团超时退款定时消息失败 userId:{} outTradeNo:{}", userId, outTradeNo, e);
        }
    }
}

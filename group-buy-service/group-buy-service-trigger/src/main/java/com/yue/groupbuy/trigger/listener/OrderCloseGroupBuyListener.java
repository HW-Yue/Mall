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
 * 消费 pay 侧或本服务超时自发的未支付关单消息，关本地订单并回退团占用库存（lock_count）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.orderCloseGroupBuyMarket:order-close-group-buy-market}",
        consumerGroup = "${app.rocketmq.consumerGroup.orderCloseGroupBuy:CG_GROUP_BUY_ORDER_CLOSE}"
)
public class OrderCloseGroupBuyListener implements RocketMQListener<String> {

    private static final String EXPECTED_MARKET_TYPE = "group_buy";

    @Resource
    private IGroupBuyDomainService groupBuyDomainService;

    @Override
    public void onMessage(String message) {
        log.info("order-close-group-buy 收到消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String marketType = dto.getString("marketType");
            if (marketType != null && !EXPECTED_MARKET_TYPE.equals(marketType)) {
                log.warn("order-close-group-buy 消息 marketType 不匹配，跳过 marketType:{} message:{}", marketType, message);
                return;
            }
            String orderId = dto.getString("orderId");
            if (StringUtils.isBlank(orderId)) {
                log.error("消息缺少 orderId: {}", message);
                return;
            }
            groupBuyDomainService.handleOrderClose(orderId);
        } catch (Exception e) {
            log.error("order-close-group-buy 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}

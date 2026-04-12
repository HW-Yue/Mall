package com.yue.groupbuy.infrastructure.event;

import com.alibaba.fastjson.JSON;
import com.yue.groupbuy.domain.trade.adapter.port.IGroupBuyEventPublisher;
import com.yue.groupbuy.infrastructure.event.dto.TeamSuccessNotifyMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class GroupBuyEventPublisher implements IGroupBuyEventPublisher {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${app.rocketmq.topic.groupBuySuccessNotify:group-buy-success-notify}")
    private String groupBuySuccessNotifyTopic;

    @Override
    public void publishGroupBuySuccess(String teamId, List<Map<String, Object>> completedOrders) {
        List<TeamSuccessNotifyMessage.OrderItem> items = new ArrayList<>();
        for (Map<String, Object> order : completedOrders) {
            Object payPriceObj = order.get("payPrice");
            BigDecimal payPrice = payPriceObj instanceof BigDecimal
                    ? (BigDecimal) payPriceObj
                    : (payPriceObj != null ? new BigDecimal(payPriceObj.toString()) : null);

            items.add(TeamSuccessNotifyMessage.OrderItem.builder()
                    .orderId((String) order.get("orderId"))
                    .userId((String) order.get("userId"))
                    .goodsType((String) order.get("goodsType"))
                    .resKey((String) order.get("resKey"))
                    .resValue((String) order.get("resValue"))
                    .amount(payPrice)
                    .build());
        }
        TeamSuccessNotifyMessage message = TeamSuccessNotifyMessage.builder()
                .teamId(teamId)
                .orders(items)
                .build();

        String body = JSON.toJSONString(message);
        log.info("发送拼团成功通知 topic:{} teamId:{}", groupBuySuccessNotifyTopic, teamId);
        rocketMQTemplate.convertAndSend(groupBuySuccessNotifyTopic, body);
    }

    public void publishRawMessage(String topic, String message) {
        log.info("发送原始消息 topic:{} message:{}", topic, message);
        rocketMQTemplate.convertAndSend(topic, message);
    }
}

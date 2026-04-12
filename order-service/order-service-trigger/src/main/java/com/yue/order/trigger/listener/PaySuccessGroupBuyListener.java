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

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 拼团支付成功 MQ 消费者（pay-success-group-buy）
 * 接管 mall 原 PaySuccessSettlementRocketMqListener 的状态更新职责
 * 结算逻辑由 group-buy-service 消费 order-paid-group_buy 完成
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.paySuccessGroupBuy:pay-success-group-buy}",
        consumerGroup = "${app.rocketmq.consumerGroup.paySuccessGroupBuy:CG_PAY_SUCCESS_GROUP_BUY}"
)
public class PaySuccessGroupBuyListener implements RocketMQListener<String> {

    @Resource
    private IOrderDomainService orderDomainService;

    @Override
    public void onMessage(String message) {
        log.info("pay-success-group-buy 收到消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String outTradeNo = dto.getString("outTradeNo");
            String outTradeTimeStr = dto.getString("outTradeTime");
            Date outTradeTime;
            try {
                outTradeTime = outTradeTimeStr != null
                        ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(outTradeTimeStr)
                        : new Date();
            } catch (Exception e) {
                outTradeTime = new Date();
            }

            if (StringUtils.isBlank(outTradeNo)) {
                log.error("消息缺少 outTradeNo: {}", message);
                return;
            }
            orderDomainService.handlePaySuccess(outTradeNo, "group_buy", outTradeTime);
        } catch (Exception e) {
            log.error("pay-success-group-buy 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}

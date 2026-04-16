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

import jakarta.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 普通商品支付成功 MQ 消费者（pay-success-normal）
 * 接管 mall 原 PaySuccessNormalListener 职责
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.paySuccessNormal:pay-success-normal}",
        consumerGroup = "${app.rocketmq.consumerGroup.paySuccessNormal:CG_PAY_SUCCESS_NORMAL}"
)
public class PaySuccessListener implements RocketMQListener<String> {

    @Resource
    private IOrderDomainService orderDomainService;

    @Override
    public void onMessage(String message) {
        log.info("pay-success-normal 收到消息: {}", message);
        handle(message, "normal");
    }

    void handle(String message, String defaultMarketType) {
        try {
            JSONObject dto = JSON.parseObject(message);
            String outTradeNo = dto.getString("outTradeNo");
            String marketType = StringUtils.defaultIfBlank(dto.getString("marketType"), defaultMarketType);
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
            orderDomainService.handlePaySuccess(outTradeNo, marketType, outTradeTime);
        } catch (Exception e) {
            log.error("处理支付成功消息失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}

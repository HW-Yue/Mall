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
 * 秒杀支付成功 MQ 消费者（pay-success-seckill）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.paySuccessSeckill:pay-success-seckill}",
        consumerGroup = "${app.rocketmq.consumerGroup.paySuccessSeckill:CG_PAY_SUCCESS_SECKILL}"
)
public class PaySuccessSeckillListener implements RocketMQListener<String> {

    @Resource
    private IOrderDomainService orderDomainService;

    @Override
    public void onMessage(String message) {
        log.info("pay-success-seckill 收到消息: {}", message);
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
            orderDomainService.handlePaySuccess(outTradeNo, "seckill", outTradeTime);
        } catch (Exception e) {
            log.error("pay-success-seckill 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}

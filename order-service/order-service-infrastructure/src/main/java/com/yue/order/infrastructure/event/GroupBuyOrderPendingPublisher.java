package com.yue.order.infrastructure.event;

import com.yue.order.domain.order.adapter.port.IGroupBuyOrderPendingPublisher;
import com.yue.order.types.enums.ResponseCode;
import com.yue.order.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class GroupBuyOrderPendingPublisher implements IGroupBuyOrderPendingPublisher {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${app.rocketmq.topic.groupBuyOrderCreate:group-buy-order-create}")
    private String topic;

    @Override
    public void publishInsertSync(String messageBody) {
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(topic, messageBody, 3000L);
            if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
                log.error("group-buy-order-create 同步发送失败 result:{}", sendResult);
                throw new AppException(ResponseCode.UN_ERROR.getCode(), "拼团订单入队失败，请稍后重试");
            }
            log.info("group-buy-order-create 已投递 msgId:{}", sendResult.getMsgId());
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("group-buy-order-create 发送异常", e);
            throw new AppException(ResponseCode.UN_ERROR.getCode(), "拼团订单入队失败: " + e.getMessage());
        }
    }
}

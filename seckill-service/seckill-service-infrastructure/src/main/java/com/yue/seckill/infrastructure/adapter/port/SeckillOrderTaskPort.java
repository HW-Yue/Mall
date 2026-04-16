package com.yue.seckill.infrastructure.adapter.port;

import com.alibaba.fastjson.JSON;
import com.yue.seckill.domain.trade.adapter.port.ISeckillOrderTaskPort;
import com.yue.seckill.types.model.SeckillOrderTaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 秒杀下单消息发送实现
 */
@Slf4j
@Service
public class SeckillOrderTaskPort implements ISeckillOrderTaskPort {

    @Value("${app.rocketmq.topic.seckillOrderCreate:seckill-order-create}")
    private String topic;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void sendCreateOrderTask(SeckillOrderTaskMessage message) {
        rocketMQTemplate.convertAndSend(topic, JSON.toJSONString(message));
        log.info("发送秒杀下单任务消息 topic:{} seckillToken:{}", topic, message.getSeckillToken());
    }

}

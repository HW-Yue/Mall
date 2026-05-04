package com.yue.seckill.infrastructure.adapter.port;

import com.alibaba.fastjson.JSON;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockDeductPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 秒杀库存扣减 MQ 发送实现
 * 通过异步消息通知 MySQL 更新 sc_sku_activity.stock_count，与 Redis 削峰解耦
 */
@Slf4j
@Service
public class SeckillStockDeductPort implements ISeckillStockDeductPort {

    @Value("${app.rocketmq.topic.seckillStockDeduct:seckill-stock-deduct}")
    private String topic;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void sendDeductStockTask(Long activityId, String productId) {
        sendStockTask(activityId, productId, "deduct");
    }

    @Override
    public void sendRecoverStockTask(Long activityId, String productId) {
        sendStockTask(activityId, productId, "recover");
    }

    private void sendStockTask(Long activityId, String productId, String op) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("activityId", activityId);
        msg.put("productId", productId);
        msg.put("op", op);
        rocketMQTemplate.convertAndSend(topic, JSON.toJSONString(msg));
        log.info("[秒杀库存] 发送 MySQL 库存任务 topic:{} op:{} activityId:{} productId:{}", topic, op, activityId, productId);
    }

}

package com.yue.seckill.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 秒杀库存扣减任务消费者（seckill-stock-deduct）
 *
 * <p>从 MQ 异步接收库存扣减指令，将 sc_sku_activity.stock_count--，
 * 与 Redis 削峰阶段的原子扣减解耦，避免支付高峰直接打穿 MySQL。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.seckillStockDeduct:seckill-stock-deduct}",
        consumerGroup = "${app.rocketmq.consumerGroup.seckillStockDeduct:CG_SECKILL_STOCK_DEDUCT}"
)
public class SeckillStockDeductListener implements RocketMQListener<String> {

    @Resource
    private ISeckillActivityRepository seckillActivityRepository;

    @Override
    public void onMessage(String message) {
        log.info("seckill-stock-deduct 收到消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            Long activityId = dto.getLong("activityId");
            String productId = dto.getString("productId");
            String op = dto.getString("op");

            if (activityId == null) {
                log.error("[MySQL库存扣减] 消息缺少 activityId: {}", message);
                return;
            }

            if (op == null || "deduct".equals(op)) {
                boolean success = seckillActivityRepository.deductStock(activityId, productId);
                if (success) {
                    log.info("[MySQL库存扣减] 成功 activityId:{} productId:{}", activityId, productId);
                } else {
                    log.warn("[MySQL库存扣减] 无可扣减库存（已售罄或并发）activityId:{} productId:{}", activityId, productId);
                }
            } else if ("recover".equals(op)) {
                boolean success = seckillActivityRepository.recoverStock(activityId, productId);
                if (success) {
                    log.info("[MySQL库存恢复] 成功 activityId:{} productId:{}", activityId, productId);
                } else {
                    log.warn("[MySQL库存恢复] 未命中（活动已删除？）activityId:{} productId:{}", activityId, productId);
                }
            } else {
                log.warn("[MySQL库存] 未知 op:{} activityId:{} productId:{}", op, activityId, productId);
            }
        } catch (Exception e) {
            log.error("seckill-stock-deduct 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }

}

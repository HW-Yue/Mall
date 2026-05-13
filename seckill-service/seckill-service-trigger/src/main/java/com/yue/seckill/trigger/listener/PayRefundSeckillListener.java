package com.yue.seckill.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockDeductPort;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀退款消费者（pay-refund-seckill）
 *
 * <p>由 order-service 在退款完成后发出。本服务收到后：
 * 1. SETNX 幂等锁，避免重投复加；
 * 2. 反查 orderId → activityId/productId 上下文（OrderPaidSeckillListener 在真实扣减成功时写入）；
 *    上下文缺失视为活动早已结束，ack 返回不重试；
 * 3. Lua 同时 INCR available + real（仅当 key 仍存在）；
 * 4. 发 seckill-stock-deduct (op=recover) 异步把 sc_sku_activity.stock_count 加回。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.orderRefundSeckill:order-refund-seckill}",
        consumerGroup = "${app.rocketmq.consumerGroup.payRefundSeckill:CG_SECKILL_PAY_REFUND}"
)
public class PayRefundSeckillListener implements RocketMQListener<String> {

    private static final String EXPECTED_MARKET_TYPE = "seckill";
    private static final String REFUND_ORDER_META_KEY_PREFIX = "seckill:refund:order:";
    private static final String REFUNDED_KEY_PREFIX = "seckill:refunded:";
    private static final long DEDUP_TTL_HOURS = 24L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ISeckillStockPort seckillStockPort;
    @Resource
    private ISeckillStockDeductPort seckillStockDeductPort;

    @Override
    public void onMessage(String message) {
        log.info("pay-refund-seckill 收到消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String marketType = dto.getString("marketType");
            if (marketType != null && !EXPECTED_MARKET_TYPE.equals(marketType)) {
                log.warn("pay-refund-seckill marketType 不匹配，跳过 marketType:{} message:{}", marketType, message);
                return;
            }
            String orderId = dto.getString("orderId");
            if (StringUtils.isBlank(orderId)) {
                log.error("[秒杀退款] 消息缺少 orderId: {}", message);
                return;
            }

            String dedupKey = REFUNDED_KEY_PREFIX + orderId;
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                    dedupKey, String.valueOf(System.currentTimeMillis()), DEDUP_TTL_HOURS, TimeUnit.HOURS);
            if (acquired == null || !acquired) {
                log.info("[秒杀退款] 幂等命中，跳过 orderId:{}", orderId);
                return;
            }

            String ctx = stringRedisTemplate.opsForValue().get(REFUND_ORDER_META_KEY_PREFIX + orderId);
            if (StringUtils.isBlank(ctx)) {
                log.warn("[秒杀退款] 库存上下文已不存在，活动可能已结束，跳过 orderId:{}", orderId);
                return;
            }
            String[] parts = ctx.split(":");
            if (parts.length < 2 || StringUtils.isAnyBlank(parts[0], parts[1])) {
                log.error("[秒杀退款] 库存上下文格式异常 orderId:{} ctx:{}", orderId, ctx);
                return;
            }
            Long activityId;
            try {
                activityId = Long.parseLong(parts[0]);
            } catch (NumberFormatException e) {
                log.error("[秒杀退款] activityId 解析失败 orderId:{} ctx:{}", orderId, ctx, e);
                return;
            }
            String productId = parts[1];

            // Redis 双层库存恢复
            int restoreResult = seckillStockPort.restoreFullStock(activityId, productId);
            if (restoreResult == 0) {
                log.warn("[秒杀退款] Redis 库存 key 已过期，跳过 orderId:{} activityId:{} productId:{}",
                        orderId, activityId, productId);
                return;
            }
            log.info("[秒杀退款] Redis 库存恢复 result:{} orderId:{} activityId:{} productId:{}",
                    restoreResult, orderId, activityId, productId);

            // MySQL stock_count 异步回写
            seckillStockDeductPort.sendRecoverStockTask(activityId, productId);
        } catch (Exception e) {
            log.error("pay-refund-seckill 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}

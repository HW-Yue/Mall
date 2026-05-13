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
 * 秒杀支付成功消费者（order-paid-seckill）
 *
 * <p>流程：
 * 1. 通过 orderId 反查秒杀上下文
 * 2. Lua 原子扣减 Redis 真实库存（seckill:stock:real:...）
 * 3. 扣减成功后发 seckill-stock-deduct MQ，异步更新 MySQL sc_sku_activity.stock_count
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.orderPaidSeckill:order-paid-seckill}",
        consumerGroup = "${app.rocketmq.consumerGroup.orderPaidSeckill:CG_ORDER_PAID_SECKILL}"
)
public class OrderPaidSeckillListener implements RocketMQListener<String> {

    private static final String TOKEN_BY_ORDER_KEY_PREFIX = "seckill:token:by:order:";
    private static final String TOKEN_KEY_PREFIX = "seckill:token:";
    private static final String ORDER_META_KEY_PREFIX = "seckill:order:meta:";
    private static final String REFUND_ORDER_META_KEY_PREFIX = "seckill:refund:order:";
    private static final long REFUND_INDEX_TTL_HOURS = 24L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ISeckillStockPort seckillStockPort;
    @Resource
    private ISeckillStockDeductPort seckillStockDeductPort;

    @Override
    public void onMessage(String message) {
        log.info("order-paid-seckill 收到消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String orderId = dto.getString("orderId");
            if (StringUtils.isBlank(orderId)) {
                log.error("[秒杀支付成功] 消息缺少 orderId: {}", message);
                return;
            }

            StockContext stockContext = loadStockContext(orderId);

            // 3. Lua 原子扣减 Redis 真实库存
            int realResult = seckillStockPort.deductRealStockByLua(stockContext.getActivityId(), stockContext.getProductId());
            if (realResult == 1) {
                seckillStockDeductPort.sendDeductStockTask(stockContext.getActivityId(), stockContext.getProductId());
                stringRedisTemplate.opsForValue().set(
                        REFUND_ORDER_META_KEY_PREFIX + orderId,
                        stockContext.getActivityId() + ":" + stockContext.getProductId(),
                        REFUND_INDEX_TTL_HOURS, TimeUnit.HOURS);
                log.info("[真实库存] 扣减成功，已发 MySQL 更新 MQ orderId:{} activityId:{} productId:{}",
                        orderId, stockContext.getActivityId(), stockContext.getProductId());
            } else if (realResult == 0) {
                log.warn("[真实库存] 库存不足（Redis real=0） orderId:{} activityId:{} productId:{}",
                        orderId, stockContext.getActivityId(), stockContext.getProductId());
            } else {
                log.warn("[真实库存] 扣减异常 result:{} orderId:{} activityId:{} productId:{}",
                        realResult, orderId, stockContext.getActivityId(), stockContext.getProductId());
            }

        } catch (Exception e) {
            log.error("order-paid-seckill 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }

    private StockContext loadStockContext(String orderId) {
        String orderMeta = stringRedisTemplate.opsForValue().get(ORDER_META_KEY_PREFIX + orderId);
        if (StringUtils.isNotBlank(orderMeta)) {
            return parseStockContext(orderMeta, orderId, "orderMeta");
        }

        String seckillToken = stringRedisTemplate.opsForValue().get(TOKEN_BY_ORDER_KEY_PREFIX + orderId);
        if (StringUtils.isBlank(seckillToken)) {
            throw new IllegalStateException("[秒杀支付成功] Redis 未找到 orderMeta 和 seckillToken orderId:" + orderId);
        }

        String tokenValue = stringRedisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + seckillToken);
        if (StringUtils.isBlank(tokenValue)) {
            throw new IllegalStateException("[秒杀支付成功] Redis token 已过期且无 orderMeta seckillToken:" + seckillToken + " orderId:" + orderId);
        }

        return parseStockContext(tokenValue, orderId, "token");
    }

    private StockContext parseStockContext(String rawValue, String orderId, String source) {
        String[] parts = rawValue.split(":");
        if (parts.length < 2) {
            throw new IllegalStateException("[秒杀支付成功] 上下文格式异常 source:" + source + " rawValue:" + rawValue + " orderId:" + orderId);
        }

        String productId;
        String activityIdValue;
        if (parts.length >= 3) {
            productId = parts[1];
            activityIdValue = parts[2];
        } else {
            productId = parts[0];
            activityIdValue = parts[1];
        }

        if (StringUtils.isAnyBlank(productId, activityIdValue)) {
            throw new IllegalStateException("[秒杀支付成功] 上下文字段缺失 source:" + source + " rawValue:" + rawValue + " orderId:" + orderId);
        }

        try {
            return new StockContext(Long.parseLong(activityIdValue), productId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("[秒杀支付成功] activityId 解析失败 source:" + source + " rawValue:" + rawValue + " orderId:" + orderId, e);
        }
    }

    private static final class StockContext {
        private final Long activityId;
        private final String productId;

        private StockContext(Long activityId, String productId) {
            this.activityId = activityId;
            this.productId = productId;
        }

        public Long getActivityId() {
            return activityId;
        }

        public String getProductId() {
            return productId;
        }
    }
}

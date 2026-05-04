package com.yue.seckill.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
 * 秒杀订单关单消费者：恢复 Redis 可售库存。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.orderCloseSeckill:order-close-seckill}",
        consumerGroup = "${app.rocketmq.consumerGroup.orderCloseSeckill:CG_ORDER_CLOSE_SECKILL_MARKET}"
)
public class OrderCloseSeckillListener implements RocketMQListener<String> {

    private static final String TOKEN_BY_ORDER_KEY_PREFIX = "seckill:token:by:order:";
    private static final String TOKEN_KEY_PREFIX = "seckill:token:";
    private static final String ORDER_META_KEY_PREFIX = "seckill:order:meta:";
    private static final String ORDER_CLOSE_HANDLED_KEY_PREFIX = "seckill:order:close:handled:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ISeckillStockPort seckillStockPort;

    @Override
    public void onMessage(String message) {
        log.info("order-close-seckill 收到消息: {}", message);
        String handledKey = null;
        try {
            JSONObject dto = JSON.parseObject(message);
            String orderId = dto.getString("orderId");
            if (StringUtils.isBlank(orderId)) {
                log.error("消息缺少 orderId: {}", message);
                return;
            }

            handledKey = ORDER_CLOSE_HANDLED_KEY_PREFIX + orderId;
            Boolean firstHandle = stringRedisTemplate.opsForValue().setIfAbsent(
                    handledKey, "1", 2, TimeUnit.HOURS);
            if (!Boolean.TRUE.equals(firstHandle)) {
                log.info("[秒杀关单] 已处理过，跳过重复返库 orderId:{}", orderId);
                return;
            }

            StockContext stockContext = loadStockContext(orderId);
            if (stockContext == null) {
                log.warn("[秒杀关单] 库存上下文不存在（活动可能已结束），跳过可售库存恢复 orderId:{}", orderId);
                return;
            }

            // 这里只恢复可售库存；真实库存只在支付成功链路扣减，不在关单时回滚。
            seckillStockPort.recoverStock(stockContext.getActivityId(), stockContext.getProductId());
            log.info("[秒杀关单] 可售库存恢复成功 orderId:{} activityId:{} productId:{}",
                    orderId, stockContext.getActivityId(), stockContext.getProductId());
        } catch (Exception e) {
            if (handledKey != null) {
                stringRedisTemplate.delete(handledKey);
            }
            log.error("order-close-seckill 处理失败: {}", message, e);
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
            // 活动已结束，Redis 数据已过期，无需恢复库存
            log.warn("[秒杀关单] Redis 未找到 orderMeta 和 seckillToken，视为活动已结束 orderId:{}", orderId);
            return null;
        }

        String tokenValue = stringRedisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + seckillToken);
        if (StringUtils.isBlank(tokenValue)) {
            // token 已过期，活动数据不在 Redis，无需恢复
            log.warn("[秒杀关单] Redis token 已过期且无 orderMeta，视为活动已结束 seckillToken:{} orderId:{}", seckillToken, orderId);
            return null;
        }

        return parseStockContext(tokenValue, orderId, "token");
    }

    private StockContext parseStockContext(String rawValue, String orderId, String source) {
        String[] parts = rawValue.split(":");
        if (parts.length < 2) {
            throw new IllegalStateException("[秒杀关单] 上下文格式异常 source:" + source + " rawValue:" + rawValue + " orderId:" + orderId);
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

        if (StringUtils.isBlank(productId) || StringUtils.isBlank(activityIdValue)) {
            throw new IllegalStateException("[秒杀关单] 上下文字段缺失 source:" + source + " rawValue:" + rawValue + " orderId:" + orderId);
        }

        try {
            return new StockContext(Long.parseLong(activityIdValue), productId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("[秒杀关单] activityId 解析失败 source:" + source + " rawValue:" + rawValue + " orderId:" + orderId, e);
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

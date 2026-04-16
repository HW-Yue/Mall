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

/**
 * 秒杀支付成功消费者（order-paid-seckill）
 *
 * <p>流程：
 * 1. 通过 outTradeNo 从 Redis 反向查出 seckillToken
 * 2. 通过 seckillToken 解析出 activityId 和 productId
 * 3. Lua 原子扣减 Redis 真实库存（seckill:stock:real:...）
 * 4. 扣减成功后发 seckill-stock-deduct MQ，异步更新 MySQL remain_count
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
            String outTradeNo = dto.getString("outTradeNo");

            if (StringUtils.isBlank(outTradeNo)) {
                log.error("[秒杀支付成功] 消息缺少 outTradeNo: {}", message);
                return;
            }

            // orderId 是内部订单号，建单时 SeckillOrderCreateListener 用它写了反向索引
            // outTradeNo 是支付宝交易号，两者不同，必须用 orderId 查 token
            String orderId = dto.getString("orderId");
            if (StringUtils.isBlank(orderId)) {
                log.error("[秒杀支付成功] 消息缺少 orderId: {}", message);
                return;
            }

            // 1. orderId → seckillToken（建单时由 order-service 写入）
            String seckillToken = stringRedisTemplate.opsForValue().get(TOKEN_BY_ORDER_KEY_PREFIX + orderId);
            if (StringUtils.isBlank(seckillToken)) {
                log.warn("[秒杀支付成功] Redis 未找到 seckillToken orderId:{}", orderId);
                return;
            }

            // 2. seckillToken → userId:productId:activityId
            String tokenValue = stringRedisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + seckillToken);
            if (StringUtils.isBlank(tokenValue)) {
                log.warn("[秒杀支付成功] Redis token 已过期 seckillToken:{}", seckillToken);
                return;
            }

            String[] parts = tokenValue.split(":");
            if (parts.length < 3) {
                log.error("[秒杀支付成功] token 格式异常 tokenValue:{}", tokenValue);
                return;
            }
            String productId = parts[1];
            Long activityId;
            try {
                activityId = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                log.error("[秒杀支付成功] activityId 解析失败 tokenValue:{}", tokenValue);
                return;
            }

            // 3. Lua 原子扣减 Redis 真实库存
            int realResult = seckillStockPort.deductRealStockByLua(activityId, productId);
            if (realResult == 1) {
                // 4. 扣减成功 → 发 MQ 异步更新 MySQL remain_count
                seckillStockDeductPort.sendDeductStockTask(activityId, productId);
                log.info("[真实库存] 扣减成功，已发 MySQL 更新 MQ outTradeNo:{} activityId:{} productId:{}", outTradeNo, activityId, productId);
            } else if (realResult == 0) {
                log.warn("[真实库存] 库存不足（Redis real=0） outTradeNo:{} activityId:{} productId:{}", outTradeNo, activityId, productId);
            } else {
                log.warn("[真实库存] 扣减异常 result:{} outTradeNo:{} activityId:{} productId:{}", realResult, outTradeNo, activityId, productId);
            }

        } catch (Exception e) {
            log.error("order-paid-seckill 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }

}

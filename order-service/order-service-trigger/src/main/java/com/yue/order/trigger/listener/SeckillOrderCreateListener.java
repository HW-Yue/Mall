package com.yue.order.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yue.order.domain.order.model.entity.CreateOrderCommand;
import com.yue.order.domain.order.service.IOrderDomainService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀建单任务消费者（seckill-order-create）
 * 异步接收 seckill-service 的建单请求，在订单库落单，并将结果写入 Redis 供前端轮询
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
        topic = "${app.rocketmq.topic.seckillOrderCreate:seckill-order-create}",
        consumerGroup = "${app.rocketmq.consumerGroup.seckillOrderCreate:CG_SECKILL_ORDER_CREATE_ORDER}"
)
public class SeckillOrderCreateListener implements RocketMQListener<String> {

    private static final String TOKEN_STATUS_KEY_PREFIX = "seckill:token:status:";
    private static final String ORDER_RESULT_KEY_PREFIX = "seckill:order:";
    private static final String TOKEN_BY_ORDER_KEY_PREFIX = "seckill:token:by:order:";
    private static final String ORDER_META_KEY_PREFIX = "seckill:order:meta:";
    private static final long TTL_MINUTES = 120;
    private static final long META_TTL_HOURS = 24;

    @Resource
    private IOrderDomainService orderDomainService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(String message) {
        log.info("seckill-order-create 收到消息: {}", message);
        try {
            JSONObject dto = JSON.parseObject(message);
            String seckillToken = dto.getString("seckillToken");
            String userId = dto.getString("userId");
            String productId = dto.getString("productId");
            Long activityId = dto.getLong("activityId");
            String source = dto.getString("source");
            String channel = dto.getString("channel");
            String goodsName = dto.getString("goodsName");
            String goodsImageUrl = dto.getString("goodsImageUrl");
            BigDecimal originalPrice = parseBigDecimal(dto.getString("originalPrice"));
            BigDecimal deductionPrice = parseBigDecimal(dto.getString("deductionPrice"));
            BigDecimal payPrice = parseBigDecimal(dto.getString("payPrice"));

            if (StringUtils.isAnyBlank(seckillToken, userId, productId) || activityId == null) {
                log.error("seckill-order-create 消息字段缺失: {}", message);
                return;
            }
            if (payPrice == null || payPrice.compareTo(BigDecimal.ZERO) < 0) {
                log.error("seckill-order-create payPrice 无效: {}", message);
                return;
            }

            CreateOrderCommand command = CreateOrderCommand.builder()
                    .userId(userId)
                    .goodsId(productId)
                    .marketType("seckill")
                    .originalPrice(originalPrice != null ? originalPrice : BigDecimal.ZERO)
                    .deductionPrice(deductionPrice != null ? deductionPrice : BigDecimal.ZERO)
                    .payPrice(payPrice)
                    .source(source)
                    .channel(channel)
                    .goodsName(goodsName)
                    .goodsImageUrl(goodsImageUrl)
                    .build();

            String orderId = orderDomainService.createOrder(command);

            // 前端查询结果仍保留 2 小时；秒杀链路元数据单独保留 24 小时，覆盖超时关单/支付补偿/MQ 重试窗口。
            stringRedisTemplate.opsForValue().set(ORDER_RESULT_KEY_PREFIX + seckillToken, orderId, TTL_MINUTES, TimeUnit.MINUTES);
            stringRedisTemplate.opsForValue().set(TOKEN_STATUS_KEY_PREFIX + seckillToken, "1", TTL_MINUTES, TimeUnit.MINUTES);
            stringRedisTemplate.opsForValue().set(TOKEN_BY_ORDER_KEY_PREFIX + orderId, seckillToken, META_TTL_HOURS, TimeUnit.HOURS);
            stringRedisTemplate.opsForValue().set(ORDER_META_KEY_PREFIX + orderId, productId + ":" + activityId, META_TTL_HOURS, TimeUnit.HOURS);

            log.info("秒杀建单成功 seckillToken:{} orderId:{}", seckillToken, orderId);
        } catch (Exception e) {
            log.error("seckill-order-create 处理失败: {}", message, e);
            throw new RuntimeException(e);
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (StringUtils.isBlank(value)) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

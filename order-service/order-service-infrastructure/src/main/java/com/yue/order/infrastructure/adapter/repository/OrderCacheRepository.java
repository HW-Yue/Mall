package com.yue.order.infrastructure.adapter.repository;

import com.yue.order.domain.order.adapter.repository.IOrderCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.time.Duration;

@Slf4j
@Repository
public class OrderCacheRepository implements IOrderCacheRepository {

    private static final String KEY_PREFIX = "order:exists:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void markPending(String userId, String orderId, String outTradeNo, Duration ttl) {
        if (StringUtils.isAnyBlank(userId, orderId)) {
            return;
        }
        String key = buildKey(userId, orderId);
        Duration effective = ttl != null && !ttl.isZero() && !ttl.isNegative() ? ttl : Duration.ofMinutes(30);
        try {
            stringRedisTemplate.opsForValue().set(key, StringUtils.defaultString(outTradeNo), effective);
        } catch (Exception e) {
            log.error("order:exists 写入失败 userId:{} orderId:{}", userId, orderId, e);
        }
    }

    @Override
    public boolean existsPending(String userId, String orderId) {
        if (StringUtils.isAnyBlank(userId, orderId)) {
            return false;
        }
        try {
            Boolean has = stringRedisTemplate.hasKey(buildKey(userId, orderId));
            return Boolean.TRUE.equals(has);
        } catch (Exception e) {
            log.error("order:exists 读取失败 userId:{} orderId:{}", userId, orderId, e);
            return false;
        }
    }

    @Override
    public void clearPending(String userId, String orderId) {
        if (StringUtils.isAnyBlank(userId, orderId)) {
            return;
        }
        try {
            stringRedisTemplate.delete(buildKey(userId, orderId));
        } catch (Exception e) {
            log.error("order:exists 删除失败 userId:{} orderId:{}", userId, orderId, e);
        }
    }

    private String buildKey(String userId, String orderId) {
        return KEY_PREFIX + userId + ":" + orderId;
    }
}

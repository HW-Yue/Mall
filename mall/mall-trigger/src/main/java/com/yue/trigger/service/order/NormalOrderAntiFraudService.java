package com.yue.trigger.service.order;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;

/**
 * 普通品下单：Redis 简单防刷（短窗去重 + 分钟级次数）
 */
@Slf4j
@Service
public class NormalOrderAntiFraudService {

    private static final String DEDUP_PREFIX = "mall:order:dedup:";
    private static final String RATE_PREFIX = "mall:order:rate:min:";

    @Resource
    private RedissonClient redissonClient;

    /**
     * @return 错误信息，null 表示通过
     */
    public String checkOrNull(String userId, String goodsId) {
        if (StringUtils.isAnyBlank(userId, goodsId)) {
            return "userId / goodsId 不能为空";
        }
        String dedupKey = DEDUP_PREFIX + userId + ":" + goodsId;
        RBucket<String> dedup = redissonClient.getBucket(dedupKey);
        boolean first = dedup.setIfAbsent("1", Duration.ofSeconds(2));
        if (!first) {
            return "提交过于频繁，请稍后再试";
        }

        RAtomicLong counter = redissonClient.getAtomicLong(RATE_PREFIX + userId);
        long n = counter.incrementAndGet();
        if (n == 1) {
            counter.expire(Duration.ofMinutes(1));
        }
        if (n > 50) {
            return "下单过于频繁，请稍后再试";
        }
        return null;
    }
}

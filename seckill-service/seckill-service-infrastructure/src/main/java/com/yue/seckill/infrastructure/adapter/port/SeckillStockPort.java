package com.yue.seckill.infrastructure.adapter.port;

import com.yue.seckill.domain.trade.adapter.port.ISeckillStockPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀库存 Redis 实现
 */
@Service
public class SeckillStockPort implements ISeckillStockPort {

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String USER_BOUGHT_KEY_PREFIX = "seckill:user:";
    private static final String TOKEN_KEY_PREFIX = "seckill:token:";
    private static final String TOKEN_STATUS_KEY_PREFIX = "seckill:token:status:";

    /**
     * Lua 脚本返回值：
     *  1: 扣减成功
     *  0: 库存不足
     * -1: 重复购买
     * -2: 库存 key 不存在（未预热）
     */
    private static final String STOCK_DEDUCT_LUA =
            "local stockKey = KEYS[1];" +
            "local userKey = KEYS[2];" +
            "local userId = ARGV[1];" +
            "local stock = redis.call('GET', stockKey);" +
            "if (not stock) then return -2 end;" +
            "local hasBought = redis.call('SISMEMBER', userKey, userId);" +
            "if (hasBought == 1) then return -1 end;" +
            "if (tonumber(stock) <= 0) then return 0 end;" +
            "redis.call('DECR', stockKey);" +
            "redis.call('SADD', userKey, userId);" +
            "return 1;";

    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT = new DefaultRedisScript<>();

    static {
        DEDUCT_SCRIPT.setScriptText(STOCK_DEDUCT_LUA);
        DEDUCT_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void preloadStock(Long activityId, String goodsId, Integer stock) {
        stringRedisTemplate.opsForValue().set(buildStockKey(activityId, goodsId), String.valueOf(stock));
    }

    @Override
    public void preloadStock(Long activityId, String goodsId, Integer stock, long expireSeconds) {
        stringRedisTemplate.opsForValue().set(buildStockKey(activityId, goodsId), String.valueOf(stock), expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String getStockValue(Long activityId, String goodsId) {
        return stringRedisTemplate.opsForValue().get(buildStockKey(activityId, goodsId));
    }

    @Override
    public int deductByLua(Long activityId, String goodsId, String userId) {
        Long result = stringRedisTemplate.execute(
                DEDUCT_SCRIPT,
                Arrays.asList(buildStockKey(activityId, goodsId), buildUserKey(activityId, goodsId)),
                userId
        );
        return result != null ? result.intValue() : 0;
    }

    @Override
    public void recoverStock(Long activityId, String goodsId) {
        stringRedisTemplate.opsForValue().increment(buildStockKey(activityId, goodsId));
    }

    @Override
    public void saveSeckillToken(String seckillToken, String userId, String goodsId, Long activityId) {
        String tokenValue = userId + ":" + goodsId + ":" + activityId;
        stringRedisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + seckillToken, tokenValue, 30, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(TOKEN_STATUS_KEY_PREFIX + seckillToken, "0", 30, TimeUnit.MINUTES);
    }

    private String buildStockKey(Long activityId, String goodsId) {
        return STOCK_KEY_PREFIX + activityId + ":" + goodsId;
    }

    private String buildUserKey(Long activityId, String goodsId) {
        return USER_BOUGHT_KEY_PREFIX + activityId + ":" + goodsId;
    }

}

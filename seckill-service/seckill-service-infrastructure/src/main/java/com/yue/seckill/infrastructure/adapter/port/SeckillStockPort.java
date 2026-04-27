package com.yue.seckill.infrastructure.adapter.port;

import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import com.yue.seckill.domain.activity.model.valobj.SeckillActivityDiscountVO;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockPort;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀库存 Redis 实现
 *
 * <p>双层库存设计：
 * <ul>
 *   <li><b>可售库存</b>（available）：下单时 Lua 原子扣减，订单取消/超时时恢复</li>
 *   <li><b>真实库存</b>（real）：支付成功后 Lua 原子扣减，随后异步更新 MySQL</li>
 * </ul>
 * 预热时两个 key 初始化为相同值（均来自 MySQL remain_count）。
 */
@Slf4j
@Service
public class SeckillStockPort implements ISeckillStockPort {

    // ── key 前缀 ──────────────────────────────────────────────────────────
    private static final String AVAILABLE_STOCK_KEY_PREFIX = "seckill:stock:available:";
    private static final String REAL_STOCK_KEY_PREFIX      = "seckill:stock:real:";
    private static final String USER_BOUGHT_KEY_PREFIX     = "seckill:user:";
    private static final String TOKEN_KEY_PREFIX           = "seckill:token:";
    private static final String TOKEN_STATUS_KEY_PREFIX    = "seckill:token:status:";
    private static final long TOKEN_TTL_HOURS              = 24L;
    private static final String STOCK_INIT_LOCK_PREFIX     = "seckill:init:lock:";

    private static final long INIT_LOCK_WAIT_SECONDS = 3L;
    private static final long STOCK_EXPIRE_SECONDS   = 7200L;

    // ── Lua：扣减可售库存（含用户去重）────────────────────────────────────
    /**
     * 返回值：1=成功, 0=库存不足, -1=重复购买, -2=key 不存在（未预热）
     */
    private static final String AVAILABLE_DEDUCT_LUA =
            "local stockKey = KEYS[1];" +
            "local userKey  = KEYS[2];" +
            "local userId   = ARGV[1];" +
            "local stock = redis.call('GET', stockKey);" +
            "if (not stock) then return -2 end;" +
            "local hasBought = redis.call('SISMEMBER', userKey, userId);" +
            "if (hasBought == 1) then return -1 end;" +
            "if (tonumber(stock) <= 0) then return 0 end;" +
            "redis.call('DECR', stockKey);" +
            "redis.call('SADD', userKey, userId);" +
            "return 1;";

    private static final String ROLLBACK_ORDER_LUA =
            "local stockKey = KEYS[1];" +
            "local userKey  = KEYS[2];" +
            "local userId   = ARGV[1];" +
            "local removed = redis.call('SREM', userKey, userId);" +
            "if (removed == 1) then redis.call('INCR', stockKey); end;" +
            "return removed;";

    // ── Lua：扣减真实库存（无需用户去重）─────────────────────────────────
    /**
     * 返回值：1=成功, 0=库存不足, -2=key 不存在（未预热）
     */
    private static final String REAL_DEDUCT_LUA =
            "local realKey = KEYS[1];" +
            "local stock = redis.call('GET', realKey);" +
            "if (not stock) then return -2 end;" +
            "if (tonumber(stock) <= 0) then return 0 end;" +
            "redis.call('DECR', realKey);" +
            "return 1;";

    private static final DefaultRedisScript<Long> AVAILABLE_DEDUCT_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> REAL_DEDUCT_SCRIPT      = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> ROLLBACK_ORDER_SCRIPT   = new DefaultRedisScript<>();

    static {
        AVAILABLE_DEDUCT_SCRIPT.setScriptText(AVAILABLE_DEDUCT_LUA);
        AVAILABLE_DEDUCT_SCRIPT.setResultType(Long.class);
        REAL_DEDUCT_SCRIPT.setScriptText(REAL_DEDUCT_LUA);
        REAL_DEDUCT_SCRIPT.setResultType(Long.class);
        ROLLBACK_ORDER_SCRIPT.setScriptText(ROLLBACK_ORDER_LUA);
        ROLLBACK_ORDER_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ISeckillActivityRepository seckillActivityRepository;

    // ── 预热：同时初始化可售 key 和真实 key ──────────────────────────────

    @Override
    public void preloadStock(Long activityId, String goodsId, Integer stock) {
        stringRedisTemplate.opsForValue().set(buildAvailableKey(activityId, goodsId), String.valueOf(stock));
        stringRedisTemplate.opsForValue().set(buildRealKey(activityId, goodsId), String.valueOf(stock));
    }

    @Override
    public void preloadStock(Long activityId, String goodsId, Integer stock, long expireSeconds) {
        stringRedisTemplate.opsForValue().set(buildAvailableKey(activityId, goodsId), String.valueOf(stock), expireSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(buildRealKey(activityId, goodsId), String.valueOf(stock), expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String getStockValue(Long activityId, String goodsId) {
        return stringRedisTemplate.opsForValue().get(buildAvailableKey(activityId, goodsId));
    }

    // ── 下单：扣减可售库存 ─────────────────────────────────────────────

    @Override
    public int deductByLua(Long activityId, String goodsId, String userId) {
        int result = executeAvailableDeduct(activityId, goodsId, userId);
        if (result != -2) {
            return result;
        }
        // key 不存在 → 懒加载后重试一次
        lazyInitStock(activityId, goodsId);
        return executeAvailableDeduct(activityId, goodsId, userId);
    }

    private int executeAvailableDeduct(Long activityId, String goodsId, String userId) {
        Long result = stringRedisTemplate.execute(
                AVAILABLE_DEDUCT_SCRIPT,
                Arrays.asList(buildAvailableKey(activityId, goodsId), buildUserKey(activityId, goodsId)),
                userId
        );
        return result != null ? result.intValue() : 0;
    }

    // ── 支付成功：扣减真实库存 ────────────────────────────────────────

    @Override
    public int deductRealStockByLua(Long activityId, String goodsId) {
        Long result = stringRedisTemplate.execute(
                REAL_DEDUCT_SCRIPT,
                Collections.singletonList(buildRealKey(activityId, goodsId))
        );
        return result != null ? result.intValue() : 0;
    }

    // ── 取消/超时：恢复可售库存（真实库存不恢复）──────────────────────

    @Override
    public void recoverStock(Long activityId, String goodsId) {
        stringRedisTemplate.opsForValue().increment(buildAvailableKey(activityId, goodsId));
    }

    @Override
    public void rollbackSeckillOrder(Long activityId, String goodsId, String userId, String seckillToken) {
        Long removed = stringRedisTemplate.execute(
                ROLLBACK_ORDER_SCRIPT,
                Arrays.asList(buildAvailableKey(activityId, goodsId), buildUserKey(activityId, goodsId)),
                userId
        );
        stringRedisTemplate.delete(TOKEN_KEY_PREFIX + seckillToken);
        stringRedisTemplate.delete(TOKEN_STATUS_KEY_PREFIX + seckillToken);
        log.info("[秒杀回滚] activityId:{} goodsId:{} userId:{} seckillToken:{} removed:{}",
                activityId, goodsId, userId, seckillToken, removed);
    }

    // ── 令牌 ──────────────────────────────────────────────────────────

    @Override
    public void saveSeckillToken(String seckillToken, String userId, String goodsId, Long activityId) {
        String tokenValue = userId + ":" + goodsId + ":" + activityId;
        // 保留 24 小时，覆盖下单后轮询、支付回调补偿、超时关单返库与 MQ 重试窗口。
        stringRedisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + seckillToken, tokenValue, TOKEN_TTL_HOURS, TimeUnit.HOURS);
        stringRedisTemplate.opsForValue().set(TOKEN_STATUS_KEY_PREFIX + seckillToken, "0", TOKEN_TTL_HOURS, TimeUnit.HOURS);
    }

    // ── 懒加载初始化（两个 key 均初始化）────────────────────────────────

    private void lazyInitStock(Long activityId, String goodsId) {
        String lockKey = STOCK_INIT_LOCK_PREFIX + activityId + ":" + goodsId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(INIT_LOCK_WAIT_SECONDS, -1L, TimeUnit.SECONDS);
            if (acquired) {
                // double-check
                if (stringRedisTemplate.opsForValue().get(buildAvailableKey(activityId, goodsId)) != null) {
                    log.debug("[懒加载] double-check 命中 activityId:{} goodsId:{}", activityId, goodsId);
                    return;
                }
                SeckillActivityDiscountVO vo = seckillActivityRepository.querySeckillActivityDiscountVO(activityId);
                if (vo == null || vo.getRemainCount() == null) {
                    log.warn("[懒加载] DB 无活动数据 activityId:{}", activityId);
                    return;
                }
                int remainCount = Math.max(vo.getRemainCount(), 0);
                preloadStock(activityId, goodsId, remainCount, STOCK_EXPIRE_SECONDS);
                log.info("[懒加载] 双层库存初始化完成 activityId:{} goodsId:{} remainCount:{}", activityId, goodsId, remainCount);
            } else {
                log.warn("[懒加载] 未抢到初始化锁 activityId:{} goodsId:{}", activityId, goodsId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[懒加载] 等待锁被中断 activityId:{} goodsId:{}", activityId, goodsId, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ── key 构建工具 ──────────────────────────────────────────────────

    private String buildAvailableKey(Long activityId, String goodsId) {
        return AVAILABLE_STOCK_KEY_PREFIX + activityId + ":" + goodsId;
    }

    private String buildRealKey(Long activityId, String goodsId) {
        return REAL_STOCK_KEY_PREFIX + activityId + ":" + goodsId;
    }

    private String buildUserKey(Long activityId, String goodsId) {
        return USER_BOUGHT_KEY_PREFIX + activityId + ":" + goodsId;
    }

}

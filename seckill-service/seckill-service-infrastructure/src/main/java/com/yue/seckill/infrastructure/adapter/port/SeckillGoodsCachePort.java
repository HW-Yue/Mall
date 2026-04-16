package com.yue.seckill.infrastructure.adapter.port;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yue.seckill.domain.activity.adapter.port.ISeckillGoodsCachePort;
import com.yue.seckill.domain.activity.model.entity.SeckillGoodsEntity;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 秒杀商品三级缓存实现
 *
 * <pre>
 * L1: Caffeine  本地缓存，10s 过期，毫秒级响应
 * L2: Redis     分布式缓存，5min 过期，单实例共享
 * L3: DB        兜底，Redisson 分布式锁 + 看门狗防止缓存击穿
 * </pre>
 */
@Slf4j
@Service
public class SeckillGoodsCachePort implements ISeckillGoodsCachePort {

    private static final String CAFFEINE_KEY = "seckill_goods_list";
    private static final String REDIS_KEY    = "seckill:goods:list";
    private static final String LOCK_KEY     = "seckill:goods:list:lock";
    private static final long   REDIS_TTL_SECONDS   = 300L;  // 5 分钟
    private static final int    CAFFEINE_TTL_SECONDS = 10;   // 10 秒
    private static final long   LOCK_WAIT_SECONDS   = 3L;    // 最长等锁 3s

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    /** L1 本地缓存 */
    private Cache<String, List<SeckillGoodsEntity>> caffeineCache;

    @PostConstruct
    public void init() {
        caffeineCache = Caffeine.newBuilder()
                .expireAfterWrite(CAFFEINE_TTL_SECONDS, TimeUnit.SECONDS)
                .maximumSize(1)  // 商品列表只有一份，无需多 key
                .build();
    }

    @Override
    public List<SeckillGoodsEntity> getGoodsList(Supplier<List<SeckillGoodsEntity>> dbLoader) {
        // ── L1: Caffeine ────────────────────────────────────────────────────
        List<SeckillGoodsEntity> l1 = caffeineCache.getIfPresent(CAFFEINE_KEY);
        if (l1 != null) {
            log.debug("[商品缓存] L1 Caffeine 命中，size={}", l1.size());
            return l1;
        }

        // ── L2: Redis ────────────────────────────────────────────────────────
        String redisJson = stringRedisTemplate.opsForValue().get(REDIS_KEY);
        if (redisJson != null) {
            log.debug("[商品缓存] L2 Redis 命中");
            List<SeckillGoodsEntity> l2 = JSON.parseObject(redisJson, new TypeReference<List<SeckillGoodsEntity>>() {});
            caffeineCache.put(CAFFEINE_KEY, l2);  // 回填 L1
            return l2;
        }

        // ── L3: DB（Redisson 分布式锁 + 看门狗） ─────────────────────────────
        return loadFromDb(dbLoader);
    }

    /**
     * 使用 Redisson 分布式锁保护 DB 查询，看门狗自动续约防止锁超时。
     * <ul>
     *   <li>leaseTime = -1 → 启用看门狗（默认 30s 内每 10s 续约一次）</li>
     *   <li>waitTime = 3s → 若 3s 内未拿到锁则走降级路径</li>
     * </ul>
     */
    private List<SeckillGoodsEntity> loadFromDb(Supplier<List<SeckillGoodsEntity>> dbLoader) {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, -1L, TimeUnit.SECONDS);
            if (acquired) {
                // 拿到锁后 double-check Redis，防止并发时重复查 DB
                String redisJsonAgain = stringRedisTemplate.opsForValue().get(REDIS_KEY);
                if (redisJsonAgain != null) {
                    log.debug("[商品缓存] double-check Redis 命中（并发场景）");
                    List<SeckillGoodsEntity> list = JSON.parseObject(redisJsonAgain, new TypeReference<List<SeckillGoodsEntity>>() {});
                    caffeineCache.put(CAFFEINE_KEY, list);
                    return list;
                }

                // 真正查 DB
                log.info("[商品缓存] L2 Redis 未命中，回源 DB");
                List<SeckillGoodsEntity> dbList = dbLoader.get();
                if (dbList == null) dbList = Collections.emptyList();

                // 写入 Redis（L2）
                stringRedisTemplate.opsForValue().set(REDIS_KEY, JSON.toJSONString(dbList), REDIS_TTL_SECONDS, TimeUnit.SECONDS);
                // 写入 Caffeine（L1）
                caffeineCache.put(CAFFEINE_KEY, dbList);
                log.info("[商品缓存] 已从 DB 加载并写入 L1/L2，size={}", dbList.size());
                return dbList;
            }

            // 未拿到锁（其他实例正在加载）→ 再次尝试 Redis，兜底直接走 DB
            log.warn("[商品缓存] 未拿到分布式锁，降级：再查 Redis");
            String fallbackJson = stringRedisTemplate.opsForValue().get(REDIS_KEY);
            if (fallbackJson != null) {
                return JSON.parseObject(fallbackJson, new TypeReference<List<SeckillGoodsEntity>>() {});
            }
            log.warn("[商品缓存] Redis 仍未命中，直接查 DB（降级）");
            return dbLoader.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[商品缓存] 等待锁被中断，降级直接查 DB", e);
            return dbLoader.get();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}

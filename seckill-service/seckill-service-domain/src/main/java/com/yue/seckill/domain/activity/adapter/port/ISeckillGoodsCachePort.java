package com.yue.seckill.domain.activity.adapter.port;

import com.yue.seckill.domain.activity.model.entity.SeckillGoodsEntity;

import java.util.List;
import java.util.function.Supplier;

/**
 * 秒杀商品三级缓存端口（Caffeine → Redis → DB）
 */
public interface ISeckillGoodsCachePort {

    /**
     * 获取秒杀商品列表，按 Caffeine → Redis → DB 顺序查找。
     * DB 回源时使用 Redisson 分布式锁 + 看门狗机制防止缓存击穿。
     *
     * @param dbLoader DB 兜底查询（仅在两级缓存均未命中时调用）
     */
    List<SeckillGoodsEntity> getGoodsList(Supplier<List<SeckillGoodsEntity>> dbLoader);

}

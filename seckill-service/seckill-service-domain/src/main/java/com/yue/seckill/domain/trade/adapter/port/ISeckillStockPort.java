package com.yue.seckill.domain.trade.adapter.port;

/**
 * 秒杀库存 Redis 端口
 */
public interface ISeckillStockPort {

    void preloadStock(Long activityId, String goodsId, Integer stock);

    /**
     * 预热库存到 Redis（带过期时间）
     *
     * @param expireSeconds 过期时间（秒）
     */
    void preloadStock(Long activityId, String goodsId, Integer stock, long expireSeconds);

    /**
     * 查询 Redis 中的库存值
     *
     * @return 库存值字符串，null 表示未预热
     */
    String getStockValue(Long activityId, String goodsId);

    /**
     * Lua 扣减可售库存（下单时调用，含用户去重）
     * @return 1: 扣减成功, 0: 库存不足, -1: 重复购买, -2: 未初始化
     */
    int deductByLua(Long activityId, String goodsId, String userId);

    /**
     * Lua 扣减真实库存（支付成功后调用，无需用户去重）
     * @return 1: 扣减成功, 0: 库存不足, -2: 未初始化
     */
    int deductRealStockByLua(Long activityId, String goodsId);

    void recoverStock(Long activityId, String goodsId);

    void rollbackSeckillOrder(Long activityId, String goodsId, String userId, String seckillToken);

    void saveSeckillToken(String seckillToken, String userId, String goodsId, Long activityId);

}

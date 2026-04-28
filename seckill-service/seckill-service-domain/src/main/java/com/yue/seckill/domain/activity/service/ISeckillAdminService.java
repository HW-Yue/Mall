package com.yue.seckill.domain.activity.service;

import com.yue.seckill.domain.activity.model.valobj.SeckillActivityWithGoodsVO;

import java.util.List;

/**
 * 秒杀管理服务（管理后台：预热库存）
 */
public interface ISeckillAdminService {

    /**
     * 查询有效活动及商品列表，用于管理后台预热选择
     */
    List<SeckillActivityWithGoodsVO> queryActivitiesWithGoods();

    /**
     * 手动预热指定商品的库存
     *
     * @param activityId    活动ID
     * @param goodsId       商品ID
     * @param stock         预热库存数（null 则使用数据库 stock_count）
     * @param expireSeconds 过期时间（秒）
     */
    void preheatStock(Long activityId, String goodsId, Integer stock, long expireSeconds);

    /**
     * 查询当前 Redis 中商品的预热状态
     *
     * @return 库存值字符串，null 表示未预热
     */
    String getPreheatStatus(Long activityId, String goodsId);

}

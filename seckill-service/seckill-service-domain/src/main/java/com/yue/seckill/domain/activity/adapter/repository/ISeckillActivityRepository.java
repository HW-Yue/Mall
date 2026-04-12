package com.yue.seckill.domain.activity.adapter.repository;

import com.yue.seckill.domain.activity.model.entity.SeckillActivityEntity;
import com.yue.seckill.domain.activity.model.entity.SeckillGoodsEntity;
import com.yue.seckill.domain.activity.model.valobj.SeckillActivityDiscountVO;
import com.yue.seckill.domain.activity.model.valobj.SeckillActivityWithGoodsVO;
import com.yue.seckill.domain.activity.model.valobj.SeckillStockVO;
import com.yue.seckill.domain.activity.model.valobj.SkuVO;

import java.util.List;

/**
 * 秒杀活动仓储接口
 */
public interface ISeckillActivityRepository {

    /**
     * 查询秒杀活动折扣信息
     */
    SeckillActivityDiscountVO querySeckillActivityDiscountVO(Long activityId);

    /**
     * 查询商品信息
     */
    SkuVO querySkuByGoodsId(String goodsId);

    /**
     * 查询所有有效秒杀活动
     */
    List<SeckillActivityEntity> queryEffectiveActivities();

    /**
     * 查询活动商品列表
     */
    List<SeckillGoodsEntity> querySeckillGoodsList();

    /**
     * 查询可预热库存列表（goodsId + activityId）
     */
    List<SeckillStockVO> querySeckillStockList();

    /**
     * 查询有效活动及其商品列表（用于管理后台预热选择）
     */
    List<SeckillActivityWithGoodsVO> querySeckillActivitiesWithGoods();

    /**
     * 扣减库存
     */
    boolean deductStock(Long activityId);

    /**
     * 恢复库存
     */
    boolean recoverStock(Long activityId);

    /**
     * 降级开关
     */
    boolean downgradeSwitch();

}

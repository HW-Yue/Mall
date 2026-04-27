package com.yue.seckill.domain.trade.service;

import com.yue.seckill.domain.activity.model.entity.SeckillActivityEntity;
import com.yue.seckill.domain.activity.model.valobj.SkuVO;
import com.yue.seckill.domain.trade.model.entity.SeckillOrderResultEntity;

/**
 * 秒杀交易服务接口
 */
public interface ISeckillTradeService {

    /**
     * 创建秒杀订单
     */
    SeckillOrderResultEntity createSeckillOrder(String userId, String productId, Long activityId,
                                                String source, String channel, String goodsName, String goodsImageUrl,
                                                boolean isTest);

    /**
     * 查询秒杀活动
     */
    SeckillActivityEntity querySeckillActivity(Long activityId);

    /**
     * 查询商品
     */
    SkuVO querySku(String goodsId);

    /**
     * 退款
     */
    void refund(String userId, String orderId);

}

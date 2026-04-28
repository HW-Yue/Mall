package com.yue.seckill.domain.trade.adapter.port;

/**
 * 秒杀库存异步扣减端口（发送 MQ 通知 MySQL 更新 sc_sku_activity.stock_count）
 */
public interface ISeckillStockDeductPort {

    void sendDeductStockTask(Long activityId, String productId);

}

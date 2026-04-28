package com.yue.seckill.domain.activity.service;

import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import com.yue.seckill.domain.activity.model.valobj.SeckillActivityWithGoodsVO;
import com.yue.seckill.domain.activity.model.valobj.SeckillStockVO;
import com.yue.seckill.domain.trade.adapter.port.ISeckillStockPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 秒杀管理服务实现
 */
@Slf4j
@Service
public class SeckillAdminServiceImpl implements ISeckillAdminService {

    @Resource
    private ISeckillActivityRepository seckillActivityRepository;
    @Resource
    private ISeckillStockPort seckillStockPort;

    @Override
    public List<SeckillActivityWithGoodsVO> queryActivitiesWithGoods() {
        return seckillActivityRepository.querySeckillActivitiesWithGoods();
    }

    @Override
    public void preheatStock(Long activityId, String goodsId, Integer stock, long expireSeconds) {
        int stockToLoad = stock != null ? stock : resolveStockFromDb(activityId, goodsId);
        log.info("[管理预热] activityId:{} goodsId:{} stock:{} expireSeconds:{}", activityId, goodsId, stockToLoad, expireSeconds);
        seckillStockPort.preloadStock(activityId, goodsId, stockToLoad, expireSeconds);
    }

    @Override
    public String getPreheatStatus(Long activityId, String goodsId) {
        return seckillStockPort.getStockValue(activityId, goodsId);
    }

    /**
     * 从数据库查询 sc_sku_activity.stock_count 作为预热库存兜底值
     */
    private int resolveStockFromDb(Long activityId, String goodsId) {
        List<SeckillStockVO> stockList = seckillActivityRepository.querySeckillStockList();
        for (SeckillStockVO vo : stockList) {
            if (activityId.equals(vo.getActivityId()) && goodsId.equals(vo.getGoodsId())) {
                return vo.getRemainCount() != null ? vo.getRemainCount() : 0;
            }
        }
        return 0;
    }

}

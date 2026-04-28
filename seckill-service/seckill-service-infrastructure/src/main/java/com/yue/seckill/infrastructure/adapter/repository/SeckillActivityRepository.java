package com.yue.seckill.infrastructure.adapter.repository;

import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import com.yue.seckill.domain.activity.model.entity.SeckillActivityEntity;
import com.yue.seckill.domain.activity.model.entity.SeckillGoodsEntity;
import com.yue.seckill.domain.activity.model.valobj.SeckillActivityWithGoodsVO;
import com.yue.seckill.domain.activity.model.valobj.SeckillStockVO;
import com.yue.seckill.domain.activity.model.valobj.SkuVO;
import com.yue.seckill.infrastructure.dao.IScSkuActivityDao;
import com.yue.seckill.infrastructure.dao.ISeckillActivityDao;
import com.yue.seckill.infrastructure.dao.ISkuDao;
import com.yue.seckill.infrastructure.dao.po.ScSkuActivity;
import com.yue.seckill.infrastructure.dao.po.SeckillActivity;
import com.yue.seckill.infrastructure.dao.po.Sku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 秒杀活动仓储实现
 */
@Slf4j
@Repository
public class SeckillActivityRepository implements ISeckillActivityRepository {

    @Resource
    private ISeckillActivityDao seckillActivityDao;
    @Resource
    private ISkuDao skuDao;
    @Resource
    private IScSkuActivityDao scSkuActivityDao;

    @Override
    public SeckillActivityEntity querySeckillActivity(Long activityId) {
        SeckillActivity activity = seckillActivityDao.queryByActivityId(activityId);
        if (activity == null) {
            return null;
        }
        Integer totalStock = sumActivityStock(activityId);
        return SeckillActivityEntity.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .seckillPrice(activity.getSeckillPrice())
                .stockCount(totalStock)
                .remainCount(totalStock)
                .takeLimitCount(activity.getTakeLimitCount())
                .status(activity.getStatus())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .tagId(activity.getTagId())
                .tagScope(activity.getTagScope())
                .build();
    }

    @Override
    public SkuVO querySkuByGoodsId(String goodsId) {
        Sku sku = skuDao.queryByGoodsId(goodsId);
        if (sku == null) {
            return null;
        }
        return SkuVO.builder()
                .goodsId(sku.getGoodsId())
                .goodsName(sku.getGoodsName())
                .goodsImageUrl(sku.getGoodsImageUrl())
                .goodsDetail(sku.getGoodsDetail())
                .originalPrice(sku.getOriginalPrice())
                .build();
    }

    @Override
    public List<SeckillActivityEntity> queryEffectiveActivities() {
        List<SeckillActivity> activities = seckillActivityDao.queryEffectiveActivities();
        List<SeckillActivityEntity> result = new ArrayList<>();
        if (activities == null || activities.isEmpty()) {
            return result;
        }

        for (SeckillActivity activity : activities) {
            Integer totalStock = sumActivityStock(activity.getActivityId());
            result.add(SeckillActivityEntity.builder()
                    .activityId(activity.getActivityId())
                    .activityName(activity.getActivityName())
                    .seckillPrice(activity.getSeckillPrice())
                    .stockCount(totalStock)
                    .remainCount(totalStock)
                    .takeLimitCount(activity.getTakeLimitCount())
                    .status(activity.getStatus())
                    .startTime(activity.getStartTime())
                    .endTime(activity.getEndTime())
                    .tagId(activity.getTagId())
                    .tagScope(activity.getTagScope())
                    .build());
        }
        return result;
    }

    @Override
    public List<SeckillGoodsEntity> querySeckillGoodsList() {
        List<SeckillActivity> activities = seckillActivityDao.queryEffectiveActivities();
        List<SeckillGoodsEntity> result = new ArrayList<>();
        if (activities == null || activities.isEmpty()) {
            return result;
        }

        for (SeckillActivity activity : activities) {
            List<ScSkuActivity> scList = scSkuActivityDao.queryByActivityId(activity.getActivityId());
            if (scList == null || scList.isEmpty()) {
                continue;
            }

            for (ScSkuActivity sc : scList) {
                Sku sku = skuDao.queryByGoodsId(sc.getGoodsId());
                if (sku == null) {
                    continue;
                }

                result.add(SeckillGoodsEntity.builder()
                        .goodsId(sku.getGoodsId())
                        .goodsName(sku.getGoodsName())
                        .goodsImageUrl(sku.getGoodsImageUrl())
                        .originalPrice(sku.getOriginalPrice())
                        .payPrice(activity.getSeckillPrice())
                        .source(sc.getSource())
                        .channel(sc.getChannel())
                        .activityId(activity.getActivityId())
                        .build());
            }
        }
        return result;
    }

    @Override
    public List<SeckillStockVO> querySeckillStockList() {
        List<SeckillActivity> activities = seckillActivityDao.queryEffectiveActivities();
        List<SeckillStockVO> result = new ArrayList<>();
        if (activities == null || activities.isEmpty()) {
            return result;
        }

        for (SeckillActivity activity : activities) {
            List<ScSkuActivity> scList = scSkuActivityDao.queryByActivityId(activity.getActivityId());
            if (scList == null || scList.isEmpty()) {
                continue;
            }
            for (ScSkuActivity sc : scList) {
                result.add(SeckillStockVO.builder()
                        .activityId(activity.getActivityId())
                        .goodsId(sc.getGoodsId())
                        .remainCount(sc.getStockCount())
                        .build());
            }
        }
        return result;
    }

    @Override
    public List<SeckillActivityWithGoodsVO> querySeckillActivitiesWithGoods() {
        List<SeckillActivity> activities = seckillActivityDao.queryEffectiveActivities();
        List<SeckillActivityWithGoodsVO> result = new ArrayList<>();
        if (activities == null || activities.isEmpty()) {
            return result;
        }

        for (SeckillActivity activity : activities) {
            List<ScSkuActivity> scList = scSkuActivityDao.queryByActivityId(activity.getActivityId());
            if (scList == null || scList.isEmpty()) {
                continue;
            }

            List<SeckillActivityWithGoodsVO.GoodsItem> goodsItems = new ArrayList<>();
            int totalStock = 0;
            for (ScSkuActivity sc : scList) {
                Sku sku = skuDao.queryByGoodsId(sc.getGoodsId());
                String goodsName = sku != null ? sku.getGoodsName() : sc.getGoodsId();
                goodsItems.add(SeckillActivityWithGoodsVO.GoodsItem.builder()
                        .goodsId(sc.getGoodsId())
                        .goodsName(goodsName)
                        .build());
                totalStock += sc.getStockCount() != null ? sc.getStockCount() : 0;
            }

            result.add(SeckillActivityWithGoodsVO.builder()
                    .activityId(activity.getActivityId())
                    .activityName(activity.getActivityName())
                    .seckillPrice(activity.getSeckillPrice())
                    .remainCount(totalStock)
                    .goodsList(goodsItems)
                    .build());
        }
        return result;
    }

    @Override
    public boolean deductStock(Long activityId, String goodsId) {
        int rows = scSkuActivityDao.deductStock(activityId, goodsId);
        return rows > 0;
    }

    @Override
    public boolean recoverStock(Long activityId, String goodsId) {
        int rows = scSkuActivityDao.recoverStock(activityId, goodsId);
        return rows > 0;
    }

    @Override
    public boolean downgradeSwitch() {
        return false;
    }

    private Integer sumActivityStock(Long activityId) {
        List<ScSkuActivity> scList = scSkuActivityDao.queryByActivityId(activityId);
        if (scList == null || scList.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ScSkuActivity sc : scList) {
            total += sc.getStockCount() != null ? sc.getStockCount() : 0;
        }
        return total;
    }

}

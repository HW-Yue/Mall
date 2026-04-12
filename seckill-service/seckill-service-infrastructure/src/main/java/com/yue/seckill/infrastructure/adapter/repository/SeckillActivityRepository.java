package com.yue.seckill.infrastructure.adapter.repository;

import com.yue.seckill.domain.activity.adapter.repository.ISeckillActivityRepository;
import com.yue.seckill.domain.activity.model.entity.SeckillActivityEntity;
import com.yue.seckill.domain.activity.model.entity.SeckillGoodsEntity;
import com.yue.seckill.domain.activity.model.valobj.SeckillActivityDiscountVO;
import com.yue.seckill.domain.activity.model.valobj.SeckillActivityWithGoodsVO;
import com.yue.seckill.domain.activity.model.valobj.SeckillStockVO;
import com.yue.seckill.domain.activity.model.valobj.SkuVO;
import com.yue.seckill.infrastructure.dao.*;
import com.yue.seckill.infrastructure.dao.po.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
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
    private IDiscountDao discountDao;
    @Resource
    private ISkuDao skuDao;
    @Resource
    private IScSkuActivityDao scSkuActivityDao;

    @Override
    public SeckillActivityDiscountVO querySeckillActivityDiscountVO(Long activityId) {
        SeckillActivity activity = seckillActivityDao.queryByActivityId(activityId);
        if (null == activity || activity.getStatus() != 1) return null;

        Discount discount = discountDao.queryByDiscountId(activity.getDiscountId());
        if (null == discount) return null;

        return SeckillActivityDiscountVO.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .seckillDiscount(SeckillActivityDiscountVO.SeckillDiscount.builder()
                        .discountName(discount.getDiscountName())
                        .discountDesc(discount.getDiscountDesc())
                        .discountType(discount.getDiscountType())
                        .marketPlan(discount.getMarketPlan())
                        .marketExpr(discount.getMarketExpr())
                        .tagId(discount.getTagId())
                        .build())
                .stockCount(activity.getStockCount())
                .remainCount(activity.getRemainCount())
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
        if (null == sku) return null;
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
        if (activities == null || activities.isEmpty()) return result;

        for (SeckillActivity activity : activities) {
            result.add(SeckillActivityEntity.builder()
                    .activityId(activity.getActivityId())
                    .activityName(activity.getActivityName())
                    .discountId(activity.getDiscountId())
                    .stockCount(activity.getStockCount())
                    .remainCount(activity.getRemainCount())
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
        if (activities == null || activities.isEmpty()) return result;

        for (SeckillActivity activity : activities) {
            // 查询该活动下的商品
            List<ScSkuActivity> scList = scSkuActivityDao.queryByActivityId(activity.getActivityId());
            if (scList == null || scList.isEmpty()) continue;

            Discount discount = discountDao.queryByDiscountId(activity.getDiscountId());
            BigDecimal deduction = calculateDeduction(discount);

            for (ScSkuActivity sc : scList) {
                Sku sku = skuDao.queryByGoodsId(sc.getGoodsId());
                if (sku == null) continue;

                result.add(SeckillGoodsEntity.builder()
                        .goodsId(sku.getGoodsId())
                        .goodsName(sku.getGoodsName())
                        .goodsImageUrl(sku.getGoodsImageUrl())
                        .originalPrice(sku.getOriginalPrice())
                        .payPrice(sku.getOriginalPrice().subtract(deduction))
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
                        .remainCount(activity.getRemainCount())
                        .build());
            }
        }
        return result;
    }

    @Override
    public List<SeckillActivityWithGoodsVO> querySeckillActivitiesWithGoods() {
        List<SeckillActivity> activities = seckillActivityDao.queryEffectiveActivities();
        List<SeckillActivityWithGoodsVO> result = new ArrayList<>();
        if (activities == null || activities.isEmpty()) return result;

        for (SeckillActivity activity : activities) {
            List<ScSkuActivity> scList = scSkuActivityDao.queryByActivityId(activity.getActivityId());
            if (scList == null || scList.isEmpty()) continue;

            List<SeckillActivityWithGoodsVO.GoodsItem> goodsItems = new ArrayList<>();
            for (ScSkuActivity sc : scList) {
                Sku sku = skuDao.queryByGoodsId(sc.getGoodsId());
                String goodsName = sku != null ? sku.getGoodsName() : sc.getGoodsId();
                goodsItems.add(SeckillActivityWithGoodsVO.GoodsItem.builder()
                        .goodsId(sc.getGoodsId())
                        .goodsName(goodsName)
                        .build());
            }

            result.add(SeckillActivityWithGoodsVO.builder()
                    .activityId(activity.getActivityId())
                    .activityName(activity.getActivityName())
                    .remainCount(activity.getRemainCount())
                    .goodsList(goodsItems)
                    .build());
        }
        return result;
    }

    private BigDecimal calculateDeduction(Discount discount) {
        if (discount == null || discount.getMarketExpr() == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(discount.getMarketExpr());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public boolean deductStock(Long activityId) {
        int rows = seckillActivityDao.deductStock(activityId);
        return rows > 0;
    }

    @Override
    public boolean recoverStock(Long activityId) {
        int rows = seckillActivityDao.recoverStock(activityId);
        if (rows > 0) {
            log.info("恢复秒杀库存成功 activityId:{}", activityId);
            return true;
        }
        return false;
    }

    @Override
    public boolean downgradeSwitch() {
        // 暂无 DCC 配置，默认不降级
        return false;
    }

}

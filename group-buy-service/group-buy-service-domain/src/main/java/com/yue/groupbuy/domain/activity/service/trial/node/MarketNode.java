package com.yue.groupbuy.domain.activity.service.trial.node;

import com.yue.groupbuy.domain.activity.model.entity.MarketProductEntity;
import com.yue.groupbuy.domain.activity.model.entity.TrialBalanceEntity;
import com.yue.groupbuy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.yue.groupbuy.domain.activity.model.valobj.SCSkuActivityVO;
import com.yue.groupbuy.domain.activity.model.valobj.SkuVO;
import com.yue.groupbuy.domain.activity.service.discount.IDiscountCalculateService;
import com.yue.groupbuy.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import com.yue.groupbuy.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class MarketNode extends AbstractGroupBuyMarketSupport {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private Map<String, IDiscountCalculateService> discountCalculateServiceMap;
    @Resource
    private ErrorNode errorNode;
    @Resource
    private TagNode tagNode;

    @Override
    protected void multiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<GroupBuyActivityDiscountVO> groupBuyActivityDiscountVOCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Long availableActivityId = requestParameter.getActivityId();
            if (null == availableActivityId) {
                SCSkuActivityVO scSkuActivityVO = repository.querySCSkuActivityBySCGoodsId(requestParameter.getSource(), requestParameter.getChannel(), requestParameter.getGoodsId());
                if (null == scSkuActivityVO) return null;
                availableActivityId = scSkuActivityVO.getActivityId();
            }
            return repository.queryGroupBuyActivityDiscountVO(availableActivityId);
        }, threadPoolExecutor);

        CompletableFuture<SkuVO> skuVOCompletableFuture = CompletableFuture.supplyAsync(() ->
                repository.querySkuByGoodsId(requestParameter.getGoodsId()), threadPoolExecutor);

        CompletableFuture.allOf(groupBuyActivityDiscountVOCompletableFuture, skuVOCompletableFuture)
                .get(timeout, TimeUnit.MILLISECONDS);

        dynamicContext.setGroupBuyActivityDiscountVO(groupBuyActivityDiscountVOCompletableFuture.join());
        dynamicContext.setSkuVO(skuVOCompletableFuture.join());

        log.info("拼团商品查询试算服务-MarketNode userId:{} 异步线程加载数据「GroupBuyActivityDiscountVO、SkuVO」完成", requestParameter.getUserId());
    }

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品查询试算服务-MarketNode userId:{} requestParameter:{}", requestParameter.getUserId(), JSON.toJSONString(requestParameter));

        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();
        if (null == groupBuyActivityDiscountVO) {
            return router(requestParameter, dynamicContext);
        }

        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount = groupBuyActivityDiscountVO.getGroupBuyDiscount();
        SkuVO skuVO = dynamicContext.getSkuVO();
        if (null == groupBuyDiscount || null == skuVO) {
            return router(requestParameter, dynamicContext);
        }

        IDiscountCalculateService discountCalculateService = discountCalculateServiceMap.get(groupBuyDiscount.getMarketPlan());
        if (null == discountCalculateService) {
            log.info("不存在{}类型的折扣计算服务，支持类型为:{}", groupBuyDiscount.getMarketPlan(), JSON.toJSONString(discountCalculateServiceMap.keySet()));
            throw new AppException(ResponseCode.E0001.getCode(), ResponseCode.E0001.getInfo());
        }

        BigDecimal payPrice = discountCalculateService.calculate(requestParameter.getUserId(), skuVO.getOriginalPrice(), groupBuyDiscount);
        dynamicContext.setDeductionPrice(skuVO.getOriginalPrice().subtract(payPrice));
        dynamicContext.setPayPrice(payPrice);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        if (null == dynamicContext.getGroupBuyActivityDiscountVO() || null == dynamicContext.getSkuVO() || null == dynamicContext.getDeductionPrice()) {
            return errorNode;
        }
        return tagNode;
    }

}

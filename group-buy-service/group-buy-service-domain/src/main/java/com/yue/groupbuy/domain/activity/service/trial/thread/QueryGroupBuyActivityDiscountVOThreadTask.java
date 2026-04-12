package com.yue.groupbuy.domain.activity.service.trial.thread;

import com.yue.groupbuy.domain.activity.adapter.repository.IActivityRepository;
import com.yue.groupbuy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.yue.groupbuy.domain.activity.model.valobj.SCSkuActivityVO;

import java.util.concurrent.Callable;

public class QueryGroupBuyActivityDiscountVOThreadTask implements Callable<GroupBuyActivityDiscountVO> {

    private final Long activityId;
    private final String source;
    private final String channel;
    private final String goodsId;
    private final IActivityRepository activityRepository;

    public QueryGroupBuyActivityDiscountVOThreadTask(Long activityId, String source, String channel, String goodsId, IActivityRepository activityRepository) {
        this.activityId = activityId;
        this.source = source;
        this.channel = channel;
        this.goodsId = goodsId;
        this.activityRepository = activityRepository;
    }

    @Override
    public GroupBuyActivityDiscountVO call() throws Exception {
        Long availableActivityId = activityId;
        if (null == activityId) {
            SCSkuActivityVO scSkuActivityVO = activityRepository.querySCSkuActivityBySCGoodsId(source, channel, goodsId);
            if (null == scSkuActivityVO) return null;
            availableActivityId = scSkuActivityVO.getActivityId();
        }
        return activityRepository.queryGroupBuyActivityDiscountVO(availableActivityId);
    }

}

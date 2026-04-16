package com.yue.groupbuy.domain.activity.service.trial;

import com.yue.groupbuy.domain.activity.adapter.repository.IActivityRepository;
import com.yue.groupbuy.domain.activity.model.entity.MarketProductEntity;
import com.yue.groupbuy.domain.activity.model.entity.TrialBalanceEntity;
import com.yue.groupbuy.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;

import jakarta.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractGroupBuyMarketSupport extends AbstractMultiThreadStrategyRouter<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    protected long timeout = 5000;

    @Resource
    protected IActivityRepository repository;

    @Override
    protected void multiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 缺省的方法
    }

}

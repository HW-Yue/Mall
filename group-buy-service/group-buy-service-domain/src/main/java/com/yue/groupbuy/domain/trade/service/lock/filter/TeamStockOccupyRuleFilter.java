package com.yue.groupbuy.domain.trade.service.lock.filter;

import com.yue.groupbuy.domain.trade.adapter.repository.ITradeRepository;
import com.yue.groupbuy.domain.trade.model.entity.GroupBuyActivityEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.yue.groupbuy.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class TeamStockOccupyRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-组队库存校验 userId:{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        String teamId = requestParameter.getTeamId();
        if (StringUtils.isBlank(teamId)) {
            return TradeLockRuleFilterBackEntity.builder()
                    .userTakeOrderCount(dynamicContext.getUserTakeOrderCount())
                    .build();
        }

        GroupBuyActivityEntity groupBuyActivity = dynamicContext.getGroupBuyActivity();
        Integer target = groupBuyActivity.getTarget();
        Integer validTime = groupBuyActivity.getValidTime();
        String teamStockKey = dynamicContext.generateTeamStockKey(teamId);
        String recoveryTeamStockKey = dynamicContext.generateRecoveryTeamStockKey(teamId);

        boolean status = repository.occupyTeamStock(teamStockKey, recoveryTeamStockKey, target, validTime);
        if (!status) {
            log.warn("交易规则过滤-组队库存抢占失败 userId:{} activityId:{} key:{}", requestParameter.getUserId(), requestParameter.getActivityId(), teamStockKey);
            throw new AppException(ResponseCode.E0008);
        }

        return TradeLockRuleFilterBackEntity.builder()
                .userTakeOrderCount(dynamicContext.getUserTakeOrderCount())
                .recoveryTeamStockKey(recoveryTeamStockKey)
                .build();
    }

}

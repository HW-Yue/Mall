package com.yue.groupbuy.domain.trade.service.lock.filter;

import com.yue.groupbuy.domain.trade.adapter.repository.ITradeRepository;
import com.yue.groupbuy.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.yue.groupbuy.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class UserTakeLimitRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-用户参与次数限制校验 userId:{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        Integer userTakeOrderCount = repository.queryOrderCountByGoodsId(requestParameter.getGoodsId(), requestParameter.getUserId());
        Integer takeLimitCount = dynamicContext.getGroupBuyActivity().getTakeLimitCount();

        if (userTakeOrderCount >= takeLimitCount) {
            log.info("用户参与次数限制校验，超过参团次数限制 userId:{} count:{} limit:{}", requestParameter.getUserId(), userTakeOrderCount, takeLimitCount);
            throw new AppException(ResponseCode.E0103.getCode(), ResponseCode.E0103.getInfo());
        }

        dynamicContext.setUserTakeOrderCount(userTakeOrderCount);

        return next(requestParameter, dynamicContext);
    }

}

package com.yue.groupbuy.domain.trade.service.refund;

import com.yue.groupbuy.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeRefundCommandEntity;
import com.yue.groupbuy.domain.trade.model.valobj.RefundTypeEnumVO;
import com.yue.groupbuy.domain.trade.model.valobj.TeamRefundSuccess;
import com.yue.groupbuy.domain.trade.service.ITradeRefundOrderService;
import com.yue.groupbuy.domain.trade.service.refund.business.IRefundOrderStrategy;
import com.yue.groupbuy.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Map;

@Slf4j
@Service
public class TradeRefundOrderService implements ITradeRefundOrderService {

    private final Map<String, IRefundOrderStrategy> refundOrderStrategyMap;

    public TradeRefundOrderService(Map<String, IRefundOrderStrategy> refundOrderStrategyMap) {
        this.refundOrderStrategyMap = refundOrderStrategyMap;
    }

    @Resource
    private BusinessLinkedList<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> tradeRefundRuleFilter;

    @Override
    public TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception {
        log.info("逆向流程，退单操作 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
        return tradeRefundRuleFilter.apply(tradeRefundCommandEntity, new TradeRefundRuleFilterFactory.DynamicContext());
    }

    @Override
    public void restoreTeamLockStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        log.info("逆向流程，恢复锁单量 userId:{} activityId:{} teamId:{}", teamRefundSuccess.getUserId(), teamRefundSuccess.getActivityId(), teamRefundSuccess.getTeamId());
        String type = teamRefundSuccess.getType();
        RefundTypeEnumVO refundTypeEnumVO = RefundTypeEnumVO.getRefundTypeEnumVOByCode(type);
        IRefundOrderStrategy refundOrderStrategy = refundOrderStrategyMap.get(refundTypeEnumVO.getStrategy());
        refundOrderStrategy.reverseStock(teamRefundSuccess);
    }

}

package com.yue.groupbuy.domain.trade.service.refund.filter;

import com.yue.groupbuy.domain.trade.model.entity.MarketPayOrderEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeRefundCommandEntity;
import com.yue.groupbuy.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.yue.groupbuy.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UniqueRefundNodeFilter implements ILogicHandler<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.RefundLinkContext, TradeRefundBehaviorEntity> {

    @Override
    public TradeRefundBehaviorEntity apply(TradeRefundCommandEntity tradeRefundCommandEntity, TradeRefundRuleFilterFactory.RefundLinkContext dynamicContext) throws Exception {
        log.info("逆向流程-退单操作，重复退单检查 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());

        MarketPayOrderEntity marketPayOrderEntity = dynamicContext.getMarketPayOrderEntity();
        TradeOrderStatusEnumVO tradeOrderStatusEnumVO = marketPayOrderEntity.getTradeOrderStatusEnumVO();

        if (TradeOrderStatusEnumVO.CLOSED.equals(tradeOrderStatusEnumVO)) {
            log.info("逆向流程，退单操作(幂等-重复退单) userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
            return stop(
                    tradeRefundCommandEntity,
                    dynamicContext,
                    TradeRefundBehaviorEntity.builder()
                            .userId(tradeRefundCommandEntity.getUserId())
                            .orderId(marketPayOrderEntity.getOrderId())
                            .teamId(marketPayOrderEntity.getTeamId())
                            .tradeRefundBehaviorEnum(TradeRefundBehaviorEntity.TradeRefundBehaviorEnum.REPEAT)
                            .build());
        }

        return next(tradeRefundCommandEntity, dynamicContext);
    }

}

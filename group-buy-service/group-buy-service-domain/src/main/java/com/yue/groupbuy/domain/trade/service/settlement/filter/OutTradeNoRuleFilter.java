package com.yue.groupbuy.domain.trade.service.settlement.filter;

import com.yue.groupbuy.domain.trade.adapter.repository.ITradeRepository;
import com.yue.groupbuy.domain.trade.model.entity.MarketPayOrderEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.yue.groupbuy.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.yue.groupbuy.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.yue.groupbuy.types.enums.ResponseCode;
import com.yue.groupbuy.types.exception.AppException;
import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class OutTradeNoRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.SettlementLinkContext, TradeSettlementRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.SettlementLinkContext dynamicContext) throws Exception {
        log.info("结算规则过滤-外部单号校验 userId:{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        MarketPayOrderEntity marketPayOrderEntity = repository.queryMarketPayOrderEntityByOutTradeNo(requestParameter.getUserId(), requestParameter.getOutTradeNo());

        if (null == marketPayOrderEntity || TradeOrderStatusEnumVO.CLOSE.equals(marketPayOrderEntity.getTradeOrderStatusEnumVO())) {
            log.error("不存在的外部交易单号或用户已退单 userId:{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());
            throw new AppException(ResponseCode.E0104);
        }

        dynamicContext.setMarketPayOrderEntity(marketPayOrderEntity);

        return next(requestParameter, dynamicContext);
    }

}

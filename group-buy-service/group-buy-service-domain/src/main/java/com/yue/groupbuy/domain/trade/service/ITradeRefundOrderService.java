package com.yue.groupbuy.domain.trade.service;

import com.yue.groupbuy.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeRefundCommandEntity;
import com.yue.groupbuy.domain.trade.model.valobj.TeamRefundSuccess;

public interface ITradeRefundOrderService {

    TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception;

    void restoreTeamLockStock(TeamRefundSuccess teamRefundSuccess) throws Exception;

}

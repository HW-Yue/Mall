package com.yue.groupbuy.domain.trade.service.refund.business;

import com.yue.groupbuy.domain.trade.model.entity.TradeRefundOrderEntity;
import com.yue.groupbuy.domain.trade.model.valobj.TeamRefundSuccess;

public interface IRefundOrderStrategy {

    void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) throws Exception;

    void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception;

}

package com.yue.groupbuy.domain.trade.service;

import com.yue.groupbuy.domain.trade.model.entity.MarketPayOrderEntity;
import com.yue.groupbuy.domain.trade.model.entity.PayActivityEntity;
import com.yue.groupbuy.domain.trade.model.entity.PayDiscountEntity;
import com.yue.groupbuy.domain.trade.model.entity.UserEntity;
import com.yue.groupbuy.domain.trade.model.valobj.GroupBuyProgressVO;

public interface ITradeLockOrderService {

    MarketPayOrderEntity queryNoPayMarketPayOrderByOrderId(String userId, String orderId);

    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) throws Exception;

}

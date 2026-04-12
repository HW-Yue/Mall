package com.yue.groupbuy.domain.trade.service;

import com.yue.groupbuy.domain.trade.model.entity.LockOrderCommand;
import com.yue.groupbuy.domain.trade.model.entity.MarketPayOrderEntity;
import com.yue.groupbuy.domain.trade.model.entity.SettlementCommand;

public interface IGroupBuyDomainService {

    /**
     * 创建拼团订单（锁单）
     * @return MarketPayOrderEntity 含 orderId、teamId、价格信息
     */
    MarketPayOrderEntity createGroupBuyOrder(LockOrderCommand command);

    /**
     * 支付结算（消费 order-paid-group_buy MQ 消息）
     */
    void settlementGroupBuyOrder(SettlementCommand command);

    /**
     * 拼团退款
     */
    void refundGroupBuyOrder(String userId, String orderId);
}

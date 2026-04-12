package com.yue.groupbuy.domain.trade.adapter.port;

import java.util.List;
import java.util.Map;

public interface IGroupBuyEventPublisher {

    /**
     * 拼团完成，通知 agent 下发 token
     * @param teamId 队伍ID
     * @param completedOrders 已完成订单列表（含 orderId, userId, goodsId, payPrice）
     */
    void publishGroupBuySuccess(String teamId, List<Map<String, Object>> completedOrders);
}

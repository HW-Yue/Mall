package com.yue.groupbuy.domain.trade.service.refund.business.impl;

import com.yue.groupbuy.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.yue.groupbuy.domain.trade.model.entity.NotifyTaskEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeRefundOrderEntity;
import com.yue.groupbuy.domain.trade.model.valobj.TeamRefundSuccess;
import com.yue.groupbuy.domain.trade.service.refund.business.AbstractRefundOrderStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("unpaid2RefundStrategy")
public class Unpaid2RefundStrategy extends AbstractRefundOrderStrategy {

    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) throws Exception {
        log.info("退单；未支付，未成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());

        NotifyTaskEntity notifyTaskEntity = repository.unpaid2Refund(GroupBuyRefundAggregate.buildUnpaid2RefundAggregate(tradeRefundOrderEntity, -1));

        sendRefundNotifyMessage(notifyTaskEntity, "未支付，未成团");
    }

    @Override
    public void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        doReverseStock(teamRefundSuccess, "未支付，未成团，恢复锁单库存");
    }

}

package com.yue.groupbuy.domain.trade.service.refund.business.impl;

import com.yue.groupbuy.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.yue.groupbuy.domain.trade.model.entity.NotifyTaskEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradeRefundOrderEntity;
import com.yue.groupbuy.domain.trade.model.valobj.TeamRefundSuccess;
import com.yue.groupbuy.domain.trade.service.refund.business.AbstractRefundOrderStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("paid2RefundStrategy")
public class Paid2RefundStrategy extends AbstractRefundOrderStrategy {

    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) throws Exception {
        log.info("退单；已支付，未成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());

        NotifyTaskEntity notifyTaskEntity = repository.paid2Refund(GroupBuyRefundAggregate.buildPaid2RefundAggregate(tradeRefundOrderEntity, -1, -1));

        sendRefundNotifyMessage(notifyTaskEntity, "已支付，未成团");
    }

    @Override
    public void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        doReverseStock(teamRefundSuccess, "已支付，未成团，恢复锁单库存");
    }

}

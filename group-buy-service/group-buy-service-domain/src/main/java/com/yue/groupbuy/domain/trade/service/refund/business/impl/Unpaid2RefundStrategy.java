package com.yue.groupbuy.domain.trade.service.refund.business.impl;

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
        log.info("退单；未支付，未成团，发 order-close-group-buy MQ userId:{} teamId:{} orderId:{} outTradeNo:{}",
                tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(),
                tradeRefundOrderEntity.getOrderId(), tradeRefundOrderEntity.getOutTradeNo());
        // 发 MQ：由 OrderCloseGroupBuyListener 统一原子执行 t_order 关单 + team_order.lock_count -1
        groupBuyRefundMqProducer.sendOrderCloseMessage(
                tradeRefundOrderEntity.getOutTradeNo(), tradeRefundOrderEntity.getUserId());
    }

    @Override
    public void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        doReverseStock(teamRefundSuccess, "未支付，未成团，恢复锁单库存");
    }

}

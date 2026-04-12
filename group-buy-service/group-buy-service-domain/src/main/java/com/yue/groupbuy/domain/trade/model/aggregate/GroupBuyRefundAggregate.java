package com.yue.groupbuy.domain.trade.model.aggregate;

import com.yue.groupbuy.domain.trade.model.entity.TradeRefundOrderEntity;
import com.yue.groupbuy.domain.trade.model.valobj.GroupBuyProgressVO;
import com.yue.groupbuy.types.enums.GroupBuyOrderEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyRefundAggregate {

    /** 交易退单 */
    private TradeRefundOrderEntity tradeRefundOrderEntity;
    /** 退单进度 */
    private GroupBuyProgressVO groupBuyProgress;
    /** 拼团枚举 */
    private GroupBuyOrderEnumVO groupBuyOrderEnumVO;

    public static GroupBuyRefundAggregate buildUnpaid2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity, Integer lockCount) {
        GroupBuyRefundAggregate agg = new GroupBuyRefundAggregate();
        agg.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        agg.setGroupBuyProgress(GroupBuyProgressVO.builder().lockCount(lockCount).build());
        return agg;
    }

    public static GroupBuyRefundAggregate buildPaid2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                     Integer lockCount, Integer completeCount) {
        GroupBuyRefundAggregate agg = new GroupBuyRefundAggregate();
        agg.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        agg.setGroupBuyProgress(GroupBuyProgressVO.builder().lockCount(lockCount).completeCount(completeCount).build());
        return agg;
    }

    public static GroupBuyRefundAggregate buildPaidTeam2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                         Integer lockCount, Integer completeCount,
                                                                         GroupBuyOrderEnumVO groupBuyOrderEnumVO) {
        GroupBuyRefundAggregate agg = new GroupBuyRefundAggregate();
        agg.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        agg.setGroupBuyProgress(GroupBuyProgressVO.builder().lockCount(lockCount).completeCount(completeCount).build());
        agg.setGroupBuyOrderEnumVO(groupBuyOrderEnumVO);
        return agg;
    }

}

package com.yue.groupbuy.domain.trade.model.aggregate;

import com.yue.groupbuy.domain.trade.model.entity.GroupBuyTeamEntity;
import com.yue.groupbuy.domain.trade.model.entity.TradePaySuccessEntity;
import com.yue.groupbuy.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyTeamSettlementAggregate {

    /** 用户实体 */
    private UserEntity userEntity;
    /** 组队实体 */
    private GroupBuyTeamEntity groupBuyTeamEntity;
    /** 支付成功实体 */
    private TradePaySuccessEntity tradePaySuccessEntity;

}

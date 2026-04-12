package com.yue.groupbuy.domain.trade.model.aggregate;

import com.yue.groupbuy.domain.trade.model.entity.PayActivityEntity;
import com.yue.groupbuy.domain.trade.model.entity.PayDiscountEntity;
import com.yue.groupbuy.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrderAggregate {

    /** 用户实体 */
    private UserEntity userEntity;
    /** 支付活动实体 */
    private PayActivityEntity payActivityEntity;
    /** 支付优惠实体 */
    private PayDiscountEntity payDiscountEntity;
    /** 已参与拼团量 */
    private Integer userTakeOrderCount;

}

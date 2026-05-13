package com.yue.groupbuy.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团个人订单实体（用于超时退款场景）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamOrderEntity {

    private String orderId;
    private String userId;
    private Integer status;

}

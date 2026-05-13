package com.yue.groupbuy.domain.trade.model.entity;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class SettlementCommand {

    private String userId;
    private String orderId;
    private Date outTradeTime;
}

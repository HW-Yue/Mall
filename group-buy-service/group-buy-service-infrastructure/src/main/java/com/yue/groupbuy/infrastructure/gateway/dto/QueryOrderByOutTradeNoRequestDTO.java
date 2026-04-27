package com.yue.groupbuy.infrastructure.gateway.dto;

import lombok.Data;

@Data
public class QueryOrderByOutTradeNoRequestDTO {

    private String userId;
    private String outTradeNo;
}

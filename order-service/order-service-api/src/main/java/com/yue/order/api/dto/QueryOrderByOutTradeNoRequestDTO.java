package com.yue.order.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class QueryOrderByOutTradeNoRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;
    private String outTradeNo;
}

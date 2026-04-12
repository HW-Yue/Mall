package com.yue.groupbuy.infrastructure.gateway.dto;

import lombok.Data;

@Data
public class GatewayResponse<T> {

    private String code;
    private String info;
    private T data;
}

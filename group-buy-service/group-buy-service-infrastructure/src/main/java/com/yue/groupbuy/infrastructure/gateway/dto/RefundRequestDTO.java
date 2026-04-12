package com.yue.groupbuy.infrastructure.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundRequestDTO {

    private String userId;
    private String orderId;
}

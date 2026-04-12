package com.yue.groupbuy.infrastructure.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamSuccessNotifyMessage {

    private String teamId;
    private List<OrderItem> orders;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItem {
        private String orderId;
        private String userId;
        private String goodsType;
        private String resKey;
        private String resValue;
        private BigDecimal amount;
    }
}

package com.yue.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryUserOrderListResponseDTO {

    private List<UserOrderItemDTO> orderList;
    private Boolean hasMore;
    /** 下一页游标，传给下次请求的 lastId */
    private Long lastId;
}

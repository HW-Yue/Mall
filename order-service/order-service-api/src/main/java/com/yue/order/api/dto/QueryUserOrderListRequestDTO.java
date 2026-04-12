package com.yue.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryUserOrderListRequestDTO {

    private String userId;
    /** 游标分页：传上一页最后一条 id；首次传 null */
    private Long lastId;
    @Builder.Default
    private Integer pageSize = 10;
}

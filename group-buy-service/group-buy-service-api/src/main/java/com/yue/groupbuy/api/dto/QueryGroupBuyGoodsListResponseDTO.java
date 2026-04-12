package com.yue.groupbuy.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 查询拼团商品列表响应
 */
@Data
@Builder
public class QueryGroupBuyGoodsListResponseDTO {

    private List<GroupBuyGoodsDTO> groupBuyGoodsList;

}

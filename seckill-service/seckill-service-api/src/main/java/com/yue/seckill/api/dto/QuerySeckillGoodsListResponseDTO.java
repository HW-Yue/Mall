package com.yue.seckill.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 查询秒杀商品列表响应
 */
@Data
@Builder
public class QuerySeckillGoodsListResponseDTO {

    private List<SeckillGoodsDTO> seckillGoodsList;

}

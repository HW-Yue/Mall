package com.yue.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 商品分页查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsPageResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总条数 */
    private Long total;
    /** 当前页数据 */
    private List<GoodsItemDTO> list;
}

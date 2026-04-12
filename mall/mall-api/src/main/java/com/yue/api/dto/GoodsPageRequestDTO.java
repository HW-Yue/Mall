package com.yue.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品分页查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsPageRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品类型ID（类目ID），可选，不传则查全部 */
    private Integer categoryId;
    /** 页码，从 1 开始 */
    private Integer pageNum;
    /** 每页条数 */
    private Integer pageSize;
}

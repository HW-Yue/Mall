package com.yue.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品类型（类目）项，用于列表返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTypeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 类型ID */
    private Integer id;
    /** 类型名称 */
    private String name;
    /** 图标URL */
    private String iconUrl;
    /** 排序权重 */
    private Integer sortOrder;
}

package com.yue.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 商品类目
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    /** 自增ID */
    private Integer id;
    /** 类目名称 */
    private String name;
    /** 类目编码 */
    private String code;
    /** 分类图标 */
    private String iconUrl;
    /** 排序权重 */
    private Integer sortOrder;
    /** 是否启用 */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}

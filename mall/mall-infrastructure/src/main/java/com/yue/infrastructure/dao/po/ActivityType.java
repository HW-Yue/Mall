package com.yue.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 活动类型字典
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityType {

    /** 自增ID */
    private Integer id;
    /** 活动名称 (如：秒杀、拼团) */
    private String typeName;
    /** 活动编码 (如：seckill, group_buy) */
    private String typeCode;
    /** 状态 (0禁用 1启用) */
    private Integer status;
    /** 创建时间 */
    private Date createTime;
}

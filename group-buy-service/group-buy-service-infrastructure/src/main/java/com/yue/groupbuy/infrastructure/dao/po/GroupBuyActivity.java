package com.yue.groupbuy.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyActivity {

    private Long activityId;
    private String activityName;
    private String discountId;
    private Integer groupType;
    private Integer takeLimitCount;
    private Integer target;
    private Integer validTime;
    /** 0-创建、1-生效、2-过期、3-废弃 */
    private Integer status;
    private Date startTime;
    private Date endTime;
    /** 人群标签规则标识 */
    private String tagId;
    /** 人群标签规则范围 */
    private String tagScope;
}

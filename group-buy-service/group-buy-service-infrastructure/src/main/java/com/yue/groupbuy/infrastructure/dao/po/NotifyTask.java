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
public class NotifyTask {

    private Long id;
    private Long activityId;
    private String teamId;
    private String notifyCategory;
    private String notifyType;
    private String notifyMQ;
    private String notifyUrl;
    private Integer notifyCount;
    /** 0初始、1完成、2重试、3失败 */
    private Integer notifyStatus;
    private String parameterJson;
    private String uuid;
    private Date createTime;
    private Date updateTime;

}

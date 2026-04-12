package com.yue.groupbuy.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyTaskEntity {

    /** 组队ID */
    private String teamId;
    /** 通知类型 */
    private String notifyType;
    /** MQ Topic */
    private String notifyMQ;
    /** HTTP 回调URL */
    private String notifyUrl;
    /** 已通知次数 */
    private Integer notifyCount;
    /** 参数JSON */
    private String parameterJson;
    /** 唯一ID */
    private String uuid;

    public String lockKey() {
        return "notify_task_lock_" + uuid;
    }

}

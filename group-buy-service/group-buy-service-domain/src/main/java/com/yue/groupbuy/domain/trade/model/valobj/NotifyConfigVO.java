package com.yue.groupbuy.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyConfigVO {

    /** 通知类型（HTTP/MQ） */
    private String notifyType;
    /** MQ Topic */
    private String notifyMQ;
    /** HTTP 回调URL */
    private String notifyUrl;

}

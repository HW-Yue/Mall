package com.yue.groupbuy.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotifyTypeEnumVO {

    HTTP("HTTP", "HTTP回调"),
    MQ("MQ", "MQ消息"),
    ;

    private final String code;
    private final String info;

}

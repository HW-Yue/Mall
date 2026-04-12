package com.permissionsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data // 自动生成 getter, setter, toString, equals, hashCode
public class LogDTO {

    private Long id;

    /**
     * 操作人ID或用户名
     */
    private String username;

    /**
     * 操作类型 (e.g., login, create, update, delete)
     */
    private String operationType;

    /**
     * 具体操作的描述
     */
    private String method;

    /**
     * 用户IP地址
     */
    private String ip;

    /**
     * 用户代理 (浏览器/操作系统信息)
     */
    private String userAgent;

    /**
     * 操作时间
     */
    private LocalDateTime createDate;

    private Long costTime;//操作耗时


}

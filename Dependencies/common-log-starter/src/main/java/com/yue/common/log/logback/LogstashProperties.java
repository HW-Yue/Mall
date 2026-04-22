package com.yue.common.log.logback;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Logstash 上报配置属性
 *
 * <p>在 application.yml 中配置：
 * <pre>
 * logstash:
 *   enabled: true
 *   host: 127.0.0.1
 *   port: 4560
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "logstash")
public class LogstashProperties {

    /** 是否启用 Logstash 上报，默认关闭 */
    private boolean enabled = false;

    /** Logstash TCP 地址 */
    private String host = "127.0.0.1";

    /** Logstash TCP 端口 */
    private int port = 4560;
}

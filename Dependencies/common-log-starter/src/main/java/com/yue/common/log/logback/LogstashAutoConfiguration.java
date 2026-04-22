package com.yue.common.log.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Logstash 自动装配
 *
 * <p>触发条件：
 * <ol>
 *   <li>classpath 上存在 {@code logstash-logback-encoder}（引入了该依赖）</li>
 *   <li>{@code yue.log.logstash.enabled=true}（显式开启）</li>
 * </ol>
 *
 * <p>满足条件后自动向 Root Logger 注册 {@link LogstashTcpSocketAppender}，
 * 无需在各服务的 logback-spring.xml 中手动 include。
 */
@Slf4j
@Configuration
@ConditionalOnClass(LogstashTcpSocketAppender.class)
@ConditionalOnProperty(prefix = "logstash", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(LogstashProperties.class)
public class LogstashAutoConfiguration {

    private final LogstashProperties properties;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    public LogstashAutoConfiguration(LogstashProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void addLogstashAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setCustomFields("{\"service\":\"" + applicationName + "\"}");
        encoder.setContext(context);
        encoder.start();

        LogstashTcpSocketAppender appender = new LogstashTcpSocketAppender();
        appender.setName("LOGSTASH");
        appender.setContext(context);
        appender.addDestination(properties.getHost() + ":" + properties.getPort());
        appender.setEncoder(encoder);
        appender.start();

        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);

        log.info("[CommonLogStarter] Logstash appender 已启用 - {}:{}, service={}",
                properties.getHost(), properties.getPort(), applicationName);
    }
}

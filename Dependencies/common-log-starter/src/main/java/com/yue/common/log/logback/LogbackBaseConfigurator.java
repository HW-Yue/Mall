package com.yue.common.log.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.LoggerFactory;

/**
 * Logback 基础配置器
 *
 * <p>如果用户不想使用 xml 配置，可以通过代码方式快速配置。</p>
 *
 * <p>使用方式（在应用启动时调用）：</p>
 * <pre>
 * LogbackBaseConfigurator.configureDefaults();
 * </pre>
 */
public class LogbackBaseConfigurator {

    /**
     * 基础日志格式（带 trace-id）
     */
    public static final String DEFAULT_PATTERN =
            "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} -%X{trace-id} - %msg%n";

    /**
     * 简洁日志格式
     */
    public static final String SIMPLE_PATTERN =
            "%d{HH:mm:ss.SSS} [%thread] %-5level -%X{trace-id} - %msg%n";

    /**
     * 配置默认控制台输出（带 trace-id）
     */
    public static void configureDefaults() {
        configureDefaults(DEFAULT_PATTERN, Level.INFO);
    }

    /**
     * 配置默认控制台输出（自定义格式和级别）
     *
     * @param pattern 日志格式
     * @param level   日志级别
     */
    public static void configureDefaults(String pattern, Level level) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        // 创建 PatternLayout
        PatternLayout layout = new PatternLayout();
        layout.setPattern(pattern);
        layout.setContext(context);
        layout.start();

        // 创建 ConsoleAppender
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setName("CONSOLE");
        consoleAppender.setContext(context);
        consoleAppender.setLayout(layout);
        consoleAppender.start();

        // 获取 Root Logger 并配置
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAndStopAllAppenders();
        rootLogger.addAppender(consoleAppender);
        rootLogger.setLevel(level);
    }

}

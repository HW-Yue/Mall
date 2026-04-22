package com.yue.common.log.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Nacos 日志冲突解决处理器
 *
 * <p>在 Spring Environment 准备阶段提前处理 Nacos 相关日志配置：
 * <ul>
 *     <li>禁用 Nacos 默认 logback 配置，避免与 common-log-starter 冲突</li>
 *     <li>将 Nacos 内部日志重定向到项目目录，避免占用用户 home 目录</li>
 * </ul>
 *
 * <p>处理时机：EnvironmentPostProcessor 在 Spring 应用启动早期执行，
 * 确保在 Nacos 客户端初始化前完成所有配置。
 *
 * @author yue
 */
@Slf4j
public class NacosLoggingEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /**
     * 禁用 Nacos 默认日志配置的 System Property Key
     */
    private static final String NACOS_LOGGING_DISABLED_KEY = "nacos.logging.default.config.enabled";

    /**
     * Nacos 内部日志路径配置 Key（JM.LOG.PATH 是 Nacos 客户端内部使用的）
     */
    private static final String NACOS_JM_LOG_PATH_KEY = "JM.LOG.PATH";

    /**
     * 默认 Nacos 日志子目录
     */
    private static final String DEFAULT_NACOS_LOG_DIR = "data/logs/nacos";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 1. 禁用 Nacos 默认日志配置（避免 logback.xml 冲突）
        disableNacosDefaultLogging();

        // 2. 设置 Nacos 日志输出路径（避免写入 user.home）
        configureNacosLogPath();
    }

    /**
     * 禁用 Nacos 默认日志配置
     */
    private void disableNacosDefaultLogging() {
        String existingValue = System.getProperty(NACOS_LOGGING_DISABLED_KEY);
        if (existingValue == null) {
            System.setProperty(NACOS_LOGGING_DISABLED_KEY, "false");
            log.debug("[CommonLogStarter] 已自动禁用 Nacos 默认日志配置，避免 logback 冲突");
        } else {
            log.debug("[CommonLogStarter] Nacos 日志配置已被显式设置为: {}", existingValue);
        }
    }

    /**
     * 配置 Nacos 日志输出路径
     */
    private void configureNacosLogPath() {
        String existingPath = System.getProperty(NACOS_JM_LOG_PATH_KEY);
        if (existingPath != null) {
            log.debug("[CommonLogStarter] Nacos 日志路径已被显式设置为: {}", existingPath);
            return;
        }

        try {
            // 优先从配置读取自定义路径，否则使用默认路径
            String customPath = System.getProperty("yue.log.nacos.path");
            Path nacosLogDir;

            if (customPath != null && !customPath.isEmpty()) {
                nacosLogDir = Paths.get(customPath).toAbsolutePath().normalize();
            } else {
                // 默认路径：${user.dir}/data/logs/nacos
                String userDir = System.getProperty("user.dir", ".");
                nacosLogDir = Paths.get(userDir).resolve(DEFAULT_NACOS_LOG_DIR).toAbsolutePath().normalize();
            }

            System.setProperty(NACOS_JM_LOG_PATH_KEY, nacosLogDir.toString());
            log.debug("[CommonLogStarter] 已设置 Nacos 日志路径: {}", nacosLogDir);
        } catch (Exception e) {
            // 路径设置失败不影响主流程，使用 Nacos 默认行为
            log.warn("[CommonLogStarter] 设置 Nacos 日志路径失败，使用默认配置: {}", e.getMessage());
        }
    }
}

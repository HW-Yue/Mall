package com.yue.common.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import jakarta.annotation.PostConstruct;

/**
 * 全链路日志自动配置主类
 *
 * <p>功能：
 * <ul>
 *     <li>加载配置属性 {@link CommonLogProperties}</li>
 *     <li>根据环境自动启用 Servlet / WebFlux / Feign 配置</li>
 *     <li>初始化常量</li>
 * </ul>
 *
 * <p>使用方式：添加依赖后无需任何代码修改，自动生效。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CommonLogProperties.class)
@ConditionalOnProperty(prefix = "yue.log.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
        // Servlet 配置（条件加载）
        com.yue.common.log.servlet.TraceIdServletConfiguration.class,
        // WebFlux 配置（条件加载）
        com.yue.common.log.webflux.TraceIdWebFluxConfiguration.class,
        // Feign 配置（条件加载）
        com.yue.common.log.feign.TraceIdFeignConfiguration.class,
        // 线程池装饰器配置
        MdcTaskDecoratorAutoConfiguration.class
})
public class CommonLogAutoConfiguration {

    @Autowired
    private CommonLogProperties properties;

    @PostConstruct
    public void init() {
        // 初始化常量
        CommonLogConstants.init(properties);

        log.info("[CommonLogStarter] 全链路日志已启用 - headerName: {}, mdcKey: {}, responseHeader: {}, taskDecorator: {}",
                properties.getHeaderName(),
                properties.getMdcKey(),
                properties.isResponseHeader(),
                properties.isTaskDecorator());
    }

}

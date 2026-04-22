package com.yue.opsagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 允许 devops/ 下的前端以 file:// 协议直接访问本服务。
 *
 * <p>允许来源使用 {@code allowedOriginPatterns("*")}，这样既可以匹配常规的 http(s) Origin，
 * 也能匹配 file:// 打开 HTML 时浏览器发送的 {@code Origin: null}。不启用 credentials 以避开
 * Spring 对 {@code "*" + credentials} 的限制，SSE / fetch 在默认模式下也无需带 Cookie。</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type")
                .allowCredentials(false)
                .maxAge(3600);

        registry.addMapping("/agui/**")
                .allowedOriginPatterns("*")
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}

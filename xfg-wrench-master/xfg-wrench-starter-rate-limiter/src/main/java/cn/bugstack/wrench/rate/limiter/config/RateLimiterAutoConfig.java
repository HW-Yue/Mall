package cn.bugstack.wrench.rate.limiter.config;

import cn.bugstack.wrench.rate.limiter.aop.RateLimiterAOP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 限流配置
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * 2025-05-07 14:16
 */
@Configuration
public class RateLimiterAutoConfig {

    @Bean
    public RateLimiterAOP rateLimiterAOP() {
        return new RateLimiterAOP();
    }

}

package cn.bugstack.wrench.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

@Slf4j
@EnableAsync
@Configuration
// @EnableConfigurationProperties(ThreadPoolConfigProperties.class)
public class ThreadPoolConfig {

    @Bean
    @ConditionalOnMissingBean(ThreadPoolExecutor.class)
    public ThreadPoolExecutor threadPoolExecutor(/* ThreadPoolConfigProperties properties */) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        // 实例化策略
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        
        // 创建线程池
        return new ThreadPoolExecutor(
                20,  // corePoolSize
                50,  // maxPoolSize
                5000L,  // keepAliveTime
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5000),  // blockQueueSize
                Executors.defaultThreadFactory(),
                handler);
    }

}

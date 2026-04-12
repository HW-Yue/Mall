package com.yue.common.log;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

/**
 * MDC 任务装饰器
 * <p>
 * 用于 {@link ThreadPoolTaskExecutor}，确保提交到线程池的任务能继承父线程的 TraceId。
 * <p>
 * 使用方式（Spring 自动配置会自动注册）：
 * <pre>
 * @Bean
 * public ThreadPoolTaskExecutor taskExecutor() {
 *     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *     executor.setTaskDecorator(new MdcTaskDecorator());
 *     // ... 其他配置
 *     return executor;
 * }
 * </pre>
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 捕获父线程的 MDC 上下文
        Map<String, String> parentMdc = MDC.getCopyOfContextMap();

        return () -> {
            // 保存当前线程的原始 MDC
            Map<String, String> originalMdc = MDC.getCopyOfContextMap();

            try {
                // 设置父线程的 MDC 到当前线程
                if (parentMdc != null) {
                    MDC.setContextMap(parentMdc);
                }

                // 执行实际任务
                runnable.run();
            } finally {
                // 恢复当前线程的原始 MDC（清理或恢复）
                if (originalMdc != null) {
                    MDC.setContextMap(originalMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

}

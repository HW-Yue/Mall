package com.yue.common.log;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * MdcTaskDecorator 自动配置
 *
 * <p>自动为所有 ThreadPoolTaskExecutor Bean 添加 MdcTaskDecorator，
 * 确保异步线程池中的任务也能继承父线程的 TraceId。
 *
 * <p>可以通过配置关闭：
 * <pre>
 * yue.log.trace.task-decorator: false
 * </pre>
 */
@Configuration
@ConditionalOnClass(ThreadPoolTaskExecutor.class)
@ConditionalOnProperty(prefix = "yue.log.trace", name = "task-decorator", havingValue = "true", matchIfMissing = true)
public class MdcTaskDecoratorAutoConfiguration {

    /**
     * Bean 后置处理器，为 ThreadPoolTaskExecutor 自动添加装饰器
     */
    @Configuration
    public static class MdcTaskDecoratorBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof ThreadPoolTaskExecutor) {
                ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) bean;
                // 使用反射检查是否已设置 TaskDecorator（Spring Boot 2.7 没有直接的 getter）
                try {
                    java.lang.reflect.Method getTaskDecorator = ThreadPoolTaskExecutor.class.getMethod("getTaskDecorator");
                    Object existingDecorator = getTaskDecorator.invoke(executor);
                    if (existingDecorator == null) {
                        executor.setTaskDecorator(new MdcTaskDecorator());
                    }
                } catch (NoSuchMethodException e) {
                    // Spring Boot 2.7 及以下版本没有 getTaskDecorator 方法，直接设置
                    executor.setTaskDecorator(new MdcTaskDecorator());
                } catch (Exception e) {
                    // 其他异常，忽略
                }
            }
            return bean;
        }
    }

}

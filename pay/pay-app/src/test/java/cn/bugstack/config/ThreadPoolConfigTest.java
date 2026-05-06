package cn.bugstack.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;

import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadPoolConfigTest {

    @Test
    void runtimeYamlBindsAndBuildsThreadPoolExecutor() throws Exception {
        ThreadPoolConfigProperties properties = bindRuntimeProperties("nacos/pay-service-runtime-dev.yml");
        printProperties("before-build", properties);

        ThreadPoolExecutor executor = new ThreadPoolConfig().threadPoolExecutor(properties);
        printExecutor("after-build", executor);

        assertThat(properties.getCorePoolSize()).isEqualTo(20);
        assertThat(properties.getMaxPoolSize()).isEqualTo(50);
        assertThat(properties.getKeepAliveTime()).isEqualTo(5000L);
        assertThat(properties.getBlockQueueSize()).isEqualTo(5000);
        assertThat(properties.getPolicy()).isEqualTo("CallerRunsPolicy");

        assertThat(executor.getCorePoolSize()).isEqualTo(20);
        assertThat(executor.getMaximumPoolSize()).isEqualTo(50);
        assertThat(executor.getKeepAliveTime(java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(5000L);
        assertThat(((LinkedBlockingQueue<?>) executor.getQueue()).remainingCapacity()).isEqualTo(5000);
        assertThat(executor.getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
    }

    @Test
    void unknownPolicyFallsBackToAbortPolicy() throws Exception {
        ThreadPoolConfigProperties properties = new ThreadPoolConfigProperties();
        properties.setPolicy("UnknownPolicy");

        ThreadPoolExecutor executor = new ThreadPoolConfig().threadPoolExecutor(properties);

        assertThat(executor.getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
    }

    private static ThreadPoolConfigProperties bindRuntimeProperties(String path) {
        Properties properties = loadYaml(path);
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new PropertiesPropertySource("yaml", properties));
        return Binder.get(environment)
                .bind("thread.pool.executor.config", Bindable.of(ThreadPoolConfigProperties.class))
                .orElseThrow(IllegalStateException::new);
    }

    private static Properties loadYaml(String path) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(path));
        return factory.getObject();
    }

    private static void printProperties(String phase, ThreadPoolConfigProperties properties) {
        System.out.printf(
                "[ThreadPoolConfigTest-pay] phase=%s core=%d max=%d keepAlive=%d queue=%d policy=%s%n",
                phase,
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getKeepAliveTime(),
                properties.getBlockQueueSize(),
                properties.getPolicy());
    }

    private static void printExecutor(String phase, ThreadPoolExecutor executor) {
        System.out.printf(
                "[ThreadPoolConfigTest-pay] phase=%s core=%d max=%d keepAlive=%d queueRemaining=%d reject=%s%n",
                phase,
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getKeepAliveTime(java.util.concurrent.TimeUnit.SECONDS),
                executor.getQueue().remainingCapacity(),
                executor.getRejectedExecutionHandler().getClass().getSimpleName());
    }
}

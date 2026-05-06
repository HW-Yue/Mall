package com.yue.seckill.config;

import org.apache.tomcat.util.threads.TaskQueue;
import org.dromara.dynamictp.common.entity.DtpExecutorProps;
import org.dromara.dynamictp.common.entity.TpExecutorProps;
import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.dromara.dynamictp.core.support.ExecutorAdapter;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.dromara.dynamictp.starter.adapter.webserver.tomcat.TomcatDtpAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicTpRefreshTest {

    @Test
    void shouldRefreshBusinessExecutorAndTomcatExecutor() {
        clearDtpRegistry();
        initDtpRegistryProperties();
        initApplicationContext();

        DtpExecutor executor = new DtpExecutor(20, 100, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        executor.setThreadPoolName("threadPoolExecutor");
        executor.setThreadPoolAliasName("seckill-business");
        DtpRegistry.registerExecutor(ExecutorWrapper.of(executor), "test");

        printDtpState("before-dtp-refresh", executor);
        DtpExecutorProps executorProps = new DtpExecutorProps();
        executorProps.setThreadPoolName("threadPoolExecutor");
        executorProps.setCorePoolSize(30);
        executorProps.setMaximumPoolSize(120);
        executorProps.setKeepAliveTime(120);
        executorProps.setUnit(TimeUnit.SECONDS);
        DtpRegistry.refresh(executorProps);
        printDtpState("after-dtp-refresh", executor);

        assertEquals(30, executor.getCorePoolSize());
        assertEquals(120, executor.getMaximumPoolSize());
        assertEquals(120, executor.getKeepAliveTime(TimeUnit.SECONDS));

        org.apache.tomcat.util.threads.ThreadPoolExecutor tomcatExecutor =
                new org.apache.tomcat.util.threads.ThreadPoolExecutor(
                        100, 1000, 60, TimeUnit.SECONDS, new TaskQueue(100));
        TomcatDtpAdapter adapter = new TomcatDtpAdapter();
        adapter.getExecutorWrappers().put("tomcatTp", new ExecutorWrapper("tomcatTp", createTomcatAdapter(tomcatExecutor)));

        DtpProperties properties = DtpProperties.getInstance();
        TpExecutorProps tomcatTp = new TpExecutorProps();
        tomcatTp.setThreadPoolName("tomcatTp");
        tomcatTp.setCorePoolSize(120);
        tomcatTp.setMaximumPoolSize(1200);
        tomcatTp.setKeepAliveTime(90);
        tomcatTp.setUnit(TimeUnit.SECONDS);
        properties.setTomcatTp(tomcatTp);

        printTomcatState("before-tomcat-refresh", tomcatExecutor);
        adapter.refresh(properties);
        printTomcatState("after-tomcat-refresh", tomcatExecutor);

        assertEquals(120, tomcatExecutor.getCorePoolSize());
        assertEquals(1200, tomcatExecutor.getMaximumPoolSize());
        assertEquals(90, tomcatExecutor.getKeepAliveTime(TimeUnit.SECONDS));
    }

    @SuppressWarnings("unchecked")
    private static void clearDtpRegistry() {
        Field field = ReflectionUtils.findField(DtpRegistry.class, "EXECUTOR_REGISTRY");
        ReflectionUtils.makeAccessible(field);
        Map<String, ?> registry = (Map<String, ?>) ReflectionUtils.getField(field, null);
        if (registry != null) {
            registry.clear();
        }
    }

    private static void initDtpRegistryProperties() {
        DtpProperties properties = DtpProperties.getInstance();
        properties.setPlatforms(Collections.emptyList());
        properties.setExecutors(Collections.emptyList());
        new DtpRegistry(properties);
    }

    private static void initApplicationContext() {
        new org.dromara.dynamictp.common.spring.ApplicationContextHolder()
                .setApplicationContext(new StaticApplicationContext());
    }

    private static void printDtpState(String phase, DtpExecutor executor) {
        System.out.printf("[DynamicTpRefreshTest-seckill] phase=%s dtpCore=%d dtpMax=%d dtpKeepAlive=%d%n",
                phase, executor.getCorePoolSize(), executor.getMaximumPoolSize(),
                executor.getKeepAliveTime(TimeUnit.SECONDS));
    }

    private static void printTomcatState(String phase, org.apache.tomcat.util.threads.ThreadPoolExecutor executor) {
        System.out.printf("[DynamicTpRefreshTest-seckill] phase=%s tomcatCore=%d tomcatMax=%d tomcatKeepAlive=%d%n",
                phase, executor.getCorePoolSize(), executor.getMaximumPoolSize(),
                executor.getKeepAliveTime(TimeUnit.SECONDS));
    }

    private static ExecutorAdapter<java.util.concurrent.Executor> createTomcatAdapter(
            org.apache.tomcat.util.threads.ThreadPoolExecutor executor) {
        return new ExecutorAdapter<>() {
            @Override
            public java.util.concurrent.Executor getOriginal() {
                return executor;
            }

            @Override
            public int getCorePoolSize() {
                return executor.getCorePoolSize();
            }

            @Override
            public void setCorePoolSize(int corePoolSize) {
                executor.setCorePoolSize(corePoolSize);
            }

            @Override
            public int getMaximumPoolSize() {
                return executor.getMaximumPoolSize();
            }

            @Override
            public void setMaximumPoolSize(int maximumPoolSize) {
                executor.setMaximumPoolSize(maximumPoolSize);
            }

            @Override
            public int getPoolSize() {
                return executor.getPoolSize();
            }

            @Override
            public int getActiveCount() {
                return executor.getActiveCount();
            }

            @Override
            public java.util.concurrent.BlockingQueue<Runnable> getQueue() {
                return executor.getQueue();
            }

            @Override
            public long getKeepAliveTime(TimeUnit unit) {
                return executor.getKeepAliveTime(unit);
            }

            @Override
            public void setKeepAliveTime(long time, TimeUnit unit) {
                executor.setKeepAliveTime(time, unit);
            }
        };
    }
}

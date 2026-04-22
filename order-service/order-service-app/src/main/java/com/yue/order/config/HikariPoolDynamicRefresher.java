package com.yue.order.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Nacos 等配置中心变更 {@code spring.datasource.hikari.*} 时，对已在运行的 Hikari 池应用可调参数。
 */
@Component
public class HikariPoolDynamicRefresher implements ApplicationListener<EnvironmentChangeEvent> {

    private final Environment environment;
    private final DataSource dataSource;

    public HikariPoolDynamicRefresher(Environment environment, DataSource dataSource) {
        this.environment = environment;
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        boolean touched = event.getKeys().stream().anyMatch(k -> k.startsWith("spring.datasource.hikari."));
        if (!touched || !(dataSource instanceof HikariDataSource)) {
            return;
        }
        HikariDataSource hk = (HikariDataSource) dataSource;
        Integer max = environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class);
        Integer min = environment.getProperty("spring.datasource.hikari.minimum-idle", Integer.class);
        if (max != null) {
            hk.setMaximumPoolSize(max);
        }
        if (min != null) {
            hk.setMinimumIdle(min);
        }
    }
}

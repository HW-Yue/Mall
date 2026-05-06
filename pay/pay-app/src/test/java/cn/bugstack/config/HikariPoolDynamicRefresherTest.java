package cn.bugstack.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.mock.env.MockEnvironment;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HikariPoolDynamicRefresherTest {

    @Test
    void shouldUpdateMaximumPoolSizeMinimumIdleAndConnectionTimeoutOnRefresh() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.hikari.maximum-pool-size", "20")
                .withProperty("spring.datasource.hikari.minimum-idle", "8")
                .withProperty("spring.datasource.hikari.connection-timeout", "45000");

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(5);
        dataSource.setConnectionTimeout(30000);

        HikariPoolDynamicRefresher refresher = new HikariPoolDynamicRefresher(environment, dataSource);

        printState("before-refresh", dataSource);
        refresher.onApplicationEvent(new EnvironmentChangeEvent(this, Set.of(
                "spring.datasource.hikari.maximum-pool-size",
                "spring.datasource.hikari.minimum-idle",
                "spring.datasource.hikari.connection-timeout"
        )));
        printState("after-refresh", dataSource);

        assertEquals(20, dataSource.getMaximumPoolSize());
        assertEquals(8, dataSource.getMinimumIdle());
        assertEquals(45000, dataSource.getConnectionTimeout());
    }

    private static void printState(String phase, HikariDataSource dataSource) {
        System.out.printf(
                "[HikariPoolDynamicRefresherTest-pay] phase=%s max=%d min=%d timeout=%d%n",
                phase,
                dataSource.getMaximumPoolSize(),
                dataSource.getMinimumIdle(),
                dataSource.getConnectionTimeout()
        );
    }
}

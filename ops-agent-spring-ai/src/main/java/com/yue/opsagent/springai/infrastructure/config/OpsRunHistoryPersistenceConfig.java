package com.yue.opsagent.springai.infrastructure.config;

import com.yue.opsagent.springai.service.JdbcOpsRunSummaryRepository;
import com.yue.opsagent.springai.service.OpsRunSummaryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class OpsRunHistoryPersistenceConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ops-ai.history", name = "enabled", havingValue = "true", matchIfMissing = true)
    DataSource opsRunHistoryDataSource(OpsAiProperties properties) {
        OpsAiProperties.History history = properties.getHistory();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(history.getJdbcUrl());
        dataSource.setUsername(history.getUsername());
        dataSource.setPassword(history.getPassword());
        return dataSource;
    }

    @Bean
    @ConditionalOnProperty(prefix = "ops-ai.history", name = "enabled", havingValue = "true", matchIfMissing = true)
    JdbcTemplate opsRunHistoryJdbcTemplate(DataSource opsRunHistoryDataSource) {
        return new JdbcTemplate(opsRunHistoryDataSource);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ops-ai.history", name = "enabled", havingValue = "true", matchIfMissing = true)
    OpsRunSummaryRepository jdbcOpsRunSummaryRepository(
            JdbcTemplate opsRunHistoryJdbcTemplate,
            OpsAiProperties properties) {
        return new JdbcOpsRunSummaryRepository(
                opsRunHistoryJdbcTemplate,
                properties.getHistory().getTableName());
    }

    @Bean
    @ConditionalOnProperty(prefix = "ops-ai.history", name = "enabled", havingValue = "false")
    OpsRunSummaryRepository noopOpsRunSummaryRepository() {
        return OpsRunSummaryRepository.noop();
    }
}

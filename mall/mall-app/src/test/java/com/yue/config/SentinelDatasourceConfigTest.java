package com.yue.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SentinelDatasourceConfigTest {

    @Test
    void devProfileDefinesFiveSentinelRuleDataSources() {
        assertStandardSentinelRuleSet("application-dev.yml");
    }

    private static void assertStandardSentinelRuleSet(String path) {
        Properties properties = load(path);

        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.flow.nacos.dataId"))
                .isEqualTo("${spring.application.name}-flow-rules.json");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.flow.nacos.rule-type"))
                .isEqualTo("flow");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.degrade.nacos.dataId"))
                .isEqualTo("${spring.application.name}-degrade-rules.json");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.degrade.nacos.rule-type"))
                .isEqualTo("degrade");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.param-flow.nacos.dataId"))
                .isEqualTo("${spring.application.name}-param-flow-rules.json");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.param-flow.nacos.rule-type"))
                .isEqualTo("param-flow");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.system.nacos.dataId"))
                .isEqualTo("${spring.application.name}-system-rules.json");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.system.nacos.rule-type"))
                .isEqualTo("system");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.authority.nacos.dataId"))
                .isEqualTo("${spring.application.name}-authority-rules.json");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.authority.nacos.rule-type"))
                .isEqualTo("authority");
    }

    private static Properties load(String path) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(path));
        return factory.getObject();
    }
}

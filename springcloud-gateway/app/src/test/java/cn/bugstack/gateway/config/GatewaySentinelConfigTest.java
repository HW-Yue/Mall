package cn.bugstack.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GatewaySentinelConfigTest {

    @Test
    void devProfileDefinesGatewaySentinelDatasourceAndFallback() {
        assertGatewaySentinel("application-dev.yml");
    }

    @Test
    void testProfileDefinesGatewaySentinelDatasourceAndFallback() {
        assertGatewaySentinel("application-test.yml");
    }

    private static void assertGatewaySentinel(String path) {
        Properties properties = load(path);

        assertThat(properties.getProperty("spring.cloud.sentinel.filter.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("spring.cloud.sentinel.scg.fallback.mode")).isEqualTo("response");
        assertThat(properties.getProperty("spring.cloud.sentinel.scg.fallback.response-status")).isEqualTo("429");
        assertThat(properties.getProperty("spring.cloud.sentinel.scg.fallback.response-body"))
                .contains("Blocked by Sentinel(gateway)");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.gw-flow.nacos.dataId"))
                .isEqualTo("${spring.application.name}-gw-flow-rules.json");
        assertThat(properties.getProperty("spring.cloud.sentinel.datasource.gw-flow.nacos.rule-type"))
                .isEqualTo("gw-flow");
    }

    private static Properties load(String path) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(path));
        return factory.getObject();
    }
}

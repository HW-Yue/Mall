package cn.bugstack.xfg.dev.tech.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

@Configuration
public class SentinelGatewayDataSourceConfig {

    @Bean("sentinel-json-gw-flow-converter")
    public Converter<String, List<GatewayFlowRule>> sentinelJsonGwFlowConverter(ObjectMapper objectMapper) {
        return source -> {
            try {
                return objectMapper.readValue(source, new TypeReference<List<GatewayFlowRule>>() {});
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse GatewayFlowRule list", e);
            }
        };
    }

}

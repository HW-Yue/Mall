package com.yue.opsagent.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.PropertyKeyConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * 基础设施配置：将 Nacos SDK 实例注册为 Spring Bean
 * 生命周期由 Spring 容器管理
 */
@Configuration
public class NacosAgentConfig {

    @Value("${nacos.server.addr:localhost:8848}")
    private String serverAddr;

    @Bean
    public ConfigService configService() throws NacosException {
        Properties props = new Properties();
        props.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
        return NacosFactory.createConfigService(props);
    }

    @Bean
    public NamingService namingService() throws NacosException {
        Properties props = new Properties();
        props.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
        return NacosFactory.createNamingService(props);
    }
}

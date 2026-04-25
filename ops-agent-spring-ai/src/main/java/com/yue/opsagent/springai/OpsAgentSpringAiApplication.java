package com.yue.opsagent.springai;

import com.yue.opsagent.springai.config.OpsAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(OpsAiProperties.class)
public class OpsAgentSpringAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpsAgentSpringAiApplication.class, args);
    }
}

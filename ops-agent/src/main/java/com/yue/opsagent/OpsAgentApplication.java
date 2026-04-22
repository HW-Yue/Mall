package com.yue.opsagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
@MapperScan("com.yue.opsagent.adapter.persistence")
public class OpsAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpsAgentApplication.class, args);
    }
}

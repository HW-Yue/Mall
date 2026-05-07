package com.yue;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.dromara.dynamictp.core.spring.EnableDynamicTp;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDynamicTp
@EnableDubbo
@SpringBootApplication
@Configurable
@EnableScheduling
@EnableDiscoveryClient
public class MallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
    }

}

package com.yue;

import org.dromara.dynamictp.core.spring.EnableDynamicTp;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDynamicTp
@SpringBootApplication
@Configurable
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.yue.infrastructure.gateway"})
public class MallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
    }

}

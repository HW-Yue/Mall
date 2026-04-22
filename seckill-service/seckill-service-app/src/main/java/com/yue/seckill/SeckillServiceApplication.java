package com.yue.seckill;

import org.dromara.dynamictp.core.spring.EnableDynamicTp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDynamicTp
@SpringBootApplication(scanBasePackages = "com.yue.seckill")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yue.seckill.infrastructure.gateway")
public class SeckillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillServiceApplication.class, args);
    }
}

package com.yue.seckill;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.dromara.dynamictp.core.spring.EnableDynamicTp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
@EnableDynamicTp
@EnableDubbo
@SpringBootApplication(scanBasePackages = "com.yue.seckill")
@EnableDiscoveryClient
public class SeckillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillServiceApplication.class, args);
    }
}

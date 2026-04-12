package com.yue.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.yue.seckill")
@EnableFeignClients(basePackages = "com.yue.seckill.infrastructure.gateway")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

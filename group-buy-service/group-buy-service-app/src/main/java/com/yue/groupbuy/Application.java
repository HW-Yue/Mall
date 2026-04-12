package com.yue.groupbuy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.yue.groupbuy")
@EnableFeignClients(basePackages = "com.yue.groupbuy.infrastructure.gateway")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

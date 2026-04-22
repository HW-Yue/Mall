package com.yue.groupbuy;

import org.dromara.dynamictp.core.spring.EnableDynamicTp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDynamicTp
@SpringBootApplication(scanBasePackages = "com.yue.groupbuy")
@EnableFeignClients(basePackages = "com.yue.groupbuy.infrastructure.gateway")
public class GroupBuyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroupBuyServiceApplication.class, args);
    }
}

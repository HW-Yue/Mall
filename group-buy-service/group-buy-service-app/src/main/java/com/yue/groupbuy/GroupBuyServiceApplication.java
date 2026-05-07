package com.yue.groupbuy;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.dromara.dynamictp.core.spring.EnableDynamicTp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@EnableDynamicTp
@EnableDubbo
@SpringBootApplication(scanBasePackages = "com.yue.groupbuy")
public class GroupBuyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroupBuyServiceApplication.class, args);
    }
}

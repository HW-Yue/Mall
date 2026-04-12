package com.permissionsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.permissionsystem.mapper")
@SpringBootApplication
public class PermissionSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(PermissionSystemApplication.class, args);
    }

}

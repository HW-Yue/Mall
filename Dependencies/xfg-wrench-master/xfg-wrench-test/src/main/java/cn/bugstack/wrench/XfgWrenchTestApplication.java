package cn.bugstack.wrench;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Configurable
@SpringBootApplication(scanBasePackages = {"cn.bugstack.wrench"})
public class XfgWrenchTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(XfgWrenchTestApplication.class, args);
    }

}

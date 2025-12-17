package com.demomodulish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class DemoModulishApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoModulishApplication.class, args);
    }

}

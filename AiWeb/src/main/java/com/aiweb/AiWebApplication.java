package com.aiweb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.aiweb.mapper")
public class AiWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiWebApplication.class, args);
    }
}

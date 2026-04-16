package com.marrylink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.marrylink.mapper")
public class MarrylinkAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarrylinkAdminApplication.class, args);
    }
}
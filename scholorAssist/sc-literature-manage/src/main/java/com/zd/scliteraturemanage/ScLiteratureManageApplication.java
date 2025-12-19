package com.zd.scliteraturemanage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.zd.scliteraturemanage"})
public class ScLiteratureManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScLiteratureManageApplication.class, args);
    }

}

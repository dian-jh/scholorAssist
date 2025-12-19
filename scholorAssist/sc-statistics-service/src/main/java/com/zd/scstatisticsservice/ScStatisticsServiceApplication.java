package com.zd.scstatisticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.zd.scstatisticsservice", "com.zd.sccommon"})
public class ScStatisticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScStatisticsServiceApplication.class, args);
    }

}

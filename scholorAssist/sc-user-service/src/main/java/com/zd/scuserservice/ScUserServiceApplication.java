package com.zd.scuserservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.zd.scuserservice", "com.zd.sccommon"})
@MapperScan(basePackages = "com.zd.scuserservice.mapper")
public class ScUserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScUserServiceApplication.class, args);
	}

}

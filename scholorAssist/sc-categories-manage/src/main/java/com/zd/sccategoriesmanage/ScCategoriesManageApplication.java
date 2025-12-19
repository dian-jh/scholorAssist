package com.zd.sccategoriesmanage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 分类管理服务启动类
 * 
 * @author system
 * @since 2024-01-01
 */
@SpringBootApplication(scanBasePackages = {"com.zd.sccategoriesmanage", "com.zd.sccommon"})
@EnableDiscoveryClient
@EnableTransactionManagement
@MapperScan("com.zd.sccategoriesmanage.mapper")
public class ScCategoriesManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScCategoriesManageApplication.class, args);
    }
}

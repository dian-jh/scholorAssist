package com.zd.sccommon.config;

import com.zd.sccommon.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花算法配置类
 * 
 * <p>配置雪花算法ID生成器的机器ID和数据中心ID</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Configuration
public class SnowflakeConfig {

    @Value("${snowflake.machine-id:1}")
    private long machineId;

    @Value("${snowflake.datacenter-id:1}")
    private long datacenterId;

    /**
     * 创建雪花算法ID生成器Bean
     * 
     * @return 雪花算法ID生成器实例
     */
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(machineId, datacenterId);
    }
}
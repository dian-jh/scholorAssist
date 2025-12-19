package com.zd.scapi.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.zd.sccommon.utils.UserContextHolder; // 依赖 sc-common
import com.zd.sccommon.model.UserContext;     // 依赖 sc-common
import feign.QueryMapEncoder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 默认Feign配置
 * 1. 自动扫描 sc-api 包下的 @FeignClient
 * 2. 注册 FeignRequestInterceptor 拦截器
 * 3. 注册 MyQueryMapEncoder 以支持GET请求发送复杂对象
 */
@Slf4j
@Configuration
@EnableFeignClients(basePackages = "com.zd.scapi.api")
public class DefaultFeignConfiguration {

    /**
     * 用户信息在Header中的Key
     */
    private static final String HEADER_USER_INFO = "USER_INFO";
    
    /**
     * 请求ID在Header中的Key (借鉴 sc-api/src/main/java/com/zd/scapi/filter/TraceFilter.java)
     */
    private static final String HEADER_TRACE_ID = "X-Trace-Id";


    /**
     * 注册 Feign 拦截器，用于在服务间调用时传递用户信息和链路ID
     */
    @Bean
    @ConditionalOnClass(UserContextHolder.class) // 确保 sc-common 里的 UserContextHolder 存在
    public RequestInterceptor feignRequestInterceptor(ObjectMapper objectMapper) {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                // 1. 传递用户信息 (从 sc-common 的 UserContextHolder 获取)
                try {
                    UserContext userContext = UserContextHolder.getContext();
                    if (userContext != null) {
                        String userInfoJson = objectMapper.writeValueAsString(userContext);
                        // 注意：jzo2o 使用了 Base64 编码，这里为简化直接使用 JSON 字符串
                        // 您可以根据需要像 jzo2o-common/utils/Base64Utils.java 一样进行编码
                        requestTemplate.header(HEADER_USER_INFO, userInfoJson);
                    }
                } catch (Exception e) {
                    log.error("Feign拦截器序列化用户信息失败", e);
                }

                // 2. 传递链路追踪ID (从 sc-api/util/TraceUtil.java 或 sc-common 的MDC获取)
                // 假设您的 TraceUtil 也会将 traceId 放入 MDC
                try {
                     // 假设您在 sc-common 中也有类似的 TraceUtil
                     // String traceId = com.zd.sccommon.utils.TraceUtil.getCurrentTraceId();
                     // 此处使用 sc-api 已有的 TraceUtil 作为参考
                     String traceId = com.zd.scapi.util.TraceUtil.getCurrentTraceId();
                     if (traceId != null) {
                        requestTemplate.header(HEADER_TRACE_ID, traceId);
                     }
                } catch (Exception e) {
                     log.warn("Feign拦截器获取TraceId失败", e);
                }
            }
        };
    }

    /**
     * 注册自定义的 QueryMapEncoder
     * 解决 Feign GET 请求 + Map/POJO 参数时，LocalDateTime 格式化问题
     * (借鉴 jzo2o-api/src/main/java/com/jzo2o/utils/MyQueryMapEncoder.java)
     */
    @Bean
    public QueryMapEncoder queryMapEncoder(ObjectMapper objectMapper) {
        return new MyQueryMapEncoder(objectMapper);
    }

    /**
     * 内部类：自定义 QueryMapEncoder
     */
    static class MyQueryMapEncoder implements QueryMapEncoder {
        private final ObjectMapper objectMapper;
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        public MyQueryMapEncoder(ObjectMapper objectMapper) {
            // 克隆一个
            this.objectMapper = objectMapper.copy();
            
            // 配置 LocalDateTime 序列化
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(FORMATTER));
            this.objectMapper.registerModule(javaTimeModule);
        }

        @Override
        public Map<String, Object> encode(Object o) {
            try {
                // 1. 先序列化为JSON
                String s = objectMapper.writeValueAsString(o);
                // 2. 再反序列化为Map
                return objectMapper.readValue(s, Map.class);
            } catch (JsonProcessingException e) {
                log.error("Feign QueryMapEncoder 序列化失败", e);
                throw new RuntimeException("Error encoding feign query map", e);
            }
        }
    }
}
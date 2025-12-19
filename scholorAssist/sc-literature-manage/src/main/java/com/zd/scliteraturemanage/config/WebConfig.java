package com.zd.scliteraturemanage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 静态资源配置
 *
 * <p>将本地 PDF 存储目录映射为可通过 URL 访问的静态资源路径，支持跨平台路径处理。</p>
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 文件存储根目录（默认 ./uploads）
     */
    @Value("${app.file.storage.root-path:./uploads}")
    private String rootPath;

    /**
     * 文档存储子目录（默认 documents）
     */
    @Value("${app.file.storage.document-path:documents}")
    private String documentPath;

    /**
     * 静态资源访问前缀（不包含通配符），默认 /files/
     */
    @Value("${app.file.storage.static-prefix:/files/}")
    private String staticPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 计算本地存储的绝对路径，跨平台兼容
        Path storageDir = Paths.get(rootPath, documentPath).toAbsolutePath().normalize();

        String resourceLocation = storageDir.toUri().toString(); // e.g. file:/D:/.../uploads/documents
        if (!resourceLocation.endsWith("/")) {
            resourceLocation = resourceLocation + "/";
        }

        // 规范化前缀并追加通配符
        String handlerPattern = staticPrefix.endsWith("/") ? staticPrefix + "**" : staticPrefix + "/**";

        log.info("注册静态资源映射：pattern={}, location={}", handlerPattern, resourceLocation);
        registry.addResourceHandler(handlerPattern)
                .addResourceLocations(resourceLocation);

        // 保留原有 /files/** 映射（如存在依赖），但改为跨平台计算目录
        String legacyPattern = "/files/**";
        log.info("注册兼容映射：pattern={}, location={}", legacyPattern, resourceLocation);
        registry.addResourceHandler(legacyPattern)
                .addResourceLocations(resourceLocation);
    }
}

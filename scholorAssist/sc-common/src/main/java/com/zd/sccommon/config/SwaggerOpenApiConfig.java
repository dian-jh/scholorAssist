package com.zd.sccommon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerOpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        String securitySchemeName = "Authorization";
        return new OpenAPI()
                .info(new Info()
                        .title("Scholor Assist API")
                        .description("学者助手接口文档（支持Swagger直连Token验证）")
                        .version("1.0"))
                // 定义 Header 类型的 SecurityScheme
                .schemaRequirement(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .description("在此输入JWT Token，例如：Bearer xxxxxx"))
                // 全局引用此安全项
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}

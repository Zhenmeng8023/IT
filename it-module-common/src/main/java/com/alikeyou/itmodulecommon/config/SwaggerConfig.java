package com.alikeyou.itmodulecommon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IT项目API文档")
                        .version("1.0")
                        .description("IT项目的API接口文档，包含用户、博客、互动等模块")
                        .termsOfService("http://localhost:18080/terms")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
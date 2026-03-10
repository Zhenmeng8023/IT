package com.alikeyou.itmodulelogin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)  // 禁用CORS，使用单独的CorsFilter
                .csrf(AbstractHttpConfigurer::disable)  // 禁用CSRF保护（开发环境可以这样做）
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()  // 允许所有请求，暂时用于测试
                );

        return http.build();
    }
}
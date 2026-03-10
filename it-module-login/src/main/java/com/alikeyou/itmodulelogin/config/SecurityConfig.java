package com.alikeyou.itmodulelogin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())  // 禁用CORS，使用单独的CorsFilter
                .csrf(csrf -> csrf.disable())  // 禁用CSRF保护（开发环境可以这样做）
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login").permitAll()  // 允许所有用户访问登录接口
                        .requestMatchers("/send-verify-code", "/register").permitAll()  // 允许所有用户访问发送验证码和注册接口
                        .requestMatchers("/verify-code").permitAll()  // 允许所有用户访问验证验证码接口
                        .requestMatchers("/register").permitAll()  // 允许所有用户访问注册接口
                        .anyRequest().authenticated()  // 其他所有请求都需要认证
                );

        return http.build();
    }
}
package com.alikeyou.itmodulelogin.service;

import com.alikeyou.itmodulelogin.dto.LoginRequest;
import com.alikeyou.itmodulelogin.dto.LoginResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LoginService {

    // 模拟用户验证，实际项目中应该从数据库查询
    public boolean validateUser(String username, String password) {
        // 这里只是简单模拟，实际项目中应该查询数据库并验证密码
        return "admin".equals(username) && "admin123".equals(password);
    }

    // 生成JWT token
    public String generateToken(String username) {
        // 实际项目中应该使用配置的密钥和过期时间
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24小时过期
                .signWith(SignatureAlgorithm.HS512, "secretKey") // 实际项目中应该使用配置的密钥
                .compact();
    }

    // 处理登录逻辑
    public LoginResponse login(LoginRequest request) {
        if (validateUser(request.getUsername(), request.getPassword())) {
            String token = generateToken(request.getUsername());
            return new LoginResponse(true, "登录成功", token);
        } else {
            return new LoginResponse(false, "用户名或密码错误");
        }
    }
}
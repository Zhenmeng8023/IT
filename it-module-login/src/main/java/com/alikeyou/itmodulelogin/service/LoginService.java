package com.alikeyou.itmodulelogin.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alikeyou.itmodulelogin.dto.LoginRequest;
import com.alikeyou.itmodulelogin.dto.LoginResponse;
import com.alikeyou.itmodulelogin.entity.LoginUser;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.alikeyou.itmodulelogin.utils.JwtUtil;
import com.alikeyou.itmodulelogin.utils.PasswordEncoder;

@Service
public class LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    private final UserRepository userRepository;

    public LoginService() {
        this.userRepository = new UserRepository();
    }

    // 验证用户
    public boolean validateUser(String username, String password) {
        Optional<LoginUser> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            LoginUser user = userOptional.get();
            boolean passwordMatch = PasswordEncoder.matches(password, user.getPassword());
            boolean enabled = user.isEnabled();
            return passwordMatch && enabled;
        } else {
            return false;
        }
    }

    // 处理登录逻辑
    public LoginResponse login(LoginRequest request) {
        if (validateUser(request.getUsername(), request.getPassword())) {
            String token = JwtUtil.generateToken(request.getUsername());
            return new LoginResponse(true, "登录成功", token);
        } else {
            return new LoginResponse(false, "用户名或密码错误");
        }
    }
}
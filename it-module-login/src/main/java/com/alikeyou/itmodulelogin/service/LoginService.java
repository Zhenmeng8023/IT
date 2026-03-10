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

/**
 * 登录服务类
 * 负责处理用户登录验证和token生成
 */
@Service
public class LoginService {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    // 用户信息仓库，用于从数据库中查询用户信息
    private final UserRepository userRepository;

    /**
     * 构造方法
     * 初始化用户信息仓库
     */
    public LoginService(UserRepository userRepository) {
        // 初始化用户信息仓库
        this.userRepository = userRepository;
    }

    /**
     * 验证用户
     * @param username 用户名
     * @param password 密码
     * @return 验证是否成功
     */
    public boolean validateUser(String username, String password) {
        // 根据用户名查询用户
        Optional<LoginUser> userOptional = userRepository.findByUsername(username);
        
        // 如果用户存在
        if (userOptional.isPresent()) {
            // 获取用户信息
            LoginUser user = userOptional.get();
            // 验证密码是否匹配
            boolean passwordMatch = PasswordEncoder.matches(password, user.getPasswordHash());
            // 验证用户是否启用
            boolean enabled = user.isEnabled();
            // 返回密码匹配且用户启用的结果
            return passwordMatch && enabled;
        } else {
            // 用户不存在，验证失败
            return false;
        }
    }

    /**
     * 处理登录逻辑
     * @param request 登录请求，包含用户名和密码
     * @return 登录响应，包含登录状态、消息和token
     */
    public LoginResponse login(LoginRequest request) {
        // 验证用户名和密码
        if (validateUser(request.getUsername(), request.getPassword())) {
            // 验证成功，生成JWT token
            String token = JwtUtil.generateToken(request.getUsername());
            // 返回登录成功的响应，包含token
            return new LoginResponse(true, "登录成功", token);
        } else {
            // 验证失败，返回登录失败的响应
            return new LoginResponse(false, "用户名或密码错误");
        }
    }
    

}
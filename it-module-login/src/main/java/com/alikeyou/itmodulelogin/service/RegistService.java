package com.alikeyou.itmodulelogin.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alikeyou.itmodulelogin.dto.LoginResponse;
import com.alikeyou.itmodulelogin.entity.LoginUser;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.alikeyou.itmodulelogin.utils.JwtUtil;
import com.alikeyou.itmodulelogin.utils.PasswordEncoder;

/**
 * 注册服务类
 * 负责处理用户注册逻辑
 */
@Service
public class RegistService {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(RegistService.class);

    // 用户信息仓库，用于从数据库中查询用户信息
    private final UserRepository userRepository;

    /**
     * 构造方法
     * 初始化用户信息仓库
     */
    public RegistService(UserRepository userRepository) {
        // 初始化用户信息仓库
        this.userRepository = userRepository;
    }

    /**
     * 处理注册逻辑
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @return 注册响应，包含注册状态和消息
     */
    public LoginResponse register(String username, String password, String email) {
        // 检查用户名是否已存在
        Optional<LoginUser> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            return new LoginResponse(false, "用户名已存在");
        }
        
        // 创建新用户
        LoginUser newUser = new LoginUser();
        newUser.setUsername(username);
        newUser.setPasswordHash(PasswordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setRoleId(4L); // 默认设置为用户角色（id=4）
        
        // 保存用户
        userRepository.save(newUser);
        
        // 生成token
        String token = JwtUtil.generateToken(username);
        
        // 返回注册成功的响应，包含token
        return new LoginResponse(true, "注册成功", token);
    }
}
package com.alikeyou.itmodulelogin.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.RoleRepository;
import com.alikeyou.itmodulelogin.dto.LoginResponse;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.alikeyou.itmodulecommon.utils.PasswordEncoder;

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

    // 角色仓库，用于查询角色信息
    @Autowired
    private RoleRepository roleRepository;

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
        Optional<UserInfo> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            return new LoginResponse(false, "用户名已存在");
        }
        
        // 创建新用户
        UserInfo newUser = new UserInfo();
        newUser.setUsername(username);
        newUser.setPasswordHash(PasswordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setRoleId(4); // 默认设置为用户角色（id=4）
        
        // 保存用户
        userRepository.save(newUser);
        
        return new LoginResponse(true, "注册成功", null, newUser.getRoleId());
    }
    
    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 是否更新成功
     */
    public boolean updatePassword(Long userId, String newPassword) {
        try {
            Optional<UserInfo> userOptional = userRepository.findById(userId);
            if (userOptional.isPresent()) {
                UserInfo user = userOptional.get();
                user.setPasswordHash(PasswordEncoder.encode(newPassword));
                userRepository.save(user);
                logger.info("用户密码更新成功: {}", userId);
                return true;
            }
            logger.warn("用户不存在: {}", userId);
            return false;
        } catch (Exception e) {
            logger.error("更新密码失败: {}", e.getMessage());
            return false;
        }
    }
}

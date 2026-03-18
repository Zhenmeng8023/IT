package com.alikeyou.itmodulelogin.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulelogin.dto.LoginRequest;
import com.alikeyou.itmodulelogin.dto.LoginResponse;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.alikeyou.itmodulelogin.utils.JwtUtil;
import com.alikeyou.itmodulecommon.utils.PasswordEncoder;

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
        Optional<UserInfo> userOptional = userRepository.findByUsername(username);
        
        // 如果用户存在
        if (userOptional.isPresent()) {
            // 获取用户信息
            UserInfo user = userOptional.get();
            // 验证密码是否匹配
            boolean passwordMatch = PasswordEncoder.matches(password, user.getPasswordHash());
            // 只验证密码匹配
            return passwordMatch;
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
            // 验证成功，查询用户完整信息
            Optional<UserInfo> userOptional = userRepository.findByUsername(request.getUsername());
            if (userOptional.isPresent()) {
                UserInfo user = userOptional.get();
                // 将用户信息保存到LoginConstant中
                com.alikeyou.itmodulecommon.constant.LoginConstant.setUsername(user.getUsername());
                com.alikeyou.itmodulecommon.constant.LoginConstant.setPassword(user.getPasswordHash());
                com.alikeyou.itmodulecommon.constant.LoginConstant.setEmail(user.getEmail());
                com.alikeyou.itmodulecommon.constant.LoginConstant.setUserId(user.getId());
                com.alikeyou.itmodulecommon.constant.LoginConstant.setRoleId(user.getRoleId());
                
                // 输出保存的用户信息
                logger.info("保存用户信息到LoginConstant: ");
                logger.info("Username: {}", com.alikeyou.itmodulecommon.constant.LoginConstant.getUsername());
                logger.info("Email: {}", com.alikeyou.itmodulecommon.constant.LoginConstant.getEmail());
                logger.info("UserId: {}", com.alikeyou.itmodulecommon.constant.LoginConstant.getUserId());
                logger.info("RoleId: {}", com.alikeyou.itmodulecommon.constant.LoginConstant.getRoleId());
            }
            // 生成JWT token
            String token = JwtUtil.generateToken(request.getUsername());
            // 获取用户角色id
            Integer roleId = null;
            if (userOptional.isPresent()) {
                roleId = userOptional.get().getRoleId();
            }
            // 返回登录成功的响应，包含token和roleId
            return new LoginResponse(true, "登录成功", token, roleId);
        } else {
            // 验证失败，返回登录失败的响应
            return new LoginResponse(false, "用户名或密码错误");
        }
    }
    

}
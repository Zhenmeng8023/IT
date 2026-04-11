package com.alikeyou.itmodulelogin.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.utils.PasswordEncoder;
import com.alikeyou.itmodulelogin.dto.LoginRequest;
import com.alikeyou.itmodulelogin.dto.LoginResponse;
import com.alikeyou.itmodulelogin.repository.UserRepository;

@Service
public class LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    private final UserRepository userRepository;

    public LoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean validateUser(String username, String password) {
        Optional<UserInfo> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            UserInfo user = userOptional.get();
            return PasswordEncoder.matches(password, user.getPasswordHash());
        }
        return false;
    }

    public Optional<UserInfo> authenticate(LoginRequest request) {
        Optional<UserInfo> userOptional = userRepository.findByUsername(request.getUsername());

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        UserInfo user = userOptional.get();
        boolean passwordMatch = PasswordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!passwordMatch) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    public LoginResponse login(LoginRequest request) {
        Optional<UserInfo> authenticated = authenticate(request);
        if (authenticated.isEmpty()) {
            return new LoginResponse(false, "用户名或密码错误");
        }

        UserInfo user = authenticated.get();

        com.alikeyou.itmodulecommon.constant.LoginConstant.setUsername(user.getUsername());
        com.alikeyou.itmodulecommon.constant.LoginConstant.setPassword(user.getPasswordHash());
        com.alikeyou.itmodulecommon.constant.LoginConstant.setEmail(user.getEmail());
        com.alikeyou.itmodulecommon.constant.LoginConstant.setUserId(user.getId());
        com.alikeyou.itmodulecommon.constant.LoginConstant.setRoleId(user.getRoleId());

        logger.info("保存用户信息到LoginConstant:");
        logger.info("Username: {}", com.alikeyou.itmodulecommon.constant.LoginConstant.getUsername());
        logger.info("Email: {}", com.alikeyou.itmodulecommon.constant.LoginConstant.getEmail());
        logger.info("UserId: {}", com.alikeyou.itmodulecommon.constant.LoginConstant.getUserId());
        logger.info("RoleId: {}", com.alikeyou.itmodulecommon.constant.LoginConstant.getRoleId());

        return new LoginResponse(true, "登录成功", null, user.getRoleId());
    }
}

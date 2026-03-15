package com.alikeyou.itmoduleuser.service;

import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmoduleuser.repository.UserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserInfoService {
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    // 根据ID获取用户信息
    public Optional<UserInfo> getUserById(Long id) {
        return userInfoRepository.findByIdWithAssociations(id);
    }
    
    // 根据用户名获取用户信息
    public Optional<UserInfo> getUserByUsername(String username) {
        return userInfoRepository.findByUsername(username);
    }
    
    // 根据邮箱获取用户信息
    public Optional<UserInfo> getUserByEmail(String email) {
        return userInfoRepository.findByEmail(email);
    }
    
    // 根据LoginConstant中存储的用户信息获取当前用户
    public Optional<UserInfo> getCurrentUser() {
        // 优先使用userId查询
        Long userId = LoginConstant.getUserId();
        if (userId != null) {
            return getUserById(userId);
        }
        
        // 其次使用username查询
        String username = LoginConstant.getUsername();
        if (username != null) {
            return getUserByUsername(username);
        }
        
        // 最后使用email查询
        String email = LoginConstant.getEmail();
        if (email != null) {
            return getUserByEmail(email);
        }
        
        return Optional.empty();
    }
    
    // 保存用户信息
    public UserInfo saveUser(UserInfo userInfo) {
        return userInfoRepository.save(userInfo);
    }
    
    // 更新用户信息
    public UserInfo updateUser(UserInfo userInfo) {
        return userInfoRepository.save(userInfo);
    }
    
    // 删除用户
    public void deleteUser(Long id) {
        userInfoRepository.deleteById(id);
    }
    
    // 修改密码
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isPresent()) {
            UserInfo user = userInfo.get();
            // 这里需要添加密码验证逻辑，暂时假设密码正确
            user.setPasswordHash(newPassword);
            userInfoRepository.save(user);
            return true;
        }
        return false;
    }
    
    // 修改邮箱
    public boolean changeEmail(Long userId, String newEmail) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isPresent()) {
            UserInfo user = userInfo.get();
            user.setEmail(newEmail);
            userInfoRepository.save(user);
            return true;
        }
        return false;
    }
    
    // 修改用户名
    public boolean changeUsername(Long userId, String newUsername) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isPresent()) {
            UserInfo user = userInfo.get();
            user.setUsername(newUsername);
            userInfoRepository.save(user);
            return true;
        }
        return false;
    }
    
    // 验证密码
    public boolean verifyPassword(Long userId, String password) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isPresent()) {
            UserInfo user = userInfo.get();
            // 这里需要添加密码验证逻辑，暂时假设密码正确
            return true;
        }
        return false;
    }
    
    // 绑定第三方账号
    public boolean bindThirdParty(Long userId, String thirdPartyType, String thirdPartyId) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isPresent()) {
            UserInfo user = userInfo.get();
            // 这里需要添加第三方账号绑定逻辑
            userInfoRepository.save(user);
            return true;
        }
        return false;
    }
    
    // 获取用户公开信息
    public Optional<UserInfo> getPublicUserInfo(Long userId) {
        return userInfoRepository.findByIdWithAssociations(userId);
    }
}
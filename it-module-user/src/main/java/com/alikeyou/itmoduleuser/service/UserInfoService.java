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
        return userInfoRepository.findById(id);
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
}
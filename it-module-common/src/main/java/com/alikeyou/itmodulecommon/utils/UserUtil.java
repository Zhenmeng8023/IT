package com.alikeyou.itmodulecommon.utils;

import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class UserUtil {
    
    /**
     * 获取当前登录用户
     */
    public static UserInfo getCurrentUser(Authentication authentication) {
        // 首先尝试从LoginConstant中获取用户信息
        Long userId = LoginConstant.getUserId();
        if (userId != null) {
            // 如果LoginConstant中有用户信息，创建一个UserInfo对象返回
            UserInfo userInfo = new UserInfo();
            userInfo.setId(userId);
            userInfo.setUsername(LoginConstant.getUsername());
            userInfo.setEmail(LoginConstant.getEmail());
            userInfo.setRoleId(LoginConstant.getRoleId());
            return userInfo;
        }
        
        // 如果LoginConstant中没有用户信息，从SecurityContext中获取
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserInfo) {
                return (UserInfo) principal;
            } else if (principal instanceof UserDetails) {
                // 如果是UserDetails类型，需要根据用户名或ID查询用户信息
                // 这里简化处理，实际项目中可能需要调用UserService
                throw new RuntimeException("UserDetails type not supported");
            }
        }
        throw new RuntimeException("User not authenticated");
    }
    
    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        // 首先尝试从LoginConstant中获取用户ID
        Long userId = LoginConstant.getUserId();
        if (userId != null) {
            return userId;
        }
        
        // 如果LoginConstant中没有用户ID，从SecurityContext中获取
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            UserInfo userInfo = getCurrentUser(authentication);
            return userInfo.getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}
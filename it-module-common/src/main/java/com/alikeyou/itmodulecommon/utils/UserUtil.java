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
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication != null) {
            UserInfo fromPrincipal = resolveUserInfo(authentication.getPrincipal());
            if (fromPrincipal != null) {
                return fromPrincipal;
            }

            UserInfo fromDetails = resolveUserInfo(authentication.getDetails());
            if (fromDetails != null) {
                return fromDetails;
            }

            UserInfo fromName = resolveUserInfo(authentication.getName());
            if (fromName != null) {
                return fromName;
            }

            if (authentication.getPrincipal() instanceof UserDetails) {
                throw new RuntimeException("UserDetails type not supported");
            }
        }

        Long userId = LoginConstant.getUserId();
        if (userId != null) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId(userId);
            userInfo.setUsername(LoginConstant.getUsername());
            userInfo.setEmail(LoginConstant.getEmail());
            userInfo.setRoleId(LoginConstant.getRoleId());
            return userInfo;
        }

        throw new RuntimeException("User not authenticated");
    }
    
    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            UserInfo userInfo = getCurrentUser(authentication);
            if (userInfo.getId() != null) {
                return userInfo.getId();
            }
        }

        Long userId = LoginConstant.getUserId();
        if (userId != null) {
            return userId;
        }

        throw new RuntimeException("User not authenticated");
    }

    private static UserInfo resolveUserInfo(Object source) {
        if (source == null) {
            return null;
        }

        if (source instanceof UserInfo userInfo) {
            return userInfo;
        }

        if (source instanceof Number number) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId(number.longValue());
            return userInfo;
        }

        if (source instanceof CharSequence text) {
            String value = text.toString().trim();
            if (value.isEmpty() || "anonymousUser".equalsIgnoreCase(value)) {
                return null;
            }

            UserInfo userInfo = new UserInfo();
            try {
                userInfo.setId(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                userInfo.setUsername(value);
            }
            return userInfo;
        }

        return null;
    }
}

package com.alikeyou.itmoduleproject.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CurrentUserProvider {

    public Long getCurrentUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("未从请求头 X-User-Id 中获取到当前用户ID，请先联调登录态");
        }
        return Long.parseLong(userId);
    }
}

package com.alikeyou.itmodulelogin.config;

import com.alikeyou.itmodulelogin.utils.JwtUtil;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtCurrentUserFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtCurrentUserFilter.class);

    private final UserInfoLiteRepository userInfoLiteRepository;

    public JwtCurrentUserFilter(UserInfoLiteRepository userInfoLiteRepository) {
        this.userInfoLiteRepository = userInfoLiteRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        try {
            if (StringUtils.hasText(token) && JwtUtil.validateToken(token)) {
                Long userId = JwtUtil.getUserIdFromToken(token);
                String username = JwtUtil.getUsernameFromToken(token);

                if (userId == null && StringUtils.hasText(username)) {
                    Optional<UserInfoLite> optional = userInfoLiteRepository.findByUsername(username);
                    if (optional.isPresent()) {
                        userId = optional.get().getId();
                    }
                }

                if (userId != null) {
                    request.setAttribute("currentUserId", userId);
                    request.setAttribute("userId", userId);
                    request.setAttribute("uid", userId);
                    request.setAttribute("currentUsername", username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    AuthorityUtils.NO_AUTHORITIES
                            );

                    authentication.setDetails(userId);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    logger.info("JwtCurrentUserFilter 认证成功, userId={}, username={}", userId, username);
                } else {
                    logger.warn("JwtCurrentUserFilter 未能解析出 userId, username={}", username);
                }
            }
        } catch (Exception e) {
            logger.error("JwtCurrentUserFilter 处理失败", e);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        String xToken = request.getHeader("X-Token");
        if (StringUtils.hasText(xToken)) {
            return xToken;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Admin-Token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
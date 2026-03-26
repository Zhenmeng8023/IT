package com.alikeyou.itmodulelogin.config;

import com.alikeyou.itmodulelogin.utils.JwtUtil;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

@Component
public class JwtCurrentUserFilter extends OncePerRequestFilter {

    private final UserInfoLiteRepository userInfoLiteRepository;

    public JwtCurrentUserFilter(UserInfoLiteRepository userInfoLiteRepository) {
        this.userInfoLiteRepository = userInfoLiteRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (StringUtils.hasText(token) && JwtUtil.validateToken(token)) {
                String username = JwtUtil.getUsernameFromToken(token);

                if (StringUtils.hasText(username)) {
                    Optional<UserInfoLite> optional = userInfoLiteRepository.findByUsername(username);

                    if (optional.isPresent()) {
                        Long userId = optional.get().getId();

                        request.setAttribute("currentUserId", userId);
                        request.setAttribute("userId", userId);
                        request.setAttribute("uid", userId);

                        Principal principal = () -> username;
                        request.setAttribute("principal", principal);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        AuthorityUtils.NO_AUTHORITIES
                                );

                        authentication.setDetails(userId);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
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
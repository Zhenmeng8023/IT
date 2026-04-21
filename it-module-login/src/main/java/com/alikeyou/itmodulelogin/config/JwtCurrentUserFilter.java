package com.alikeyou.itmodulelogin.config;

import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.entity.Role;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.RoleRepository;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class JwtCurrentUserFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtCurrentUserFilter.class);
    private static final String SESSION_USER_ID = LoginConstant.USER_ID;
    private static final String SESSION_USERNAME = LoginConstant.USER_NAME;
    private static final String SESSION_EMAIL = LoginConstant.EMAIL;
    private static final String SESSION_ROLE_ID = LoginConstant.ROLE_ID;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public JwtCurrentUserFilter(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            restoreFromSession(request);
        } catch (Exception e) {
            logger.error("JwtCurrentUserFilter 处理失败", e);
        } finally {
            try {
                filterChain.doFilter(request, response);
            } finally {
                LoginConstant.clearUserInfo();
                SecurityContextHolder.clearContext();
            }
        }
    }

    private boolean restoreFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        Long sessionUserId = toLong(session.getAttribute(SESSION_USER_ID));
        String sessionUsername = toText(session.getAttribute(SESSION_USERNAME));

        if (sessionUserId == null && !StringUtils.hasText(sessionUsername)) {
            return false;
        }

        UserInfo currentUser = resolveCurrentUser(sessionUserId, sessionUsername);
        if (currentUser == null || currentUser.getId() == null) {
            logger.warn("Session 中用户已失效, sessionId={}", session.getId());
            invalidateSessionQuietly(session);
            return false;
        }

        syncSessionSnapshot(session, currentUser);

        bindCurrentUser(
                request,
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getEmail(),
                currentUser.getRoleId()
        );
        logger.info("Session 认证成功, userId={}, username={}", currentUser.getId(), currentUser.getUsername());
        return true;
    }

    private UserInfo resolveCurrentUser(Long userId, String username) {
        UserInfo currentUser = null;

        if (userId == null && StringUtils.hasText(username)) {
            Optional<UserInfo> optional = userRepository.findByUsername(username);
            if (optional.isPresent()) {
                currentUser = optional.get();
            }
        }

        if (currentUser == null && userId != null) {
            currentUser = userRepository.findById(userId).orElse(null);
        }

        return currentUser;
    }

    private void syncSessionSnapshot(HttpSession session, UserInfo currentUser) {
        session.setAttribute(SESSION_USER_ID, currentUser.getId());
        session.setAttribute(SESSION_USERNAME, currentUser.getUsername());
        session.setAttribute(SESSION_EMAIL, currentUser.getEmail());
        session.setAttribute(SESSION_ROLE_ID, currentUser.getRoleId());
    }

    private void invalidateSessionQuietly(HttpSession session) {
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
        }
    }

    private void bindCurrentUser(HttpServletRequest request,
                                 Long userId,
                                 String username,
                                 String email,
                                 Integer roleId) {
        request.setAttribute("currentUserId", userId);
        request.setAttribute("userId", userId);
        request.setAttribute("uid", userId);
        request.setAttribute("currentUsername", username);

        Collection<? extends GrantedAuthority> authorities = resolveAuthorities(roleId);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities.isEmpty() ? AuthorityUtils.NO_AUTHORITIES : authorities
                );

        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginConstant.setUserId(userId);
        LoginConstant.setUsername(username);
        LoginConstant.setEmail(email);
        LoginConstant.setRoleId(roleId);
    }

    private Collection<? extends GrantedAuthority> resolveAuthorities(Integer roleId) {
        if (roleId == null) {
            return List.of();
        }

        Optional<Role> roleOptional = roleRepository.findWithMenusAndPermissionsById(roleId);
        if (roleOptional.isEmpty()) {
            return List.of();
        }

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (var menu : roleOptional.get().getMenus()) {
            if (menu == null || menu.getPermission() == null) {
                continue;
            }
            String permissionCode = menu.getPermission().getPermissionCode();
            if (!StringUtils.hasText(permissionCode)) {
                continue;
            }
            authorities.add(new SimpleGrantedAuthority(permissionCode.trim()));
        }
        return authorities;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String toText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}

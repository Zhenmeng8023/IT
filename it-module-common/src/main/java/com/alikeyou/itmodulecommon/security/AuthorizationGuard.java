package com.alikeyou.itmodulecommon.security;

import com.alikeyou.itmodulecommon.constant.LoginConstant;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authorizationGuard")
public class AuthorizationGuard {

    private static final String[] USER_ADMIN_AUTHORITIES = {
            "view:admin:user-info",
            "view:admin:user:info",
            "view:admin:user-account",
            "view:admin:user:account",
            "view:admin:user-role"
    };

    private static final String[] RBAC_AUTHORITIES = {
            "view:admin:user-role",
            "view:admin:rbac:role",
            "view:admin:rbac:menu",
            "view:admin:rbac:permission",
            "view:menu",
            "view:permission"
    };

    public boolean canManageUsers() {
        return hasAnyAuthority(USER_ADMIN_AUTHORITIES);
    }

    public boolean canManageRbac() {
        return hasAnyAuthority(RBAC_AUTHORITIES);
    }

    public boolean canAccessUser(Long targetUserId) {
        Long currentUserId = LoginConstant.getUserId();
        return currentUserId != null && currentUserId.equals(targetUserId) || canManageUsers();
    }

    public boolean canAccessAiLogUser(Long targetUserId) {
        return hasCurrentUser(targetUserId) || hasAnyAuthority("view:ai:log");
    }

    public boolean canAccessPromptTemplateOwner(Long ownerId) {
        return hasCurrentUser(ownerId) || hasAnyAuthority("view:ai:prompt-template");
    }

    private boolean hasCurrentUser(Long targetUserId) {
        Long currentUserId = LoginConstant.getUserId();
        return currentUserId != null && currentUserId.equals(targetUserId);
    }

    private boolean hasAnyAuthority(String... authorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (String authority : authorities) {
            boolean matched = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
            if (matched) {
                return true;
            }
        }
        return false;
    }
}

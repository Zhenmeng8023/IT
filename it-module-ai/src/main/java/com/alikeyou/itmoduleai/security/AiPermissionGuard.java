package com.alikeyou.itmoduleai.security;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component("aiPermissionGuard")
@RequiredArgsConstructor
public class AiPermissionGuard {

    private static final String LEGACY_KNOWLEDGE_PERMISSION = "view:knowledge-base";
    private static final String ADMIN_KNOWLEDGE_PERMISSION = "view:admin:ai:knowledge";
    private static final String FRONT_ASSISTANT_PERMISSION = "view:front:ai:assistant";
    private static final String FRONT_PERSONAL_KB_READ_PERMISSION = "view:front:ai:kb:self";
    private static final String FRONT_PERSONAL_KB_EDIT_PERMISSION = "edit:front:ai:kb:self";
    private static final String FRONT_PROJECT_KB_READ_PERMISSION = "view:front:ai:kb:project";
    private static final String FRONT_PROJECT_KB_EDIT_PERMISSION = "edit:front:ai:kb:project";
    private static final String FRONT_KB_MEMBER_MANAGE_PERMISSION = "manage:front:ai:kb:member";

    private final AiCurrentUserProvider currentUserProvider;

    public boolean canUseAssistant() {
        return currentUserProvider.resolveCurrentUserId() != null
                || hasFrontAssistantPermission()
                || hasAnyFrontKnowledgePermission()
                || canUseAdminKnowledgeDebug();
    }

    public boolean canReadFrontKnowledgeBase() {
        return hasFrontKnowledgeReadPermission()
                || canUseAdminKnowledgeDebug();
    }

    public boolean canReadMyFrontKnowledgeBase() {
        return hasPersonalKnowledgeReadPermission() || canUseAdminKnowledgeDebug();
    }

    public boolean canReadProjectFrontKnowledgeBase() {
        return hasProjectKnowledgeReadPermission() || canUseAdminKnowledgeDebug();
    }

    public boolean canEditFrontKnowledgeBase() {
        return hasFrontKnowledgeEditPermission() || canUseAdminKnowledgeDebug();
    }

    public boolean canEditPersonalFrontKnowledgeBase() {
        return hasPersonalKnowledgeEditPermission() || canUseAdminKnowledgeDebug();
    }

    public boolean canEditProjectFrontKnowledgeBase() {
        return hasProjectKnowledgeEditPermission() || canUseAdminKnowledgeDebug();
    }

    public boolean canManageFrontKnowledgeBaseMember() {
        return hasFrontKnowledgeMemberManagePermission() || canUseAdminKnowledgeDebug();
    }

    public boolean canUseAdminKnowledgeDebug() {
        return currentUserProvider.hasAuthority(ADMIN_KNOWLEDGE_PERMISSION);
    }

    public boolean canReadFrontKnowledgeBase(KnowledgeBase.ScopeType scopeType) {
        return switch (normalizeScope(scopeType)) {
            case PERSONAL -> canReadMyFrontKnowledgeBase();
            case PROJECT -> canReadProjectFrontKnowledgeBase();
            case PLATFORM -> false;
        };
    }

    public boolean canEditFrontKnowledgeBase(KnowledgeBase.ScopeType scopeType) {
        return switch (normalizeScope(scopeType)) {
            case PERSONAL -> canEditPersonalFrontKnowledgeBase();
            case PROJECT -> canEditProjectFrontKnowledgeBase();
            case PLATFORM -> false;
        };
    }

    public boolean canManageFrontKnowledgeBaseMember(KnowledgeBase.ScopeType scopeType) {
        return normalizeScope(scopeType) != KnowledgeBase.ScopeType.PLATFORM
                && canManageFrontKnowledgeBaseMember();
    }

    public void requireFrontKnowledgeBaseRead(KnowledgeBase.ScopeType scopeType) {
        requireAllowed(canReadFrontKnowledgeBase(scopeType), "You do not have permission to read this front knowledge base");
    }

    public void requireFrontKnowledgeBaseEdit(KnowledgeBase.ScopeType scopeType) {
        requireAllowed(canEditFrontKnowledgeBase(scopeType), "You do not have permission to edit this front knowledge base");
    }

    public void requireFrontKnowledgeBaseCreate(KnowledgeBase.ScopeType scopeType) {
        requireAllowed(canEditFrontKnowledgeBase(scopeType), "You do not have permission to create this front knowledge base");
    }

    public void requireFrontKnowledgeBaseMemberManage(KnowledgeBase.ScopeType scopeType) {
        requireAllowed(canManageFrontKnowledgeBaseMember(scopeType), "You do not have permission to manage front knowledge base members");
    }

    private void requireAllowed(boolean allowed, String message) {
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private KnowledgeBase.ScopeType normalizeScope(KnowledgeBase.ScopeType scopeType) {
        return scopeType == null ? KnowledgeBase.ScopeType.PERSONAL : scopeType;
    }

    private boolean hasFrontAssistantPermission() {
        return currentUserProvider.hasAuthority(FRONT_ASSISTANT_PERMISSION);
    }

    private boolean hasAnyFrontKnowledgePermission() {
        return hasFrontKnowledgeReadPermission()
                || hasFrontKnowledgeEditPermission()
                || hasFrontKnowledgeMemberManagePermission();
    }

    private boolean hasFrontKnowledgeReadPermission() {
        return hasLegacyKnowledgePermission()
                || hasPersonalKnowledgeReadPermission()
                || hasProjectKnowledgeReadPermission();
    }

    private boolean hasFrontKnowledgeEditPermission() {
        return hasLegacyKnowledgePermission()
                || hasPersonalKnowledgeEditPermission()
                || hasProjectKnowledgeEditPermission();
    }

    private boolean hasFrontKnowledgeMemberManagePermission() {
        return hasLegacyKnowledgePermission()
                || currentUserProvider.hasAuthority(FRONT_KB_MEMBER_MANAGE_PERMISSION);
    }

    private boolean hasPersonalKnowledgeReadPermission() {
        return hasLegacyKnowledgePermission()
                || currentUserProvider.hasAuthority(FRONT_PERSONAL_KB_READ_PERMISSION)
                || hasPersonalKnowledgeEditPermission()
                || hasFrontKnowledgeMemberManagePermission();
    }

    private boolean hasProjectKnowledgeReadPermission() {
        return hasLegacyKnowledgePermission()
                || currentUserProvider.hasAuthority(FRONT_PROJECT_KB_READ_PERMISSION)
                || hasProjectKnowledgeEditPermission()
                || hasFrontKnowledgeMemberManagePermission();
    }

    private boolean hasPersonalKnowledgeEditPermission() {
        return hasLegacyKnowledgePermission()
                || currentUserProvider.hasAuthority(FRONT_PERSONAL_KB_EDIT_PERMISSION);
    }

    private boolean hasProjectKnowledgeEditPermission() {
        return hasLegacyKnowledgePermission()
                || currentUserProvider.hasAuthority(FRONT_PROJECT_KB_EDIT_PERMISSION);
    }

    private boolean hasLegacyKnowledgePermission() {
        return currentUserProvider.hasAuthority(LEGACY_KNOWLEDGE_PERMISSION);
    }
}

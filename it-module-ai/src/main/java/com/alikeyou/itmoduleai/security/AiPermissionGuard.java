package com.alikeyou.itmoduleai.security;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

    public boolean canEditFrontKnowledgeBase() {
        return hasFrontKnowledgeEditPermission() || canUseAdminKnowledgeDebug();
    }

    public boolean canManageFrontKnowledgeBaseMember() {
        return hasFrontKnowledgeMemberManagePermission() || canUseAdminKnowledgeDebug();
    }

    public boolean canUseAdminKnowledgeDebug() {
        return currentUserProvider.hasAuthority(ADMIN_KNOWLEDGE_PERMISSION);
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
                || currentUserProvider.hasAuthority(FRONT_PERSONAL_KB_READ_PERMISSION)
                || currentUserProvider.hasAuthority(FRONT_PROJECT_KB_READ_PERMISSION)
                || hasFrontKnowledgeEditPermission()
                || hasFrontKnowledgeMemberManagePermission();
    }

    private boolean hasFrontKnowledgeEditPermission() {
        return hasLegacyKnowledgePermission()
                || currentUserProvider.hasAuthority(FRONT_PERSONAL_KB_EDIT_PERMISSION)
                || currentUserProvider.hasAuthority(FRONT_PROJECT_KB_EDIT_PERMISSION);
    }

    private boolean hasFrontKnowledgeMemberManagePermission() {
        return hasLegacyKnowledgePermission()
                || currentUserProvider.hasAuthority(FRONT_KB_MEMBER_MANAGE_PERMISSION);
    }

    private boolean hasLegacyKnowledgePermission() {
        return currentUserProvider.hasAuthority(LEGACY_KNOWLEDGE_PERMISSION);
    }
}

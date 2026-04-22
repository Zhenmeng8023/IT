package com.alikeyou.itmoduleai.security;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiPermissionGuardTest {

    private AiCurrentUserProvider currentUserProvider;
    private AiPermissionGuard guard;

    @BeforeEach
    void setUp() {
        currentUserProvider = mock(AiCurrentUserProvider.class);
        guard = new AiPermissionGuard(currentUserProvider);
    }

    @Test
    void readPermissionAcceptsNewFrontPersonalKnowledgeAuthority() {
        when(currentUserProvider.hasAuthority("view:front:ai:kb:self")).thenReturn(true);

        assertThat(guard.canReadFrontKnowledgeBase()).isTrue();
    }

    @Test
    void editPermissionAcceptsNewFrontProjectKnowledgeAuthority() {
        when(currentUserProvider.hasAuthority("edit:front:ai:kb:project")).thenReturn(true);

        assertThat(guard.canEditFrontKnowledgeBase()).isTrue();
    }

    @Test
    void memberManagePermissionAcceptsDedicatedAuthority() {
        when(currentUserProvider.hasAuthority("manage:front:ai:kb:member")).thenReturn(true);

        assertThat(guard.canManageFrontKnowledgeBaseMember()).isTrue();
        assertThat(guard.canReadFrontKnowledgeBase()).isTrue();
    }

    @Test
    void readPermissionNoLongerFallsBackToAuthenticatedUserOnly() {
        when(currentUserProvider.resolveCurrentUserId()).thenReturn(7L);

        assertThat(guard.canUseAssistant()).isTrue();
        assertThat(guard.canReadFrontKnowledgeBase()).isFalse();
    }

    @Test
    void legacyUnifiedPermissionRemainsReadableForCompatibility() {
        when(currentUserProvider.hasAuthority("view:knowledge-base")).thenReturn(true);

        assertThat(guard.canReadFrontKnowledgeBase()).isTrue();
        assertThat(guard.canEditFrontKnowledgeBase()).isTrue();
        assertThat(guard.canManageFrontKnowledgeBaseMember()).isTrue();
    }
}

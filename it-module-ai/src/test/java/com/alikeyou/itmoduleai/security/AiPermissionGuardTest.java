package com.alikeyou.itmoduleai.security;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiPermissionGuardTest {

    private AiCurrentUserProvider currentUserProvider;
    private KnowledgeBaseRepository knowledgeBaseRepository;
    private AiPermissionGuard guard;

    @BeforeEach
    void setUp() {
        currentUserProvider = mock(AiCurrentUserProvider.class);
        knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        guard = new AiPermissionGuard(currentUserProvider, knowledgeBaseRepository);
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

    @Test
    void personalPermissionDoesNotGrantProjectReadAccess() {
        when(currentUserProvider.hasAuthority("view:front:ai:kb:self")).thenReturn(true);

        assertThat(guard.canReadMyFrontKnowledgeBase()).isTrue();
        assertThat(guard.canReadProjectFrontKnowledgeBase()).isFalse();
        assertThat(guard.canReadFrontKnowledgeBase(KnowledgeBase.ScopeType.PROJECT)).isFalse();
    }

    @Test
    void projectEditPermissionGrantsProjectEditButNotPersonalEdit() {
        when(currentUserProvider.hasAuthority("edit:front:ai:kb:project")).thenReturn(true);

        assertThat(guard.canEditProjectFrontKnowledgeBase()).isTrue();
        assertThat(guard.canEditPersonalFrontKnowledgeBase()).isFalse();
        assertThat(guard.canEditFrontKnowledgeBase(KnowledgeBase.ScopeType.PROJECT)).isTrue();
        assertThat(guard.canEditFrontKnowledgeBase(KnowledgeBase.ScopeType.PERSONAL)).isFalse();
    }

    @Test
    void platformScopeIsRejectedByFrontKnowledgeChecks() {
        when(currentUserProvider.hasAuthority("view:admin:ai:knowledge")).thenReturn(true);

        assertThat(guard.canReadFrontKnowledgeBase(KnowledgeBase.ScopeType.PLATFORM)).isFalse();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> guard.requireFrontKnowledgeBaseRead(KnowledgeBase.ScopeType.PLATFORM));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void memberManagePermissionCannotManagePlatformScope() {
        when(currentUserProvider.hasAuthority("manage:front:ai:kb:member")).thenReturn(true);

        assertThat(guard.canManageFrontKnowledgeBaseMember(KnowledgeBase.ScopeType.PROJECT)).isTrue();
        assertThat(guard.canManageFrontKnowledgeBaseMember(KnowledgeBase.ScopeType.PLATFORM)).isFalse();
    }
}

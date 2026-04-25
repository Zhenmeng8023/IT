package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseMemberRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkEmbeddingRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeIndexTaskRepository;
import com.alikeyou.itmoduleai.service.CodeIndexService;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.alikeyou.itmoduleai.service.policy.KnowledgeBaseScopePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseServiceImplSecurityTest {

    private KnowledgeBaseRepository knowledgeBaseRepository;
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    private KnowledgeChunkRepository knowledgeChunkRepository;
    private KnowledgeIndexTaskRepository knowledgeIndexTaskRepository;
    private KnowledgeAccessGuard knowledgeAccessGuard;
    private AiCurrentUserProvider currentUserProvider;
    private KnowledgeBaseServiceImpl service;

    @BeforeEach
    void setUp() {
        knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        KnowledgeBaseMemberRepository knowledgeBaseMemberRepository = mock(KnowledgeBaseMemberRepository.class);
        knowledgeDocumentRepository = mock(KnowledgeDocumentRepository.class);
        knowledgeChunkRepository = mock(KnowledgeChunkRepository.class);
        knowledgeIndexTaskRepository = mock(KnowledgeIndexTaskRepository.class);
        knowledgeAccessGuard = mock(KnowledgeAccessGuard.class);
        currentUserProvider = mock(AiCurrentUserProvider.class);

        service = new KnowledgeBaseServiceImpl(
                knowledgeBaseRepository,
                knowledgeBaseMemberRepository,
                knowledgeDocumentRepository,
                knowledgeChunkRepository,
                mock(KnowledgeChunkEmbeddingRepository.class),
                knowledgeIndexTaskRepository,
                mock(KnowledgeChunkingService.class),
                mock(CodeIndexService.class),
                mock(KnowledgeEmbeddingService.class),
                knowledgeAccessGuard,
                mock(KnowledgeBaseScopePolicy.class),
                currentUserProvider,
                command -> command.run()
        );
    }

    @Test
    void editorCannotUpdateOwnerProjectAndScopeFields() {
        KnowledgeBase existing = knowledgeBase(1L, 100L, 200L, KnowledgeBase.ScopeType.PERSONAL);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(1L)).thenReturn(existing);
        when(knowledgeAccessGuard.hasKnowledgeBaseOwnerAccess(1L)).thenReturn(false);
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeBaseCreateRequest request = new KnowledgeBaseCreateRequest();
        request.setOwnerId(999L);
        request.setProjectId(888L);
        request.setScopeType(KnowledgeBase.ScopeType.PROJECT);
        request.setName("new-name");

        KnowledgeBase saved = service.updateKnowledgeBase(1L, request);

        assertEquals(100L, saved.getOwnerId());
        assertEquals(200L, saved.getProjectId());
        assertEquals(KnowledgeBase.ScopeType.PERSONAL, saved.getScopeType());
        assertEquals("new-name", saved.getName());
    }

    @Test
    void ownerCanUpdateOwnerProjectAndScopeFields() {
        KnowledgeBase existing = knowledgeBase(1L, 100L, 200L, KnowledgeBase.ScopeType.PERSONAL);
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(1L)).thenReturn(existing);
        when(knowledgeAccessGuard.hasKnowledgeBaseOwnerAccess(1L)).thenReturn(true);
        when(knowledgeBaseRepository.save(any(KnowledgeBase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeBaseCreateRequest request = new KnowledgeBaseCreateRequest();
        request.setOwnerId(101L);
        request.setProjectId(201L);
        request.setScopeType(KnowledgeBase.ScopeType.PROJECT);

        KnowledgeBase saved = service.updateKnowledgeBase(1L, request);

        assertEquals(101L, saved.getOwnerId());
        assertEquals(201L, saved.getProjectId());
        assertEquals(KnowledgeBase.ScopeType.PROJECT, saved.getScopeType());
    }

    @Test
    void nonAdminProjectPageUsesAccessibleQuery() {
        Pageable pageable = Pageable.unpaged();
        Page<KnowledgeBase> expected = new PageImpl<>(List.of(knowledgeBase(1L, 10L, 20L, KnowledgeBase.ScopeType.PROJECT)));
        when(currentUserProvider.isAdminAiViewer()).thenReturn(false);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(10L);
        when(knowledgeBaseRepository.findAccessibleProjectPage(99L, 10L, pageable)).thenReturn(expected);

        Page<KnowledgeBase> actual = service.pageByProject(99L, pageable);

        assertSame(expected, actual);
        verify(knowledgeBaseRepository, never()).findByProjectIdOrderByUpdatedAtDesc(any(), any());
    }

    @Test
    void listChunksRejectsUnauthorizedDocumentAccess() {
        when(knowledgeAccessGuard.requireDocumentRead(11L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.listChunks(11L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(knowledgeChunkRepository, never()).findByDocument_IdOrderByChunkIndexAsc(any());
    }

    @Test
    void listDocumentTasksRejectsUnauthorizedDocumentAccess() {
        when(knowledgeAccessGuard.requireDocumentRead(12L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.listDocumentTasks(12L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(knowledgeIndexTaskRepository, never()).findByDocument_IdOrderByCreatedAtDesc(any());
    }

    @Test
    void downloadDocumentRejectsUnauthorizedDocumentAccess() {
        when(knowledgeAccessGuard.requireDocumentRead(13L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.downloadDocument(13L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(knowledgeDocumentRepository, never()).findById(any());
        verify(knowledgeChunkRepository, never()).findByDocument_IdOrderByChunkIndexAsc(any());
        verify(knowledgeIndexTaskRepository, never()).findByDocument_IdOrderByCreatedAtDesc(any());
    }

    private KnowledgeBase knowledgeBase(Long id, Long ownerId, Long projectId, KnowledgeBase.ScopeType scopeType) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setProjectId(projectId);
        knowledgeBase.setScopeType(scopeType);
        knowledgeBase.setName("kb");
        knowledgeBase.setVisibility(KnowledgeBase.Visibility.PRIVATE);
        knowledgeBase.setSourceType(KnowledgeBase.SourceType.MANUAL);
        knowledgeBase.setChunkStrategy(KnowledgeBase.ChunkStrategy.PARAGRAPH);
        knowledgeBase.setStatus(KnowledgeBase.Status.ACTIVE);
        return knowledgeBase;
    }
}

package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.application.support.AiKnowledgeResolver;
import com.alikeyou.itmoduleai.dto.request.KnowledgeSearchDebugRequest;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeDebugControllerSecurityTest {

    private KnowledgeChunkingService knowledgeChunkingService;
    private KnowledgeEmbeddingService knowledgeEmbeddingService;
    private AiKnowledgeResolver aiKnowledgeResolver;
    private KnowledgeAccessGuard knowledgeAccessGuard;
    private KnowledgeDebugController controller;

    @BeforeEach
    void setUp() {
        knowledgeChunkingService = mock(KnowledgeChunkingService.class);
        knowledgeEmbeddingService = mock(KnowledgeEmbeddingService.class);
        aiKnowledgeResolver = mock(AiKnowledgeResolver.class);
        knowledgeAccessGuard = mock(KnowledgeAccessGuard.class);

        controller = new KnowledgeDebugController(
                mock(KnowledgeChunkRepository.class),
                knowledgeChunkingService,
                knowledgeEmbeddingService,
                aiKnowledgeResolver,
                knowledgeAccessGuard
        );
    }

    @Test
    void previewDocumentChunksRejectsUnauthorizedDocumentAccess() {
        when(knowledgeAccessGuard.requireDocumentRead(11L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.previewDocumentChunks(11L, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(knowledgeChunkingService, never()).previewChunks(any(), any(), any(), any());
    }

    @Test
    void backfillKnowledgeBaseEmbeddingsRequiresEditAccess() {
        when(knowledgeAccessGuard.requireKnowledgeBaseEdit(12L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.backfillKnowledgeBaseEmbeddings(12L, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(knowledgeEmbeddingService, never()).backfillKnowledgeBaseEmbeddings(any(), any(), any(), any());
    }

    @Test
    void searchDebugRequiresKnowledgeBaseReadAccess() {
        KnowledgeSearchDebugRequest request = new KnowledgeSearchDebugRequest();
        request.setQuery("how to use this code");
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(13L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.searchDebug(13L, request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(aiKnowledgeResolver, never()).retrieve(any(), any(), any(), any());
    }
}

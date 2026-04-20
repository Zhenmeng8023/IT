package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.application.support.AiKnowledgeResolver;
import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.response.KnowledgeChunkPreviewResponse;
import com.alikeyou.itmoduleai.dto.response.KnowledgeEmbeddingStatusResponse;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import com.alikeyou.itmoduleai.enums.GroundingStatus;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KnowledgeDebugControllerHttpTest {

    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock
    private KnowledgeChunkingService knowledgeChunkingService;
    @Mock
    private KnowledgeEmbeddingService knowledgeEmbeddingService;
    @Mock
    private AiKnowledgeResolver aiKnowledgeResolver;
    @Mock
    private KnowledgeAccessGuard knowledgeAccessGuard;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        KnowledgeDebugController controller = new KnowledgeDebugController(
                knowledgeChunkRepository,
                knowledgeChunkingService,
                knowledgeEmbeddingService,
                aiKnowledgeResolver,
                knowledgeAccessGuard
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void searchDebugReturnsRetrievalDiagnostics() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(12L)).thenReturn(knowledgeBase(12L));
        when(aiKnowledgeResolver.retrieve(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new AiKnowledgeResolver.RetrievalResult(
                        List.of(12L),
                        List.of(KnowledgeRetrievalHit.builder()
                                .knowledgeBaseId(12L)
                                .knowledgeBaseName("kb-12")
                                .documentId(31L)
                                .documentTitle("README")
                                .path("README.md")
                                .chunkId(41L)
                                .chunkIndex(0)
                                .snippet("debug snippet")
                                .score(new BigDecimal("0.920000"))
                                .retrievalMethod(AiRetrievalLog.RetrievalMethod.HYBRID)
                                .stageCode(AiRetrievalLog.StageCode.RERANK)
                                .candidateSource(AiRetrievalLog.CandidateSource.CHUNK)
                                .rankNo(1)
                                .build()),
                        3,
                        8,
                        5,
                        20,
                        1,
                        2,
                        0,
                        null,
                        AiAnalysisMode.CODE_LOCATE,
                        true,
                        GroundingStatus.STRICT_PASS,
                        false,
                        null,
                        null,
                        null,
                        "code-locate",
                        "hybrid"));

        mockMvc.perform(post("/api/ai/knowledge-bases/12/search-debug")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "find controller route",
                                  "topK": 3,
                                  "mode": "CODE_LOCATE",
                                  "strictGrounding": true,
                                  "entryFile": "KnowledgeDebugController.java",
                                  "traceDepth": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.knowledgeBaseId").value(12))
                .andExpect(jsonPath("$.data.query").value("find controller route"))
                .andExpect(jsonPath("$.data.mode").value("CODE_LOCATE"))
                .andExpect(jsonPath("$.data.strictGrounding").value(true))
                .andExpect(jsonPath("$.data.groundingStatus").value("STRICT_PASS"))
                .andExpect(jsonPath("$.data.vectorCandidateCount").value(8))
                .andExpect(jsonPath("$.data.hitCount").value(1))
                .andExpect(jsonPath("$.data.hits[0].chunkId").value(41))
                .andExpect(jsonPath("$.data.hits[0].retrievalMethod").value("HYBRID"));
    }

    @Test
    void searchDebugRejectsBlankQuery() throws Exception {
        mockMvc.perform(post("/api/ai/knowledge-bases/12/search-debug")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verify(knowledgeAccessGuard, never()).requireKnowledgeBaseRead(any());
        verify(aiKnowledgeResolver, never()).retrieve(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchDebugRejectsUnauthorizedKnowledgeBase() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(12L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        mockMvc.perform(post("/api/ai/knowledge-bases/12/search-debug")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"hello\"}"))
                .andExpect(status().isForbidden());

        verify(aiKnowledgeResolver, never()).retrieve(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void previewDocumentChunksReturnsPreviewItems() throws Exception {
        KnowledgeBase kb = knowledgeBase(12L);
        KnowledgeDocument document = document(31L, kb);
        document.setContentText("fallback text");
        when(knowledgeAccessGuard.requireDocumentRead(31L)).thenReturn(document);
        when(knowledgeChunkingService.previewChunks(any(), any(), any(), any()))
                .thenReturn(List.of(KnowledgeChunkPreviewResponse.builder()
                        .chunkIndex(0)
                        .title("part-0")
                        .content("custom preview")
                        .charCount(14)
                        .tokenCount(3)
                        .build()));

        mockMvc.perform(post("/api/ai/knowledge-bases/documents/31/chunk-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"custom preview\",\"maxChars\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].chunkIndex").value(0))
                .andExpect(jsonPath("$.data[0].content").value("custom preview"));
    }

    @Test
    void embeddingStatusReturnsNotFoundFromGuard() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge base not found"));

        mockMvc.perform(get("/api/ai/knowledge-bases/99/embedding-status"))
                .andExpect(status().isNotFound());

        verify(knowledgeEmbeddingService, never()).getKnowledgeBaseEmbeddingStatus(any());
    }

    @Test
    void embeddingStatusReturnsCurrentProfileCounters() throws Exception {
        when(knowledgeAccessGuard.requireKnowledgeBaseRead(12L)).thenReturn(knowledgeBase(12L));
        when(knowledgeEmbeddingService.getKnowledgeBaseEmbeddingStatus(12L))
                .thenReturn(KnowledgeEmbeddingStatusResponse.builder()
                        .targetType("KNOWLEDGE_BASE")
                        .targetId(12L)
                        .totalChunkCount(10L)
                        .embeddedChunkCount(8L)
                        .activeProvider("ollama")
                        .activeModelName("nomic-embed-text")
                        .activeDimension(768)
                        .needsRebuild(true)
                        .build());

        mockMvc.perform(get("/api/ai/knowledge-bases/12/embedding-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetType").value("KNOWLEDGE_BASE"))
                .andExpect(jsonPath("$.data.totalChunkCount").value(10))
                .andExpect(jsonPath("$.data.needsRebuild").value(true))
                .andExpect(jsonPath("$.data.activeProvider").value("ollama"));
    }

    private KnowledgeBase knowledgeBase(Long id) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setOwnerId(1L);
        kb.setName("kb-" + id);
        kb.setScopeType(KnowledgeBase.ScopeType.PERSONAL);
        kb.setVisibility(KnowledgeBase.Visibility.PRIVATE);
        kb.setSourceType(KnowledgeBase.SourceType.MANUAL);
        kb.setChunkStrategy(KnowledgeBase.ChunkStrategy.PARAGRAPH);
        kb.setStatus(KnowledgeBase.Status.ACTIVE);
        return kb;
    }

    private KnowledgeDocument document(Long id, KnowledgeBase kb) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setKnowledgeBase(kb);
        document.setSourceType(KnowledgeDocument.SourceType.MANUAL);
        document.setTitle("doc-" + id);
        document.setStatus(KnowledgeDocument.Status.UPLOADED);
        return document;
    }
}

package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.application.orchestrator.impl.DefaultAiChatOrchestrator;
import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeChunkEmbedding;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.repository.AiRetrievalLogRepository;
import com.alikeyou.itmoduleai.repository.AiSessionKnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkEmbeddingRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeResolverTest {

    @Mock
    private AiSessionKnowledgeBaseRepository aiSessionKnowledgeBaseRepository;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock
    private KnowledgeChunkEmbeddingRepository knowledgeChunkEmbeddingRepository;
    @Mock
    private AiRetrievalLogRepository aiRetrievalLogRepository;
    @Mock
    private KnowledgeEmbeddingService knowledgeEmbeddingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AiKnowledgeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AiKnowledgeResolver(
                aiSessionKnowledgeBaseRepository,
                knowledgeBaseRepository,
                knowledgeChunkRepository,
                knowledgeChunkEmbeddingRepository,
                aiRetrievalLogRepository,
                knowledgeEmbeddingService,
                objectMapper
        );
        setIntField("defaultTopK", 5);
        setIntField("maxTopK", 20);
        setIntField("maxVectorCandidates", 100);
        setIntField("maxKeywordCandidates", 100);

        lenient().when(knowledgeChunkEmbeddingRepository.countLatestByKnowledgeBaseIds(List.of(1L))).thenReturn(2L);
        lenient().when(knowledgeChunkEmbeddingRepository.countLatestByKnowledgeBaseIdsAndStatus(
                List.of(1L), KnowledgeChunkEmbedding.Status.ACTIVE)).thenReturn(2L);
        lenient().when(knowledgeChunkEmbeddingRepository.countLatestByKnowledgeBaseIdsAndProviderAndStatus(
                eq(List.of(1L)), anyCollection(), eq("ollama"), eq(KnowledgeChunkEmbedding.Status.ACTIVE))).thenReturn(2L);
        lenient().when(knowledgeChunkEmbeddingRepository.countLatestByKnowledgeBaseIdsAndProviderAndModelAndStatus(
                eq(List.of(1L)), anyCollection(), eq("ollama"), anyCollection(), eq("embeddinggemma:300m"), eq(KnowledgeChunkEmbedding.Status.ACTIVE))).thenReturn(1L);
        lenient().when(knowledgeChunkEmbeddingRepository.countLatestByKnowledgeBaseIdsAndProviderAndModelAndStatusNot(
                eq(List.of(1L)), anyCollection(), eq("ollama"), anyCollection(), eq("embeddinggemma:300m"), eq(KnowledgeChunkEmbedding.Status.ACTIVE))).thenReturn(0L);
        lenient().when(knowledgeBaseRepository.findById(org.mockito.ArgumentMatchers.anyLong())).thenReturn(Optional.of(kb()));
    }

    @Test
    void retrievesVectorCandidatesBeforeKeywordAndReranks() throws Exception {
        KnowledgeChunk vectorChunk = chunk(10L, "billing service handles invoices", 1);
        KnowledgeChunk keywordChunk = chunk(11L, "alpha alpha alpha only keyword match", 2);
        KnowledgeChunkEmbedding embedding = embedding(vectorChunk, List.of(1.0, 0.0));

        when(knowledgeEmbeddingService.embedText(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(1.0, 0.0));
        when(knowledgeChunkEmbeddingRepository.findLatestActiveByKnowledgeBaseIdsAndProviderAndModel(
                anyCollection(), anyCollection(), anyString(), anyCollection(), anyString(),
                eq(KnowledgeChunkEmbedding.Status.ACTIVE))).thenReturn(List.of(embedding));
        when(knowledgeEmbeddingService.parseVectorPayload(embedding.getVectorPayload())).thenReturn(List.of(1.0, 0.0));
        when(knowledgeChunkRepository.findKeywordCandidatesByKnowledgeBaseIds(org.mockito.ArgumentMatchers.<List<Long>>any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(keywordChunk));

        AiKnowledgeResolver.RetrievalResult result = resolver.retrieve(null, "alpha", List.of(1L), 2);

        assertThat(result.getVectorCandidateCount()).isEqualTo(1);
        assertThat(result.getKeywordCandidateCount()).isEqualTo(1);
        assertThat(result.getHits()).hasSize(2);
        assertThat(result.getHits().get(0).getChunkId()).isEqualTo(10L);
        verify(knowledgeChunkEmbeddingRepository, never()).findLatestByKnowledgeBaseIds(anyCollection());
    }

    @Test
    void degradesToKeywordWhenQueryEmbeddingFails() {
        KnowledgeChunk keywordChunk = chunk(12L, "refund policy alpha", 1);
        when(knowledgeEmbeddingService.embedText(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new IllegalStateException("ollama down"));
        when(knowledgeChunkRepository.findKeywordCandidatesByKnowledgeBaseIds(org.mockito.ArgumentMatchers.<List<Long>>any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(keywordChunk));

        AiKnowledgeResolver.RetrievalResult result = resolver.retrieve(null, "alpha", List.of(1L), 3);

        assertThat(result.getVectorCandidateCount()).isZero();
        assertThat(result.getKeywordCandidateCount()).isEqualTo(1);
        assertThat(result.getDegradeReason()).contains("Query embedding failed");
        assertThat(result.getHits()).extracting(KnowledgeRetrievalHit::getChunkId).containsExactly(12L);
    }

    @Test
    void honorsRequestedTopKWithoutRaisingToFive() throws Exception {
        KnowledgeChunk first = chunk(20L, "first", 1);
        KnowledgeChunk second = chunk(21L, "second", 2);
        KnowledgeChunkEmbedding firstEmbedding = embedding(first, List.of(1.0, 0.0));
        KnowledgeChunkEmbedding secondEmbedding = embedding(second, List.of(0.9, 0.1));
        when(knowledgeEmbeddingService.embedText(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(1.0, 0.0));
        when(knowledgeChunkEmbeddingRepository.findLatestActiveByKnowledgeBaseIdsAndProviderAndModel(
                anyCollection(), anyCollection(), anyString(), anyCollection(), anyString(),
                eq(KnowledgeChunkEmbedding.Status.ACTIVE))).thenReturn(List.of(firstEmbedding, secondEmbedding));
        when(knowledgeEmbeddingService.parseVectorPayload(firstEmbedding.getVectorPayload())).thenReturn(List.of(1.0, 0.0));
        when(knowledgeEmbeddingService.parseVectorPayload(secondEmbedding.getVectorPayload())).thenReturn(List.of(0.9, 0.1));

        AiKnowledgeResolver.RetrievalResult result = resolver.retrieve(null, "alpha", List.of(1L), 1);

        assertThat(result.getTopK()).isEqualTo(1);
        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().get(0).getChunkId()).isEqualTo(20L);
    }

    @Test
    void keepsTopKZeroWithoutForcingRecall() {
        AiKnowledgeResolver.RetrievalResult result = resolver.retrieve(null, "alpha", List.of(1L), 0);

        assertThat(result.getTopK()).isEqualTo(0);
        assertThat(result.getHits()).isEmpty();
        verify(knowledgeEmbeddingService, never()).embedText(anyString(), anyString(), anyString(), anyInt());
        verify(knowledgeChunkRepository, never()).findKeywordCandidatesByKnowledgeBaseIds(org.mockito.ArgumentMatchers.<List<Long>>any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void orchestratorPassesTopKThroughWithoutDefaultInflation() {
        DefaultAiChatOrchestrator orchestrator = new DefaultAiChatOrchestrator(
                mock(com.alikeyou.itmoduleai.repository.AiSessionRepository.class),
                mock(com.alikeyou.itmoduleai.repository.AiMessageRepository.class),
                mock(com.alikeyou.itmoduleai.repository.AiCallLogRepository.class),
                mock(com.alikeyou.itmoduleai.provider.AiProviderManager.class),
                mock(AiModelSelector.class),
                mock(AiPromptResolver.class),
                mock(AiContextMessageBuilder.class),
                resolver,
                objectMapper,
                mock(org.springframework.transaction.PlatformTransactionManager.class),
                mock(AiScenePostProcessor.class),
                mock(AiCurrentUserProvider.class)
        );
        AiChatSendRequest request = new AiChatSendRequest();
        request.setRequestParams(Map.of("topK", 2));

        Integer resolved = ReflectionTestUtils.invokeMethod(orchestrator, "resolveTopK", request);

        assertThat(resolved).isEqualTo(2);
    }

    @Test
    void selectsEmbeddingsByNormalizedProviderAndModel() throws Exception {
        KnowledgeChunk vectorChunk = chunk(40L, "payment overview", 1);
        KnowledgeChunkEmbedding embedding = embedding(vectorChunk, List.of(1.0, 0.0));
        when(knowledgeEmbeddingService.embedText(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(1.0, 0.0));
        when(knowledgeEmbeddingService.parseVectorPayload(embedding.getVectorPayload()))
                .thenReturn(List.of(1.0, 0.0));
        when(knowledgeChunkEmbeddingRepository.findLatestActiveByKnowledgeBaseIdsAndProviderAndModel(
                anyCollection(), anyCollection(), anyString(), anyCollection(), anyString(),
                eq(KnowledgeChunkEmbedding.Status.ACTIVE))).thenReturn(List.of(embedding));
        when(knowledgeChunkRepository.findKeywordCandidatesByKnowledgeBaseIds(org.mockito.ArgumentMatchers.<List<Long>>any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        AiKnowledgeResolver.RetrievalResult result = resolver.retrieve(null, "payment", List.of(1L), 3);

        assertThat(result.getHits()).hasSize(1);
        ArgumentCaptor<Collection<String>> providerCodesCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<String> providerCanonicalCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Collection<String>> modelNamesCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<String> modelCanonicalCaptor = ArgumentCaptor.forClass(String.class);
        verify(knowledgeChunkEmbeddingRepository).findLatestActiveByKnowledgeBaseIdsAndProviderAndModel(
                anyCollection(),
                providerCodesCaptor.capture(),
                providerCanonicalCaptor.capture(),
                modelNamesCaptor.capture(),
                modelCanonicalCaptor.capture(),
                eq(KnowledgeChunkEmbedding.Status.ACTIVE)
        );
        assertThat(providerCanonicalCaptor.getValue()).isEqualTo("ollama");
        assertThat(providerCodesCaptor.getValue()).contains("ollama", "ollama-local");
        assertThat(modelCanonicalCaptor.getValue()).isEqualTo("embeddinggemma:300m");
        assertThat(modelNamesCaptor.getValue()).contains("embeddinggemma:300m");
    }

    @Test
    void buildsCroppedDeduplicatedEvidenceBlocks() {
        String longSnippet = "alpha " + "x".repeat(1800);
        KnowledgeRetrievalHit first = KnowledgeRetrievalHit.builder()
                .knowledgeBaseName("KB")
                .documentId(99L)
                .documentTitle("Doc")
                .path("src/App.vue")
                .chunkId(30L)
                .chunkIndex(1)
                .snippet(longSnippet)
                .rankNo(1)
                .language("vue")
                .symbolName("setup")
                .symbolType("function")
                .startLine(10)
                .endLine(20)
                .build();
        KnowledgeRetrievalHit adjacent = KnowledgeRetrievalHit.builder()
                .knowledgeBaseName("KB")
                .documentId(99L)
                .documentTitle("Doc")
                .path("src/App.vue")
                .chunkId(31L)
                .chunkIndex(2)
                .snippet("beta")
                .rankNo(2)
                .language("vue")
                .symbolName("methods")
                .symbolType("vue-option")
                .startLine(21)
                .endLine(30)
                .build();
        KnowledgeRetrievalHit duplicate = KnowledgeRetrievalHit.builder()
                .chunkId(30L)
                .chunkIndex(1)
                .snippet(longSnippet)
                .rankNo(3)
                .build();

        String prompt = resolver.buildKnowledgeAugmentedQuestion("What changed?", List.of(first, adjacent, duplicate));

        assertThat(prompt).contains("Evidence #1");
        assertThat(prompt).contains("knowledgeBase: KB");
        assertThat(prompt).contains("path: src/App.vue");
        assertThat(prompt).contains("chunk: #1-#2");
        assertThat(prompt).doesNotContain("Evidence #2");
        assertThat(prompt.length()).isLessThan(2200);
    }

    private KnowledgeBase kb() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setName("KB");
        kb.setEmbeddingProvider("Ollama");
        kb.setEmbeddingModel("embeddinggemma:300m (local)");
        kb.setDefaultTopK(5);
        return kb;
    }

    private void setIntField(String name, int value) {
        try {
            Field field = AiKnowledgeResolver.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setInt(resolver, value);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private KnowledgeDocument doc() {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(1L);
        doc.setTitle("Doc");
        doc.setFileName("doc.md");
        doc.setKnowledgeBase(kb());
        return doc;
    }

    private KnowledgeChunk chunk(Long id, String content, int index) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(id);
        chunk.setKnowledgeBase(kb());
        chunk.setDocument(doc());
        chunk.setChunkIndex(index);
        chunk.setContent(content);
        return chunk;
    }

    private KnowledgeChunkEmbedding embedding(KnowledgeChunk chunk, List<Double> vector) throws Exception {
        KnowledgeChunkEmbedding embedding = new KnowledgeChunkEmbedding();
        embedding.setId(chunk.getId() + 1000);
        embedding.setChunk(chunk);
        embedding.setProviderCode("ollama");
        embedding.setModelName("embeddinggemma:300m");
        embedding.setStatus(KnowledgeChunkEmbedding.Status.ACTIVE);
        embedding.setVectorPayload(objectMapper.writeValueAsString(vector));
        return embedding;
    }
}

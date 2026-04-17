package com.alikeyou.itmoduleai.application.orchestrator.impl;

import com.alikeyou.itmoduleai.application.support.AiContextMessageBuilder;
import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.application.support.AiKnowledgeResolver;
import com.alikeyou.itmoduleai.application.support.AiModelSelector;
import com.alikeyou.itmoduleai.application.support.AiPromptResolver;
import com.alikeyou.itmoduleai.application.support.AiScenePostProcessor;
import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import com.alikeyou.itmoduleai.enums.GroundingStatus;
import com.alikeyou.itmoduleai.provider.AiProvider;
import com.alikeyou.itmoduleai.provider.AiProviderManager;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatRequest;
import com.alikeyou.itmoduleai.provider.model.AiProviderStreamChunk;
import com.alikeyou.itmoduleai.repository.AiCallLogRepository;
import com.alikeyou.itmoduleai.repository.AiMessageRepository;
import com.alikeyou.itmoduleai.repository.AiSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscription;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAiChatOrchestratorStreamTest {

    private AiSessionRepository aiSessionRepository;
    private AiMessageRepository aiMessageRepository;
    private AiCallLogRepository aiCallLogRepository;
    private AiProviderManager aiProviderManager;
    private AiModelSelector aiModelSelector;
    private AiPromptResolver aiPromptResolver;
    private AiContextMessageBuilder aiContextMessageBuilder;
    private AiKnowledgeResolver aiKnowledgeResolver;
    private DefaultAiChatOrchestrator orchestrator;
    private AiProvider provider;

    @BeforeEach
    void setUp() {
        aiSessionRepository = mock(AiSessionRepository.class);
        aiMessageRepository = mock(AiMessageRepository.class);
        aiCallLogRepository = mock(AiCallLogRepository.class);
        aiProviderManager = mock(AiProviderManager.class);
        aiModelSelector = mock(AiModelSelector.class);
        aiPromptResolver = mock(AiPromptResolver.class);
        aiContextMessageBuilder = mock(AiContextMessageBuilder.class);
        aiKnowledgeResolver = mock(AiKnowledgeResolver.class);
        provider = mock(AiProvider.class);

        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        AiCurrentUserProvider currentUserProvider = mock(AiCurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(100L);

        orchestrator = new DefaultAiChatOrchestrator(
                aiSessionRepository,
                aiMessageRepository,
                aiCallLogRepository,
                aiProviderManager,
                aiModelSelector,
                aiPromptResolver,
                aiContextMessageBuilder,
                aiKnowledgeResolver,
                new ObjectMapper(),
                transactionManager,
                mock(AiScenePostProcessor.class),
                currentUserProvider
        );
    }

    @Test
    void streamCancellationPersistsInterruptedState() throws Exception {
        AiSession session = session(10L, 100L);
        AiModel model = model(20L);
        AiPromptTemplate promptTemplate = promptTemplate(30L);
        KnowledgeRetrievalHit hit = hit();
        AiKnowledgeResolver.RetrievalResult retrieval = mockRetrieval(hit);
        AiMessage assistantPlaceholder = message(102L, AiMessage.Role.ASSISTANT);

        when(aiSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(aiModelSelector.select(any(), any(), any())).thenReturn(model);
        when(aiPromptResolver.resolve(any(), any(), any())).thenReturn(promptTemplate);
        when(aiProviderManager.resolve(model)).thenReturn(provider);
        when(aiContextMessageBuilder.build(any(), any(), anyString())).thenReturn(List.of());
        when(aiKnowledgeResolver.retrieve(any(), anyString(), any(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(retrieval);
        when(aiKnowledgeResolver.buildKnowledgeAugmentedQuestion(anyString(), any())).thenReturn("augmented question");
        when(aiKnowledgeResolver.buildCitations(any())).thenReturn(List.of());
        when(aiMessageRepository.save(any(AiMessage.class))).thenAnswer(invocation -> {
            AiMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                message.setId(message.getRole() == AiMessage.Role.USER ? 101L : assistantPlaceholder.getId());
            }
            return message;
        });
        when(aiMessageRepository.findById(assistantPlaceholder.getId())).thenReturn(Optional.of(assistantPlaceholder));
        when(aiCallLogRepository.save(any(AiCallLog.class))).thenAnswer(invocation -> {
            AiCallLog log = invocation.getArgument(0);
            log.setId(201L);
            return log;
        });
        when(provider.streamChat(any(AiProviderChatRequest.class))).thenReturn(Flux.just(
                AiProviderStreamChunk.builder().delta("part-1").finished(false).build(),
                AiProviderStreamChunk.builder().delta("part-2").finished(true).finishReason("stop").build()
        ));

        AiChatSendRequest request = request();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger seen = new AtomicInteger();
        orchestrator.stream(request)
                .doFinally(signal -> latch.countDown())
                .subscribe(new BaseSubscriber<>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }

                    @Override
                    protected void hookOnNext(com.alikeyou.itmoduleai.dto.response.AiChatStreamChunkResponse value) {
                        if (seen.incrementAndGet() == 1) {
                            cancel();
                        }
                    }
                });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(aiCallLogRepository).save(any(AiCallLog.class));
        ArgumentCaptor<AiMessage> messageCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(aiMessageRepository, atLeastOnce()).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues())
                .extracting(AiMessage::getStreamState)
                .contains(AiMessage.StreamState.CANCELLED);
    }

    private AiChatSendRequest request() {
        AiChatSendRequest request = new AiChatSendRequest();
        request.setSessionId(10L);
        request.setContent("button click chain");
        request.setAnalysisMode(AiAnalysisMode.CODE_LOGIC);
        request.setStrictGrounding(true);
        request.setKnowledgeBaseIds(List.of(1L));
        request.setRequestParams(java.util.Map.of("topK", 3));
        request.setGroundingStatus(GroundingStatus.NOT_CHECKED);
        request.setEntryFile("src/main/java/demo/OrderController.java");
        request.setSymbolHint("submitOrder");
        request.setTraceDepth(2);
        return request;
    }

    private AiSession session(Long id, Long userId) {
        AiSession session = new AiSession();
        session.setId(id);
        session.setUserId(userId);
        session.setBizType(AiSession.BizType.GENERAL);
        session.setSceneCode("general.chat");
        session.setStatus(AiSession.Status.ACTIVE);
        session.setMemoryMode(AiSession.MemoryMode.SHORT);
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        session.setDefaultKnowledgeBase(kb);
        return session;
    }

    private AiModel model(Long id) {
        AiModel model = new AiModel();
        model.setId(id);
        model.setModelName("qwen");
        return model;
    }

    private AiPromptTemplate promptTemplate(Long id) {
        AiPromptTemplate template = new AiPromptTemplate();
        template.setId(id);
        template.setTemplateName("default");
        return template;
    }

    private AiMessage message(Long id, AiMessage.Role role) {
        AiMessage message = new AiMessage();
        message.setId(id);
        message.setRole(role);
        message.setSession(session(10L, 100L));
        message.setStatus(AiMessage.Status.SUCCESS);
        message.setStreamState(AiMessage.StreamState.PARTIAL);
        message.setPartialSeq(0);
        message.setCreatedAt(Instant.now());
        return message;
    }

    private KnowledgeRetrievalHit hit() {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(1L);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(2L);
        document.setTitle("OrderController.java");
        document.setFilePath("src/main/java/demo/OrderController.java");
        chunk.setDocument(document);
        return KnowledgeRetrievalHit.builder()
                .knowledgeBaseId(1L)
                .documentId(2L)
                .chunkId(1L)
                .chunkIndex(0)
                .documentTitle("OrderController.java")
                .path("src/main/java/demo/OrderController.java")
                .snippet("class OrderController {}")
                .rankNo(1)
                .build();
    }

    @SuppressWarnings("unchecked")
    private AiKnowledgeResolver.RetrievalResult mockRetrieval(KnowledgeRetrievalHit hit) {
        AiKnowledgeResolver.RetrievalResult retrieval = mock(AiKnowledgeResolver.RetrievalResult.class);
        when(retrieval.getHits()).thenReturn(List.of(hit));
        when(retrieval.getKnowledgeBaseIds()).thenReturn(List.of(1L));
        when(retrieval.getMode()).thenReturn(AiAnalysisMode.CODE_LOGIC);
        when(retrieval.isStrictGrounding()).thenReturn(true);
        when(retrieval.getGroundingStatus()).thenReturn(GroundingStatus.NOT_CHECKED);
        when(retrieval.isRefused()).thenReturn(false);
        return retrieval;
    }
}

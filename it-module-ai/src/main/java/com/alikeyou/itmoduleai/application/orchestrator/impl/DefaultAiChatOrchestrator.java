package com.alikeyou.itmoduleai.application.orchestrator.impl;

import com.alikeyou.itmoduleai.application.orchestrator.AiChatOrchestrator;
import com.alikeyou.itmoduleai.application.support.AiContextMessageBuilder;
import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.application.support.AiKnowledgeResolver;
import com.alikeyou.itmoduleai.application.support.AiModelSelector;
import com.alikeyou.itmoduleai.application.support.AiPromptResolver;
import com.alikeyou.itmoduleai.application.support.AiSceneActionCatalog;
import com.alikeyou.itmoduleai.application.support.AiScenePostProcessor;
import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.dto.response.AiChatStreamChunkResponse;
import com.alikeyou.itmoduleai.dto.response.AiChatTurnResponse;
import com.alikeyou.itmoduleai.dto.response.AiCitationResponse;
import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import com.alikeyou.itmoduleai.enums.AiStructuredApplyTarget;
import com.alikeyou.itmoduleai.enums.GroundingStatus;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.provider.AiProvider;
import com.alikeyou.itmoduleai.provider.AiProviderManager;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatRequest;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatResponse;
import com.alikeyou.itmoduleai.provider.model.AiProviderMessage;
import com.alikeyou.itmoduleai.repository.AiCallLogRepository;
import com.alikeyou.itmoduleai.repository.AiMessageRepository;
import com.alikeyou.itmoduleai.repository.AiSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAiChatOrchestrator implements AiChatOrchestrator {

    private static final int STREAM_PARTIAL_SAVE_CHAR_STEP = 120;
    private static final long STREAM_PARTIAL_SAVE_INTERVAL_MS = 1200L;
    private static final AiSceneActionCatalog SCENE_ACTION_CATALOG = new AiSceneActionCatalog();

    private final AiSessionRepository aiSessionRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final AiProviderManager aiProviderManager;
    private final AiModelSelector aiModelSelector;
    private final AiPromptResolver aiPromptResolver;
    private final AiContextMessageBuilder aiContextMessageBuilder;
    private final AiKnowledgeResolver aiKnowledgeResolver;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final AiScenePostProcessor aiScenePostProcessor;
    private final AiCurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public AiChatTurnResponse chat(AiChatSendRequest request) {
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        AiSession session = loadOwnedSession(request.getSessionId(), currentUserId);
        String responseSceneCode = resolveSceneCode(request, session);
        String responseActionCode = resolveActionCode(request);
        List<AiStructuredApplyTarget> applyTargets = resolveApplyTargets(responseSceneCode, responseActionCode);
        AiAnalysisMode analysisMode = resolveAnalysisMode(request, session);
        boolean strictGrounding = resolveStrictGrounding(request, session);
        AiPromptTemplate promptTemplate = aiPromptResolver.resolve(request.getPromptTemplateId(), responseSceneCode, session);
        AiModel model = aiModelSelector.select(request.getModelId(), session, promptTemplate);
        AiProvider provider = aiProviderManager.resolve(model);
        AiKnowledgeResolver.RetrievalResult retrieval = retrieveEvidence(session, request, analysisMode, strictGrounding);

        AiMessage userMessage = saveUserMessage(session, currentUserId, request, promptTemplate, model);
        Instant start = Instant.now();
        if (retrieval.isRefused()) {
            AiProviderChatResponse refusalResponse = AiProviderChatResponse.builder()
                    .content(retrieval.getRefusalMessage())
                    .finishReason("grounding_refused")
                    .build();
            AiMessage assistantMessage = saveAssistantMessage(session, promptTemplate, model, refusalResponse, retrieval, request);
            updateSessionOnSuccess(session, request, request.getContent(), refusalResponse.getContent(), retrieval);
            AiCallLog callLog = writeCallLog(session, currentUserId, request, promptTemplate, model,
                    assistantMessage, refusalResponse, start, AiCallLog.RequestStage.GENERATE, retrieval, null);
            persistRetrievalLogsSafely(callLog, request.getContent(), retrieval.getHits());
            List<AiCitationResponse> citations = aiKnowledgeResolver.buildCitations(retrieval.getHits());
            Map<String, Object> retrievalSummary = buildRetrievalSummary(session, retrieval, citations);
            return AiChatTurnResponse.builder()
                    .sessionId(session.getId())
                    .userMessageId(userMessage.getId())
                    .assistantMessageId(assistantMessage.getId())
                    .callLogId(callLog.getId())
                    .content(refusalResponse.getContent())
                    .displayText(refusalResponse.getContent())
                    .finishReason(refusalResponse.getFinishReason())
                    .modelId(model.getId())
                    .modelName(model.getModelName())
                    .promptTemplateId(promptTemplate == null ? null : promptTemplate.getId())
                    .promptTemplateName(promptTemplate == null ? null : promptTemplate.getTemplateName())
                    .sceneCode(responseSceneCode)
                    .actionCode(responseActionCode)
                    .applyTargets(toNullableTargets(applyTargets))
                    .knowledgeBaseIds(retrieval.getKnowledgeBaseIds())
                    .defaultKnowledgeBaseId(session.getDefaultKnowledgeBaseId())
                    .recentKnowledgeBaseId(resolveRecentKnowledgeBaseId(session, retrieval))
                    .analysisMode(retrieval.getMode())
                    .strictGrounding(retrieval.isStrictGrounding())
                    .entryFile(request.getEntryFile())
                    .symbolHint(request.getSymbolHint())
                    .traceDepth(request.getTraceDepth())
                    .groundingStatus(retrieval.getGroundingStatus())
                    .retrievalSummary(retrievalSummary)
                    .citations(citations)
                    .build();
        }
        String providerQuestion = aiKnowledgeResolver.buildKnowledgeAugmentedQuestion(request.getContent(), retrieval.getHits());

        try {
            AiProviderChatResponse providerResponse = provider.chat(AiProviderChatRequest.builder()
                    .model(model)
                    .messages(buildProviderMessages(session.getId(), promptTemplate, providerQuestion))
                    .requestParams(request.getRequestParams())
                    .build());

            AiScenePostProcessor.ProcessedAiResult processed =
                    aiScenePostProcessor.process(responseSceneCode, responseActionCode, providerResponse.getContent());

            AiProviderChatResponse finalResponse = AiProviderChatResponse.builder()
                    .content(StringUtils.hasText(processed.displayText()) ? processed.displayText() : providerResponse.getContent())
                    .promptTokens(providerResponse.getPromptTokens())
                    .completionTokens(providerResponse.getCompletionTokens())
                    .totalTokens(providerResponse.getTotalTokens())
                    .latencyMs(providerResponse.getLatencyMs())
                    .finishReason(providerResponse.getFinishReason())
                    .errorCode(providerResponse.getErrorCode())
                    .build();

            AiMessage assistantMessage = saveAssistantMessage(session, promptTemplate, model, finalResponse, retrieval, request);
            updateSessionOnSuccess(session, request, request.getContent(), finalResponse.getContent(), retrieval);
            AiCallLog callLog = writeCallLog(session, currentUserId, request, promptTemplate, model,
                    assistantMessage, finalResponse, start, AiCallLog.RequestStage.GENERATE, retrieval, null);
            persistRetrievalLogsSafely(callLog, request.getContent(), retrieval.getHits());

            List<AiCitationResponse> citations = aiKnowledgeResolver.buildCitations(retrieval.getHits());
            Map<String, Object> retrievalSummary = buildRetrievalSummary(session, retrieval, citations);
            return AiChatTurnResponse.builder()
                    .sessionId(session.getId())
                    .userMessageId(userMessage.getId())
                    .assistantMessageId(assistantMessage.getId())
                    .callLogId(callLog.getId())
                    .content(finalResponse.getContent())
                    .displayText(finalResponse.getContent())
                    .structured(toNullableMap(processed.structured()))
                    .promptTokens(finalResponse.getPromptTokens())
                    .completionTokens(finalResponse.getCompletionTokens())
                    .totalTokens(finalResponse.getTotalTokens())
                    .latencyMs(finalResponse.getLatencyMs())
                    .finishReason(finalResponse.getFinishReason())
                    .modelId(model.getId())
                    .modelName(model.getModelName())
                    .promptTemplateId(promptTemplate == null ? null : promptTemplate.getId())
                    .promptTemplateName(promptTemplate == null ? null : promptTemplate.getTemplateName())
                    .sceneCode(responseSceneCode)
                    .actionCode(responseActionCode)
                    .applyTargets(toNullableTargets(mergeApplyTargets(processed.applyTargets(), applyTargets)))
                    .knowledgeBaseIds(retrieval.getKnowledgeBaseIds())
                    .defaultKnowledgeBaseId(session.getDefaultKnowledgeBaseId())
                    .recentKnowledgeBaseId(resolveRecentKnowledgeBaseId(session, retrieval))
                    .analysisMode(retrieval.getMode())
                    .strictGrounding(retrieval.isStrictGrounding())
                    .entryFile(request.getEntryFile())
                    .symbolHint(request.getSymbolHint())
                    .traceDepth(request.getTraceDepth())
                    .groundingStatus(retrieval.getGroundingStatus())
                    .retrievalSummary(retrievalSummary)
                    .citations(citations)
                    .build();
        } catch (RuntimeException ex) {
            writeCallLog(session, currentUserId, request, promptTemplate, model, null, null,
                    start, AiCallLog.RequestStage.GENERATE, retrieval, ex);
            throw ex;
        }
    }

    @Override
    public Flux<AiChatStreamChunkResponse> stream(AiChatSendRequest request) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        StreamRuntime runtime = tx.execute(status -> {
            Long currentUserId = currentUserProvider.requireCurrentUserId();
            AiSession session = loadOwnedSession(request.getSessionId(), currentUserId);
            String sceneCode = resolveSceneCode(request, session);
            String actionCode = resolveActionCode(request);
            List<AiStructuredApplyTarget> applyTargets = resolveApplyTargets(sceneCode, actionCode);
            AiAnalysisMode analysisMode = resolveAnalysisMode(request, session);
            boolean strictGrounding = resolveStrictGrounding(request, session);
            AiPromptTemplate promptTemplate = aiPromptResolver.resolve(request.getPromptTemplateId(), sceneCode, session);
            AiModel model = aiModelSelector.select(request.getModelId(), session, promptTemplate);
            AiProvider provider = aiProviderManager.resolve(model);
            AiKnowledgeResolver.RetrievalResult retrieval = retrieveEvidence(session, request, analysisMode, strictGrounding);
            saveUserMessage(session, currentUserId, request, promptTemplate, model);
            AiMessage assistantMessage = createAssistantStreamPlaceholder(session, promptTemplate, model, retrieval, request);
            List<AiProviderMessage> messages = List.of();
            if (!retrieval.isRefused()) {
                String providerQuestion = aiKnowledgeResolver.buildKnowledgeAugmentedQuestion(request.getContent(), retrieval.getHits());
                messages = buildProviderMessages(session.getId(), promptTemplate, providerQuestion);
            }
            List<AiCitationResponse> citations = aiKnowledgeResolver.buildCitations(retrieval.getHits());
            Long recentKnowledgeBaseId = resolveRecentKnowledgeBaseId(session, retrieval);
            Map<String, Object> retrievalSummary = buildRetrievalSummary(session, retrieval, citations);
            return new StreamRuntime(session, promptTemplate, model, provider, retrieval, messages, assistantMessage,
                    Instant.now(), currentUserId, citations, recentKnowledgeBaseId, retrievalSummary,
                    sceneCode, actionCode, applyTargets);
        });

        if (runtime == null) {
            return Flux.error(new IllegalStateException("AI stream runtime init failed"));
        }
        if (runtime.retrieval().isRefused()) {
            AiProviderChatResponse refusalResponse = AiProviderChatResponse.builder()
                    .content(runtime.retrieval().getRefusalMessage())
                    .finishReason("grounding_refused")
                    .build();
            AtomicReference<AiCallLog> callLogRef = new AtomicReference<>();
            tx.execute(status -> {
                AiMessage assistantMessage = finalizeAssistantStreamMessage(runtime.assistantMessage().getId(), refusalResponse,
                        AiMessage.StreamState.FINAL, AiMessage.Status.SUCCESS);
                updateSessionOnSuccess(runtime.session(), request, request.getContent(), refusalResponse.getContent(), runtime.retrieval());
                AiCallLog callLog = writeCallLog(runtime.session(), runtime.currentUserId(), request, runtime.promptTemplate(),
                        runtime.model(), assistantMessage, refusalResponse, runtime.startedAt(), AiCallLog.RequestStage.STREAM,
                        runtime.retrieval(), null);
                persistRetrievalLogsSafely(callLog, request.getContent(), runtime.retrieval().getHits());
                callLogRef.set(callLog);
                return null;
            });
            return Flux.just(AiChatStreamChunkResponse.builder()
                    .sessionId(runtime.session().getId())
                    .assistantMessageId(runtime.assistantMessage().getId())
                    .callLogId(callLogRef.get() == null ? null : callLogRef.get().getId())
                    .delta(refusalResponse.getContent())
                    .finished(true)
                    .finishReason(refusalResponse.getFinishReason())
                    .sceneCode(runtime.sceneCode())
                    .actionCode(runtime.actionCode())
                    .applyTargets(toNullableTargets(runtime.applyTargets()))
                    .knowledgeBaseIds(runtime.retrieval().getKnowledgeBaseIds())
                    .defaultKnowledgeBaseId(runtime.session().getDefaultKnowledgeBaseId())
                    .recentKnowledgeBaseId(runtime.recentKnowledgeBaseId())
                    .analysisMode(runtime.retrieval().getMode())
                    .strictGrounding(runtime.retrieval().isStrictGrounding())
                    .entryFile(request.getEntryFile())
                    .symbolHint(request.getSymbolHint())
                    .traceDepth(request.getTraceDepth())
                    .groundingStatus(runtime.retrieval().getGroundingStatus())
                    .retrievalSummary(runtime.retrievalSummary())
                    .citations(runtime.citations())
                    .build());
        }

        AtomicReference<StringBuilder> contentRef = new AtomicReference<>(new StringBuilder());
        AtomicReference<Integer> promptTokensRef = new AtomicReference<>();
        AtomicReference<Integer> completionTokensRef = new AtomicReference<>();
        AtomicReference<Integer> totalTokensRef = new AtomicReference<>();
        AtomicReference<String> finishReasonRef = new AtomicReference<>();
        AtomicBoolean contextEmitted = new AtomicBoolean(false);
        AtomicBoolean streamFinished = new AtomicBoolean(false);
        AtomicReference<Long> callLogIdRef = new AtomicReference<>();
        AtomicInteger partialSeqRef = new AtomicInteger(runtime.assistantMessage().getPartialSeq() == null ? 0 : runtime.assistantMessage().getPartialSeq());
        AtomicInteger lastPersistedLengthRef = new AtomicInteger(0);
        AtomicReference<Long> lastPersistedAtRef = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Map<String, Object>> structuredRef = new AtomicReference<>();
        AtomicReference<List<AiStructuredApplyTarget>> applyTargetsRef = new AtomicReference<>(toNullableTargets(runtime.applyTargets()));

        return runtime.provider().streamChat(AiProviderChatRequest.builder()
                        .model(runtime.model())
                        .messages(runtime.messages())
                        .requestParams(request.getRequestParams())
                        .build())
                .doOnNext(chunk -> {
                    if (chunk.getDelta() != null) {
                        contentRef.get().append(chunk.getDelta());
                    }
                    if (chunk.getPromptTokens() != null) {
                        promptTokensRef.set(chunk.getPromptTokens());
                    }
                    if (chunk.getCompletionTokens() != null) {
                        completionTokensRef.set(chunk.getCompletionTokens());
                    }
                    if (chunk.getTotalTokens() != null) {
                        totalTokensRef.set(chunk.getTotalTokens());
                    }
                    if (chunk.getFinishReason() != null) {
                        finishReasonRef.set(chunk.getFinishReason());
                    }
                    String partial = contentRef.get().toString();
                    if (!shouldPersistStreamPartial(chunk.getFinished(), partial, lastPersistedLengthRef.get(), lastPersistedAtRef.get())) {
                        return;
                    }
                    int partialSeq = partialSeqRef.incrementAndGet();
                    lastPersistedLengthRef.set(partial.length());
                    lastPersistedAtRef.set(System.currentTimeMillis());
                    tx.execute(status -> {
                        updateAssistantStreamPartial(runtime.assistantMessage().getId(), partial, partialSeq,
                                promptTokensRef.get(), completionTokensRef.get(), totalTokensRef.get(), finishReasonRef.get());
                        return null;
                    });
                    if (Boolean.TRUE.equals(chunk.getFinished())) {
                        tx.execute(status -> {
                            if (!streamFinished.compareAndSet(false, true)) {
                                return null;
                            }
                            StreamFinalizeResult finalizeResult = finalizeSuccessfulStream(runtime, request, contentRef.get().toString(),
                                    promptTokensRef.get(), completionTokensRef.get(), totalTokensRef.get(), finishReasonRef.get());
                            callLogIdRef.set(finalizeResult.callLog().getId());
                            structuredRef.set(toNullableMap(finalizeResult.processed().structured()));
                            applyTargetsRef.set(toNullableTargets(mergeApplyTargets(
                                    finalizeResult.processed().applyTargets(),
                                    runtime.applyTargets()
                            )));
                            return null;
                        });
                    }
                })
                .map(chunk -> AiChatStreamChunkResponse.builder()
                        .sessionId(runtime.session().getId())
                        .assistantMessageId(runtime.assistantMessage().getId())
                        .callLogId(callLogIdRef.get())
                        .delta(chunk.getDelta())
                        .finished(Boolean.TRUE.equals(chunk.getFinished()))
                        .finishReason(chunk.getFinishReason())
                        .sceneCode(runtime.sceneCode())
                        .actionCode(runtime.actionCode())
                        .structured(Boolean.TRUE.equals(chunk.getFinished()) ? structuredRef.get() : null)
                        .applyTargets(applyTargetsRef.get())
                        .knowledgeBaseIds(runtime.retrieval().getKnowledgeBaseIds())
                        .defaultKnowledgeBaseId(runtime.session().getDefaultKnowledgeBaseId())
                        .recentKnowledgeBaseId(runtime.recentKnowledgeBaseId())
                        .analysisMode(runtime.retrieval().getMode())
                        .strictGrounding(runtime.retrieval().isStrictGrounding())
                        .entryFile(request.getEntryFile())
                        .symbolHint(request.getSymbolHint())
                        .traceDepth(request.getTraceDepth())
                        .groundingStatus(runtime.retrieval().getGroundingStatus())
                        .retrievalSummary(runtime.retrievalSummary())
                        .citations(contextEmitted.compareAndSet(false, true) ? runtime.citations() : null)
                        .build())
                .doOnComplete(() -> tx.execute(status -> {
                    if (!streamFinished.compareAndSet(false, true)) {
                        return null;
                    }
                    StreamFinalizeResult finalizeResult = finalizeSuccessfulStream(runtime, request, contentRef.get().toString(),
                            promptTokensRef.get(), completionTokensRef.get(), totalTokensRef.get(), finishReasonRef.get());
                    callLogIdRef.set(finalizeResult.callLog().getId());
                    structuredRef.set(toNullableMap(finalizeResult.processed().structured()));
                    applyTargetsRef.set(toNullableTargets(mergeApplyTargets(
                            finalizeResult.processed().applyTargets(),
                            runtime.applyTargets()
                    )));
                    return null;
                }))
                .doOnError(ex -> tx.execute(status -> {
                    if (!streamFinished.compareAndSet(false, true)) {
                        return null;
                    }
                    RuntimeException streamError = ex instanceof RuntimeException re ? re : new RuntimeException(ex);
                    AiCallLog callLog = finalizeInterruptedStream(runtime, request, contentRef.get().toString(),
                            promptTokensRef.get(), completionTokensRef.get(), totalTokensRef.get(), finishReasonRef.get(), streamError);
                    callLogIdRef.set(callLog.getId());
                    return null;
                }))
                .doFinally(signalType -> {
                    if (signalType != SignalType.CANCEL) {
                        return;
                    }
                    tx.execute(status -> {
                        if (!streamFinished.compareAndSet(false, true)) {
                            return null;
                        }
                        RuntimeException streamError = new IllegalStateException("stream cancelled by client");
                        AiCallLog callLog = finalizeInterruptedStream(runtime, request, contentRef.get().toString(),
                                promptTokensRef.get(), completionTokensRef.get(), totalTokensRef.get(), finishReasonRef.get(), streamError);
                        callLogIdRef.set(callLog.getId());
                        return null;
                    });
                });
    }

    private StreamFinalizeResult finalizeSuccessfulStream(StreamRuntime runtime,
                                                          AiChatSendRequest request,
                                                          String content,
                                                          Integer promptTokens,
                                                          Integer completionTokens,
                                                          Integer totalTokens,
                                                          String finishReason) {
        AiProviderChatResponse rawResponse = AiProviderChatResponse.builder()
                .content(content)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .latencyMs((int) Duration.between(runtime.startedAt(), Instant.now()).toMillis())
                .finishReason(finishReason)
                .build();

        AiScenePostProcessor.ProcessedAiResult processed =
                aiScenePostProcessor.process(runtime.sceneCode(), runtime.actionCode(), rawResponse.getContent());

        AiProviderChatResponse response = AiProviderChatResponse.builder()
                .content(StringUtils.hasText(processed.displayText()) ? processed.displayText() : rawResponse.getContent())
                .promptTokens(rawResponse.getPromptTokens())
                .completionTokens(rawResponse.getCompletionTokens())
                .totalTokens(rawResponse.getTotalTokens())
                .latencyMs(rawResponse.getLatencyMs())
                .finishReason(rawResponse.getFinishReason())
                .build();

        AiMessage assistantMessage = finalizeAssistantStreamMessage(runtime.assistantMessage().getId(), response,
                AiMessage.StreamState.FINAL, AiMessage.Status.SUCCESS);
        updateSessionOnSuccess(runtime.session(), request, request.getContent(), response.getContent(), runtime.retrieval());
        AiCallLog callLog = writeCallLog(runtime.session(), runtime.currentUserId(), request, runtime.promptTemplate(),
                runtime.model(), assistantMessage, response, runtime.startedAt(), AiCallLog.RequestStage.STREAM,
                runtime.retrieval(), null);
        persistRetrievalLogsSafely(callLog, request.getContent(), runtime.retrieval().getHits());
        return new StreamFinalizeResult(callLog, processed);
    }

    private AiCallLog finalizeInterruptedStream(StreamRuntime runtime,
                                                AiChatSendRequest request,
                                                String partial,
                                                Integer promptTokens,
                                                Integer completionTokens,
                                                Integer totalTokens,
                                                String finishReason,
                                                RuntimeException streamError) {
        AiProviderChatResponse interruptedResponse = AiProviderChatResponse.builder()
                .content(partial)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .latencyMs((int) Duration.between(runtime.startedAt(), Instant.now()).toMillis())
                .finishReason(StringUtils.hasText(finishReason) ? finishReason : "interrupted")
                .build();
        AiMessage assistantMessage = finalizeAssistantStreamMessage(runtime.assistantMessage().getId(), interruptedResponse,
                AiMessage.StreamState.CANCELLED, AiMessage.Status.INTERRUPTED);
        updateSessionOnSuccess(runtime.session(), request, request.getContent(), interruptedResponse.getContent(), runtime.retrieval());
        AiCallLog callLog = writeCallLog(runtime.session(), runtime.currentUserId(), request, runtime.promptTemplate(),
                runtime.model(), assistantMessage, interruptedResponse, runtime.startedAt(), AiCallLog.RequestStage.STREAM,
                runtime.retrieval(), streamError);
        persistRetrievalLogsSafely(callLog, request.getContent(), runtime.retrieval().getHits());
        return callLog;
    }

    private AiSession loadOwnedSession(Long sessionId, Long currentUserId) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId must not be null");
        }
        AiSession session = aiSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI session does not exist"));
        if (!Objects.equals(session.getUserId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AI session access denied");
        }
        return session;
    }

    private List<AiProviderMessage> buildProviderMessages(Long sessionId, AiPromptTemplate promptTemplate, String userContent) {
        return aiContextMessageBuilder.build(sessionId, promptTemplate, userContent);
    }

    private AiMessage saveUserMessage(AiSession session,
                                      Long currentUserId,
                                      AiChatSendRequest request,
                                      AiPromptTemplate promptTemplate,
                                      AiModel model) {
        AiMessage message = new AiMessage();
        message.setSession(session);
        message.setRole(AiMessage.Role.USER);
        message.setSenderUserId(currentUserId);
        message.setContent(request.getContent());
        message.setContentTokens(estimateTokens(request.getContent()));
        message.setPromptTemplate(promptTemplate);
        message.setModel(model);
        message.setStatus(AiMessage.Status.SUCCESS);
        message.setCreatedAt(Instant.now());
        return aiMessageRepository.save(message);
    }

    private AiMessage saveAssistantMessage(AiSession session,
                                           AiPromptTemplate promptTemplate,
                                           AiModel model,
                                           AiProviderChatResponse response,
                                           AiKnowledgeResolver.RetrievalResult retrieval,
                                           AiChatSendRequest request) {
        List<KnowledgeRetrievalHit> retrievalHits = retrieval == null ? List.of() : retrieval.getHits();
        List<AiCitationResponse> citations = aiKnowledgeResolver.buildCitations(retrievalHits);
        Map<String, Object> retrievalSummary = buildRetrievalSummary(session, retrieval, citations);
        AiMessage message = new AiMessage();
        message.setSession(session);
        message.setRole(AiMessage.Role.ASSISTANT);
        message.setContent(response.getContent());
        message.setContentTokens(estimateTokens(response.getContent()));
        message.setPromptTemplate(promptTemplate);
        message.setModel(model);
        message.setPromptTokens(response.getPromptTokens());
        message.setCompletionTokens(response.getCompletionTokens());
        message.setTotalTokens(response.getTotalTokens());
        message.setLatencyMs(response.getLatencyMs());
        message.setFinishReason(response.getFinishReason());
        message.setContentFormat(AiMessage.ContentFormat.TEXT);
        message.setStreamState(AiMessage.StreamState.FINAL);
        message.setPartialSeq(0);
        message.setDeltaContent(response.getContent());
        message.setRetrievalSummaryJson(toJson(retrievalSummary));
        message.setGroundingStatus(resolveGroundingStatus(request, retrieval));
        message.setGroundingJson(toJson(buildGroundingReport(request, retrieval)));
        message.setCitationJson(toJson(citations));
        message.setUpdatedAt(Instant.now());
        message.setStatus(AiMessage.Status.SUCCESS);
        message.setCreatedAt(Instant.now());
        if (!retrievalHits.isEmpty()) {
            KnowledgeRetrievalHit topHit = retrievalHits.get(0);
            if (topHit.getKnowledgeBase() != null) {
                message.setKnowledgeBase(topHit.getKnowledgeBase());
            }
            message.setQuotedChunkIds(toJson(retrievalHits.stream()
                    .map(KnowledgeRetrievalHit::getChunkId)
                    .filter(Objects::nonNull)
                    .toList()));
        }
        if (retrieval != null && (!retrieval.getKnowledgeBaseIds().isEmpty() || !retrieval.getHits().isEmpty())) {
            message.setToolCallJson(buildRetrievalContextJson(session, retrieval));
        }
        return aiMessageRepository.save(message);
    }

    private void updateSessionOnSuccess(AiSession session,
                                        AiChatSendRequest request,
                                        String userContent,
                                        String assistantContent,
                                        AiKnowledgeResolver.RetrievalResult retrieval) {
        session.setLastMessageAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        if (request != null && request.getAnalysisMode() != null) {
            session.setAnalysisProfile(request.getAnalysisMode());
        }
        updateSessionKnowledgeContext(session, retrieval);
        session.setLastRetrievalSummaryJson(toJson(buildRetrievalSummary(session, retrieval, aiKnowledgeResolver.buildCitations(
                retrieval == null ? List.of() : retrieval.getHits()))));
        if (session.getMemoryMode() == AiSession.MemoryMode.SUMMARY) {
            session.setSessionSummary(buildSessionSummary(session.getSessionSummary(), userContent, assistantContent));
            session.setSummaryUpdatedAt(Instant.now());
        }
        aiSessionRepository.save(session);
    }

    private void updateSessionKnowledgeContext(AiSession session, AiKnowledgeResolver.RetrievalResult retrieval) {
        if (session == null || retrieval == null || retrieval.getKnowledgeBaseIds().isEmpty()) {
            return;
        }
        Map<String, Object> extConfig = mutableExtConfig(session.getExtConfig());
        Long recentKnowledgeBaseId = resolveRecentKnowledgeBaseId(session, retrieval);
        extConfig.put("lastRetrievalKnowledgeBaseIds", retrieval.getKnowledgeBaseIds());
        if (recentKnowledgeBaseId != null) {
            extConfig.put("recentKnowledgeBaseId", recentKnowledgeBaseId);
        }
        extConfig.put("lastRetrievalUpdatedAt", Instant.now().toString());
        session.setExtConfig(toJson(extConfig));
    }

    private AiCallLog writeCallLog(AiSession session,
                                   Long currentUserId,
                                   AiChatSendRequest request,
                                   AiPromptTemplate promptTemplate,
                                   AiModel model,
                                   AiMessage assistantMessage,
                                   AiProviderChatResponse response,
                                   Instant startedAt,
                                   AiCallLog.RequestStage requestStage,
                                   AiKnowledgeResolver.RetrievalResult retrieval,
                                   RuntimeException ex) {
        AiCallLog log = new AiCallLog();
        log.setUserId(currentUserId);
        log.setBizType(session.getBizType() == null ? null : AiCallLog.BizType.valueOf(session.getBizType().name()));
        log.setBizId(session.getBizId());
        log.setRepositoryId(session.getRepositoryId());
        log.setBranchId(session.getBranchId());
        log.setCommitId(session.getCommitId());
        log.setSession(session);
        log.setMessage(assistantMessage);
        log.setPromptTemplate(promptTemplate);
        log.setAiModel(model);
        log.setRequestType(request.getRequestType() == null ? AiCallLog.RequestType.CHAT : request.getRequestType());
        log.setRequestStage(requestStage);
        log.setRequestText(request.getContent());
        log.setRequestParams(toJson(request.getRequestParams()));
        log.setRetrievalSummaryJson(toJson(resolveCallLogRetrievalSummary(session, request, retrieval)));
        log.setGroundingReportJson(toJson(buildGroundingReport(request, retrieval)));
        log.setMetadataJson(toJson(buildRequestMetadata(request)));
        log.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        log.setCreatedAt(Instant.now());
        if (retrieval != null) {
            log.setDegradeReason(retrieval.getDegradeReason());
        }

        if (response != null) {
            log.setResponseText(response.getContent());
            log.setPromptTokens(response.getPromptTokens());
            log.setCompletionTokens(response.getCompletionTokens());
            log.setTotalTokens(response.getTotalTokens());
            log.setErrorCode(response.getErrorCode());
            log.setCostAmount(calculateCost(model, response.getPromptTokens(), response.getCompletionTokens()));
            if (ex == null) {
                log.setStatus(AiCallLog.Status.SUCCESS);
            } else {
                log.setStatus(AiCallLog.Status.FAILED);
                log.setErrorMessage(ex.getMessage());
            }
        } else {
            log.setStatus(AiCallLog.Status.FAILED);
            if (ex != null) {
                log.setErrorMessage(ex.getMessage());
            }
        }
        return aiCallLogRepository.save(log);
    }

    private void persistRetrievalLogsSafely(AiCallLog callLog, String queryText, List<KnowledgeRetrievalHit> hits) {
        if (callLog == null) {
            return;
        }
        try {
            aiKnowledgeResolver.saveRetrievalLogs(callLog, queryText, hits);
        } catch (Exception ex) {
            log.warn("Skip persisting retrieval logs for callLogId={} due to persistence error: {}",
                    callLog.getId(), ex.getMessage(), ex);
        }
    }

    private String buildRetrievalContextJson(AiSession session, AiKnowledgeResolver.RetrievalResult retrieval) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("type", "knowledge_retrieval");
        context.put("knowledgeBaseIds", retrieval.getKnowledgeBaseIds());
        context.put("defaultKnowledgeBaseId", session.getDefaultKnowledgeBaseId());
        context.put("recentKnowledgeBaseId", resolveRecentKnowledgeBaseId(session, retrieval));
        context.put("topK", retrieval.getTopK());
        context.put("finalContextSource", retrieval.getFinalContextSource());
        context.put("embeddingProfile", buildEmbeddingProfileExplain(retrieval));
        context.put("citations", aiKnowledgeResolver.buildCitations(retrieval.getHits()));
        context.put("evidence", buildEvidenceExplain(retrieval.getHits()));
        return toJson(context);
    }

    private Map<String, Object> buildRetrievalSummary(AiSession session,
                                                      AiKnowledgeResolver.RetrievalResult retrieval,
                                                      List<AiCitationResponse> citations) {
        if (retrieval == null) {
            return null;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("topK", retrieval.getTopK());
        summary.put("mode", retrieval.getMode());
        summary.put("strictGrounding", retrieval.isStrictGrounding());
        summary.put("groundingStatus", retrieval.getGroundingStatus());
        summary.put("refused", retrieval.isRefused());
        summary.put("refusalReason", retrieval.getRefusalReason());
        summary.put("degradeReason", retrieval.getDegradeReason());
        summary.put("degraded", StringUtils.hasText(retrieval.getDegradeReason()));
        summary.put("knowledgeBaseIds", retrieval.getKnowledgeBaseIds());
        summary.put("defaultKnowledgeBaseId", session == null ? null : session.getDefaultKnowledgeBaseId());
        summary.put("recentKnowledgeBaseId", resolveRecentKnowledgeBaseId(session, retrieval));
        summary.put("finalContextSource", retrieval.getFinalContextSource());
        summary.put("rerankProfile", retrieval.getRerankProfile());
        summary.put("embeddingProfile", buildEmbeddingProfileExplain(retrieval));
        summary.put("citations", citations == null ? List.of() : citations);
        Map<String, Object> evidence = buildEvidenceExplain(retrieval.getHits());
        summary.put("evidence", evidence);
        summary.put("declarationHit", evidence.get("declarationHit"));
        summary.put("declarationHitCount", evidence.get("declarationHitCount"));
        summary.put("graphExpanded", evidence.get("graphExpanded"));
        summary.put("graphExpandHitCount", evidence.get("graphExpandHitCount"));
        return summary;
    }

    private GroundingStatus resolveGroundingStatus(AiChatSendRequest request) {
        if (request != null && request.getGroundingStatus() != null) {
            return request.getGroundingStatus();
        }
        return GroundingStatus.NOT_CHECKED;
    }

    private GroundingStatus resolveGroundingStatus(AiChatSendRequest request, AiKnowledgeResolver.RetrievalResult retrieval) {
        if (retrieval != null) {
            return retrieval.getGroundingStatus();
        }
        return resolveGroundingStatus(request);
    }

    private Map<String, Object> buildGroundingReport(AiChatSendRequest request, AiKnowledgeResolver.RetrievalResult retrieval) {
        if (request == null) {
            return null;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("strictGrounding", retrieval == null ? request.getStrictGrounding() : retrieval.isStrictGrounding());
        report.put("groundingStatus", resolveGroundingStatus(request, retrieval));
        if (retrieval != null) {
            report.put("refused", retrieval.isRefused());
            report.put("refusalReason", retrieval.getRefusalReason());
        }
        return report;
    }

    private Map<String, Object> buildRequestMetadata(AiChatSendRequest request) {
        if (request == null) {
            return null;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("analysisMode", request.getAnalysisMode());
        metadata.put("preferredAnalysisMode", request.getPreferredAnalysisMode());
        metadata.put("entryFile", request.getEntryFile());
        metadata.put("symbolHint", request.getSymbolHint());
        metadata.put("traceDepth", request.getTraceDepth());
        metadata.put("sceneCode", request.getSceneCode());
        metadata.put("clientScene", request.getClientScene());
        metadata.put("actionCode", request.getActionCode());
        metadata.put("contextPayload", request.getContextPayload());
        return metadata;
    }

    private Map<String, Object> resolveCallLogRetrievalSummary(AiSession session,
                                                               AiChatSendRequest request,
                                                               AiKnowledgeResolver.RetrievalResult retrieval) {
        if (retrieval != null) {
            return buildRetrievalSummary(session, retrieval, aiKnowledgeResolver.buildCitations(retrieval.getHits()));
        }
        return request == null ? null : request.getRetrievalSummary();
    }

    private AiAnalysisMode resolveAnalysisMode(AiChatSendRequest request, AiSession session) {
        AiAnalysisMode requestMode = request == null ? null : normalizeAnalysisMode(request.getAnalysisMode());
        if (requestMode != null) {
            return requestMode;
        }
        AiAnalysisMode preferredMode = request == null ? null : parseAnalysisMode(request.getPreferredAnalysisMode());
        if (preferredMode != null) {
            return preferredMode;
        }
        Map<String, Object> requestParams = request == null ? null : request.getRequestParams();
        AiAnalysisMode paramMode = parseAnalysisMode(firstValue(requestParams,
                "mode", "analysisMode", "analysis_mode", "analysisProfile", "analysis_profile"));
        if (paramMode != null) {
            return paramMode;
        }
        AiAnalysisMode sessionMode = session == null ? null : normalizeAnalysisMode(session.getAnalysisProfile());
        if (sessionMode == AiAnalysisMode.DOC_QA) {
            return null;
        }
        return sessionMode;
    }

    private boolean resolveStrictGrounding(AiChatSendRequest request, AiSession session) {
        if (request != null && request.getStrictGrounding() != null) {
            return request.getStrictGrounding();
        }
        Map<String, Object> requestParams = request == null ? null : request.getRequestParams();
        Boolean fromRequestParams = parseBooleanValue(firstValue(requestParams,
                "strictGrounding", "strict_grounding", "strictGround", "strict_ground", "strict"));
        if (fromRequestParams != null) {
            return fromRequestParams;
        }
        Boolean fromPolicy = parseStrictGroundingPolicy(session == null ? null : session.getGroundingPolicyJson());
        if (fromPolicy != null) {
            return fromPolicy;
        }
        Boolean fromExtConfig = parseStrictGroundingPolicy(session == null ? null : session.getExtConfig());
        return fromExtConfig != null && fromExtConfig;
    }

    private AiKnowledgeResolver.RetrievalResult retrieveEvidence(AiSession session,
                                                                 AiChatSendRequest request,
                                                                 AiAnalysisMode analysisMode,
                                                                 boolean strictGrounding) {
        if (request != null) {
            request.setAnalysisMode(analysisMode);
            request.setStrictGrounding(strictGrounding);
        }
        AiKnowledgeResolver.RetrievalResult result = aiKnowledgeResolver.retrieve(
                session,
                request == null ? null : request.getContent(),
                request == null ? null : request.getKnowledgeBaseIds(),
                resolveTopK(request),
                analysisMode,
                strictGrounding,
                request == null ? null : request.getEntryFile(),
                request == null ? null : request.getSymbolHint(),
                request == null ? null : request.getTraceDepth(),
                request == null ? null : request.getActionCode()
        );
        if (request != null && result != null) {
            request.setAnalysisMode(result.getMode());
        }
        return result;
    }

    private AiMessage createAssistantStreamPlaceholder(AiSession session,
                                                       AiPromptTemplate promptTemplate,
                                                       AiModel model,
                                                       AiKnowledgeResolver.RetrievalResult retrieval,
                                                       AiChatSendRequest request) {
        AiProviderChatResponse placeholder = AiProviderChatResponse.builder()
                .content("")
                .finishReason("streaming")
                .build();
        AiMessage message = saveAssistantMessage(session, promptTemplate, model, placeholder, retrieval, request);
        message.setStreamState(AiMessage.StreamState.PARTIAL);
        message.setDeltaContent("");
        message.setPartialSeq(0);
        message.setUpdatedAt(Instant.now());
        return aiMessageRepository.save(message);
    }

    private void updateAssistantStreamPartial(Long messageId,
                                              String partialContent,
                                              Integer partialSeq,
                                              Integer promptTokens,
                                              Integer completionTokens,
                                              Integer totalTokens,
                                              String finishReason) {
        if (messageId == null) {
            return;
        }
        aiMessageRepository.findById(messageId).ifPresent(message -> {
            String safeContent = partialContent == null ? "" : partialContent;
            message.setContent(safeContent);
            message.setDeltaContent(safeContent);
            message.setContentTokens(estimateTokens(safeContent));
            message.setPromptTokens(promptTokens);
            message.setCompletionTokens(completionTokens);
            message.setTotalTokens(totalTokens);
            if (StringUtils.hasText(finishReason)) {
                message.setFinishReason(finishReason);
            }
            if (partialSeq != null) {
                message.setPartialSeq(partialSeq);
            }
            message.setStreamState(AiMessage.StreamState.PARTIAL);
            message.setStatus(AiMessage.Status.SUCCESS);
            message.setUpdatedAt(Instant.now());
            aiMessageRepository.save(message);
        });
    }

    private AiMessage finalizeAssistantStreamMessage(Long messageId,
                                                     AiProviderChatResponse response,
                                                     AiMessage.StreamState streamState,
                                                     AiMessage.Status messageStatus) {
        if (messageId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "assistant message id is missing");
        }
        AiMessage message = aiMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "assistant message does not exist"));
        String finalContent = response == null ? message.getContent() :
                (StringUtils.hasText(response.getContent()) ? response.getContent() : message.getContent());
        message.setContent(finalContent == null ? "" : finalContent);
        message.setDeltaContent(message.getContent());
        message.setContentTokens(estimateTokens(message.getContent()));
        if (response != null) {
            if (response.getPromptTokens() != null) {
                message.setPromptTokens(response.getPromptTokens());
            }
            if (response.getCompletionTokens() != null) {
                message.setCompletionTokens(response.getCompletionTokens());
            }
            if (response.getTotalTokens() != null) {
                message.setTotalTokens(response.getTotalTokens());
            }
            message.setLatencyMs(response.getLatencyMs());
            if (StringUtils.hasText(response.getFinishReason())) {
                message.setFinishReason(response.getFinishReason());
            }
        }
        message.setPartialSeq((message.getPartialSeq() == null ? 0 : message.getPartialSeq()) + 1);
        message.setStreamState(streamState == null ? AiMessage.StreamState.FINAL : streamState);
        message.setStatus(messageStatus == null ? AiMessage.Status.SUCCESS : messageStatus);
        message.setUpdatedAt(Instant.now());
        return aiMessageRepository.save(message);
    }

    private boolean shouldPersistStreamPartial(Boolean chunkFinished,
                                               String partialContent,
                                               int lastPersistedLength,
                                               Long lastPersistedAt) {
        String safeContent = partialContent == null ? "" : partialContent;
        int currentLength = safeContent.length();
        if (Boolean.TRUE.equals(chunkFinished)) {
            return true;
        }
        if (currentLength <= lastPersistedLength) {
            return false;
        }
        long now = System.currentTimeMillis();
        long previousPersistAt = lastPersistedAt == null ? 0L : lastPersistedAt;
        return currentLength - lastPersistedLength >= STREAM_PARTIAL_SAVE_CHAR_STEP
                || now - previousPersistAt >= STREAM_PARTIAL_SAVE_INTERVAL_MS;
    }

    private Map<String, Object> buildEvidenceExplain(List<KnowledgeRetrievalHit> hits) {
        List<KnowledgeRetrievalHit> safeHits = hits == null ? List.of() : hits;
        List<KnowledgeRetrievalHit> ordered = safeHits.stream()
                .sorted(Comparator.comparing(KnowledgeRetrievalHit::getRankNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        int declarationHitCount = 0;
        int graphExpandHitCount = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        Map<String, Object> byChunkId = new LinkedHashMap<>();
        for (KnowledgeRetrievalHit hit : ordered) {
            if (isDeclarationHit(hit)) {
                declarationHitCount++;
            }
            if (isGraphExpandHit(hit)) {
                graphExpandHitCount++;
            }
            Map<String, Object> hitReason = readJsonObject(hit.getHitReasonJson());
            Map<String, Object> scoreDetail = readJsonObject(hit.getScoreDetailJson());
            String reason = firstText(hitReason.get("reason"), hit.getPhase());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("chunkId", hit.getChunkId());
            item.put("rankNo", hit.getRankNo());
            item.put("stageCode", hit.getStageCode() == null ? null : hit.getStageCode().name());
            item.put("phase", firstText(hit.getPhase(), hitReason.get("phase")));
            item.put("reason", reason);
            item.put("candidateSource", hit.getCandidateSource() == null ? null : hit.getCandidateSource().name());
            item.put("retrievalMethod", hit.getRetrievalMethod() == null ? null : hit.getRetrievalMethod().name());
            item.put("declarationHit", isDeclarationHit(hit));
            item.put("graphExpanded", isGraphExpandHit(hit));
            item.put("symbolName", hit.getSymbolName());
            item.put("symbolType", hit.getSymbolType());
            item.put("path", hit.getPath());
            item.put("startLine", hit.getStartLine());
            item.put("endLine", hit.getEndLine());
            item.put("score", hit.getScore());
            item.put("keywordScore", hit.getKeywordScore());
            item.put("vectorScore", hit.getVectorScore());
            item.put("graphScore", hit.getGraphScore());
            item.put("scoreDetail", scoreDetail.isEmpty() ? null : scoreDetail);
            details.add(item);
            if (hit.getChunkId() != null) {
                byChunkId.put(String.valueOf(hit.getChunkId()), item);
            }
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("declarationHit", declarationHitCount > 0);
        evidence.put("declarationHitCount", declarationHitCount);
        evidence.put("graphExpanded", graphExpandHitCount > 0);
        evidence.put("graphExpandHitCount", graphExpandHitCount);
        evidence.put("totalHits", ordered.size());
        evidence.put("hits", details);
        evidence.put("byChunkId", byChunkId);
        return evidence;
    }

    private boolean isDeclarationHit(KnowledgeRetrievalHit hit) {
        return hit != null && hit.getStageCode() == AiRetrievalLog.StageCode.DECLARATION_FIRST;
    }

    private boolean isGraphExpandHit(KnowledgeRetrievalHit hit) {
        return hit != null && hit.getStageCode() == AiRetrievalLog.StageCode.GRAPH_EXPAND;
    }

    private AiAnalysisMode normalizeAnalysisMode(AiAnalysisMode mode) {
        if (mode == null) {
            return null;
        }
        return switch (mode) {
            case GENERAL -> AiAnalysisMode.DOC_QA;
            case CODE_ANALYSIS -> AiAnalysisMode.CODE_LOGIC;
            default -> mode;
        };
    }

    private AiAnalysisMode parseAnalysisMode(Object raw) {
        String text = firstText(raw);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim().replace('-', '_').replace(' ', '_').toUpperCase();
        try {
            return normalizeAnalysisMode(AiAnalysisMode.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Map<String, Object> buildEmbeddingProfileExplain(AiKnowledgeResolver.RetrievalResult retrieval) {
        if (retrieval == null || retrieval.getEmbeddingProfile() == null) {
            return null;
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("requestedProvider", retrieval.getEmbeddingProfile().getRequestedProvider());
        profile.put("requestedModelName", retrieval.getEmbeddingProfile().getRequestedModelName());
        profile.put("requestedDimension", retrieval.getEmbeddingProfile().getRequestedDimension());
        profile.put("configuredProvider", retrieval.getEmbeddingProfile().getConfiguredProvider());
        profile.put("configuredModelName", retrieval.getEmbeddingProfile().getConfiguredModelName());
        profile.put("provider", retrieval.getEmbeddingProfile().getProvider());
        profile.put("modelName", retrieval.getEmbeddingProfile().getModelName());
        profile.put("dimension", retrieval.getEmbeddingProfile().getDimension());
        profile.put("batchSize", retrieval.getEmbeddingProfile().getBatchSize());
        profile.put("source", retrieval.getEmbeddingProfile().getSource());
        profile.put("providerSupported", retrieval.getEmbeddingProfile().getProviderSupported());
        profile.put("warning", retrieval.getEmbeddingProfile().getWarning());
        return profile;
    }

    private Boolean parseStrictGroundingPolicy(String rawJson) {
        Map<String, Object> policy = readJsonObject(rawJson);
        if (policy.isEmpty()) {
            return null;
        }
        return parseBooleanValue(firstValue(policy, "strictGrounding", "strict_grounding", "strict"));
    }

    private Map<String, Object> readJsonObject(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Object firstValue(Map<String, Object> source, String... keys) {
        if (source == null || source.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key != null && source.containsKey(key)) {
                Object value = source.get(key);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private String firstText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private Boolean parseBooleanValue(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(raw).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if ("1".equals(text)) {
            return true;
        }
        if ("0".equals(text)) {
            return false;
        }
        return Boolean.parseBoolean(text);
    }

    private Long resolveRecentKnowledgeBaseId(AiSession session, AiKnowledgeResolver.RetrievalResult retrieval) {
        if (retrieval != null) {
            for (KnowledgeRetrievalHit hit : retrieval.getHits()) {
                if (hit.getKnowledgeBaseId() != null) {
                    return hit.getKnowledgeBaseId();
                }
            }
            if (!retrieval.getKnowledgeBaseIds().isEmpty()) {
                return retrieval.getKnowledgeBaseIds().get(0);
            }
        }
        return session == null ? null : session.getDefaultKnowledgeBaseId();
    }

    private BigDecimal calculateCost(AiModel model, Integer promptTokens, Integer completionTokens) {
        if (model == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal inputUnit = model.getCostInputPer1m() == null ? BigDecimal.ZERO : model.getCostInputPer1m();
        BigDecimal outputUnit = model.getCostOutputPer1m() == null ? BigDecimal.ZERO : model.getCostOutputPer1m();
        BigDecimal promptCost = BigDecimal.valueOf(promptTokens == null ? 0 : promptTokens)
                .multiply(inputUnit)
                .divide(BigDecimal.valueOf(1_000_000L), 8, RoundingMode.HALF_UP);
        BigDecimal completionCost = BigDecimal.valueOf(completionTokens == null ? 0 : completionTokens)
                .multiply(outputUnit)
                .divide(BigDecimal.valueOf(1_000_000L), 8, RoundingMode.HALF_UP);
        return promptCost.add(completionCost);
    }

    private Integer estimateTokens(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return Math.max(1, content.length() / 4);
    }

    private String buildSessionSummary(String oldSummary, String userContent, String assistantContent) {
        StringBuilder sb = new StringBuilder();
        if (oldSummary != null && !oldSummary.isBlank()) {
            sb.append(oldSummary).append("\n");
        }
        sb.append("User: ").append(trimForSummary(userContent)).append("\n");
        sb.append("AI: ").append(trimForSummary(assistantContent));
        String result = sb.toString();
        if (result.length() > 3000) {
            return result.substring(result.length() - 3000);
        }
        return result;
    }

    private String trimForSummary(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    private Map<String, Object> mutableExtConfig(String raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(raw)) {
            return result;
        }
        try {
            result.putAll(objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception ignored) {
            result.put("legacyExtConfig", raw);
        }
        return result;
    }

    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return String.valueOf(data);
        }
    }

    private Integer resolveTopK(AiChatSendRequest request) {
        if (request == null || request.getRequestParams() == null) {
            return 5;
        }
        Object value = firstValue(request.getRequestParams(), "topK", "top_k");
        if (value == null) {
            return 5;
        }
        if (value instanceof Number number) {
            return Math.max(1, Math.min(number.intValue(), 10));
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Math.max(1, Math.min(Integer.parseInt(text), 10));
            } catch (NumberFormatException ignored) {
                return 5;
            }
        }
        return 5;
    }

    private String resolveSceneCode(AiChatSendRequest request, AiSession session) {
        String normalized = SCENE_ACTION_CATALOG.normalizeSceneCodeOrNull(
                request == null ? null : request.getSceneCode(),
                request == null ? null : request.getClientScene(),
                request == null ? null : request.getActionCode()
        );
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (session != null && StringUtils.hasText(session.getSceneCode())) {
            return session.getSceneCode();
        }
        return SCENE_ACTION_CATALOG.normalizeSceneCode(
                null,
                null,
                request == null ? null : request.getActionCode(),
                request != null && request.getBizType() != null
                        ? request.getBizType()
                        : (session == null ? null : session.getBizType())
        );
    }

    private String resolveActionCode(AiChatSendRequest request) {
        return SCENE_ACTION_CATALOG.normalizeActionCode(request == null ? null : request.getActionCode());
    }

    private List<AiStructuredApplyTarget> resolveApplyTargets(String sceneCode, String actionCode) {
        return SCENE_ACTION_CATALOG.resolveApplyTargets(sceneCode, actionCode);
    }

    private List<AiStructuredApplyTarget> mergeApplyTargets(List<AiStructuredApplyTarget> primary,
                                                            List<AiStructuredApplyTarget> fallback) {
        List<AiStructuredApplyTarget> safePrimary = primary == null ? List.of() : primary;
        if (!safePrimary.isEmpty()) {
            return safePrimary;
        }
        return fallback == null ? List.of() : fallback;
    }

    private Map<String, Object> toNullableMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    private List<AiStructuredApplyTarget> toNullableTargets(List<AiStructuredApplyTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return null;
        }
        return List.copyOf(targets);
    }

    private record StreamFinalizeResult(
            AiCallLog callLog,
            AiScenePostProcessor.ProcessedAiResult processed
    ) {}

    private record StreamRuntime(
            AiSession session,
            AiPromptTemplate promptTemplate,
            AiModel model,
            AiProvider provider,
            AiKnowledgeResolver.RetrievalResult retrieval,
            List<AiProviderMessage> messages,
            AiMessage assistantMessage,
            Instant startedAt,
            Long currentUserId,
            List<AiCitationResponse> citations,
            Long recentKnowledgeBaseId,
            Map<String, Object> retrievalSummary,
            String sceneCode,
            String actionCode,
            List<AiStructuredApplyTarget> applyTargets
    ) {}
}

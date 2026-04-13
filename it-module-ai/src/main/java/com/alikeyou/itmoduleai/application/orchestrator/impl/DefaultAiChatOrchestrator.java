package com.alikeyou.itmoduleai.application.orchestrator.impl;

import com.alikeyou.itmoduleai.application.orchestrator.AiChatOrchestrator;
import com.alikeyou.itmoduleai.application.support.AiContextMessageBuilder;
import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.application.support.AiKnowledgeResolver;
import com.alikeyou.itmoduleai.application.support.AiModelSelector;
import com.alikeyou.itmoduleai.application.support.AiPromptResolver;
import com.alikeyou.itmoduleai.application.support.AiScenePostProcessor;
import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.dto.response.AiChatStreamChunkResponse;
import com.alikeyou.itmoduleai.dto.response.AiChatTurnResponse;
import com.alikeyou.itmoduleai.dto.response.AiCitationResponse;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAiChatOrchestrator implements AiChatOrchestrator {

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
        AiPromptTemplate promptTemplate = aiPromptResolver.resolve(request.getPromptTemplateId(), request.getSceneCode(), session);
        AiModel model = aiModelSelector.select(request.getModelId(), session, promptTemplate);
        AiProvider provider = aiProviderManager.resolve(model);
        AiKnowledgeResolver.RetrievalResult retrieval = aiKnowledgeResolver.retrieve(
                session,
                request.getContent(),
                request.getKnowledgeBaseIds(),
                resolveTopK(request)
        );

        AiMessage userMessage = saveUserMessage(session, currentUserId, request, promptTemplate, model);
        Instant start = Instant.now();
        String providerQuestion = aiKnowledgeResolver.buildKnowledgeAugmentedQuestion(request.getContent(), retrieval.getHits());

        try {
            AiProviderChatResponse providerResponse = provider.chat(AiProviderChatRequest.builder()
                    .model(model)
                    .messages(buildProviderMessages(session.getId(), promptTemplate, providerQuestion))
                    .requestParams(request.getRequestParams())
                    .build());

            AiScenePostProcessor.ProcessedAiResult processed =
                    aiScenePostProcessor.process(resolveSceneCode(request, session), providerResponse.getContent());

            AiProviderChatResponse finalResponse = AiProviderChatResponse.builder()
                    .content(StringUtils.hasText(processed.displayText()) ? processed.displayText() : providerResponse.getContent())
                    .promptTokens(providerResponse.getPromptTokens())
                    .completionTokens(providerResponse.getCompletionTokens())
                    .totalTokens(providerResponse.getTotalTokens())
                    .latencyMs(providerResponse.getLatencyMs())
                    .finishReason(providerResponse.getFinishReason())
                    .errorCode(providerResponse.getErrorCode())
                    .build();

            AiMessage assistantMessage = saveAssistantMessage(session, promptTemplate, model, finalResponse, retrieval);
            updateSessionOnSuccess(session, request.getContent(), finalResponse.getContent(), retrieval);
            AiCallLog callLog = writeCallLog(session, currentUserId, request, promptTemplate, model, assistantMessage, finalResponse, start, null);
            aiKnowledgeResolver.saveRetrievalLogs(callLog, request.getContent(), retrieval.getHits());

            List<AiCitationResponse> citations = aiKnowledgeResolver.buildCitations(retrieval.getHits());
            return AiChatTurnResponse.builder()
                    .sessionId(session.getId())
                    .userMessageId(userMessage.getId())
                    .assistantMessageId(assistantMessage.getId())
                    .callLogId(callLog.getId())
                    .content(finalResponse.getContent())
                    .displayText(finalResponse.getContent())
                    .structured(processed.structured())
                    .promptTokens(finalResponse.getPromptTokens())
                    .completionTokens(finalResponse.getCompletionTokens())
                    .totalTokens(finalResponse.getTotalTokens())
                    .latencyMs(finalResponse.getLatencyMs())
                    .finishReason(finalResponse.getFinishReason())
                    .modelId(model.getId())
                    .modelName(model.getModelName())
                    .promptTemplateId(promptTemplate == null ? null : promptTemplate.getId())
                    .promptTemplateName(promptTemplate == null ? null : promptTemplate.getTemplateName())
                    .sceneCode(session.getSceneCode())
                    .knowledgeBaseIds(retrieval.getKnowledgeBaseIds())
                    .defaultKnowledgeBaseId(session.getDefaultKnowledgeBaseId())
                    .recentKnowledgeBaseId(resolveRecentKnowledgeBaseId(session, retrieval))
                    .citations(citations)
                    .build();
        } catch (RuntimeException ex) {
            writeCallLog(session, currentUserId, request, promptTemplate, model, null, null, start, ex);
            throw ex;
        }
    }

    @Override
    public Flux<AiChatStreamChunkResponse> stream(AiChatSendRequest request) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        StreamRuntime runtime = tx.execute(status -> {
            Long currentUserId = currentUserProvider.requireCurrentUserId();
            AiSession session = loadOwnedSession(request.getSessionId(), currentUserId);
            AiPromptTemplate promptTemplate = aiPromptResolver.resolve(request.getPromptTemplateId(), request.getSceneCode(), session);
            AiModel model = aiModelSelector.select(request.getModelId(), session, promptTemplate);
            AiProvider provider = aiProviderManager.resolve(model);
            AiKnowledgeResolver.RetrievalResult retrieval = aiKnowledgeResolver.retrieve(
                    session,
                    request.getContent(),
                    request.getKnowledgeBaseIds(),
                    resolveTopK(request)
            );
            saveUserMessage(session, currentUserId, request, promptTemplate, model);
            String providerQuestion = aiKnowledgeResolver.buildKnowledgeAugmentedQuestion(request.getContent(), retrieval.getHits());
            List<AiProviderMessage> messages = buildProviderMessages(session.getId(), promptTemplate, providerQuestion);
            List<AiCitationResponse> citations = aiKnowledgeResolver.buildCitations(retrieval.getHits());
            Long recentKnowledgeBaseId = resolveRecentKnowledgeBaseId(session, retrieval);
            return new StreamRuntime(session, promptTemplate, model, provider, retrieval, messages,
                    Instant.now(), currentUserId, citations, recentKnowledgeBaseId);
        });

        if (runtime == null) {
            return Flux.error(new IllegalStateException("AI stream runtime init failed"));
        }

        AtomicReference<StringBuilder> contentRef = new AtomicReference<>(new StringBuilder());
        AtomicReference<Integer> promptTokensRef = new AtomicReference<>();
        AtomicReference<Integer> completionTokensRef = new AtomicReference<>();
        AtomicReference<Integer> totalTokensRef = new AtomicReference<>();
        AtomicReference<String> finishReasonRef = new AtomicReference<>();
        AtomicBoolean contextEmitted = new AtomicBoolean(false);

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
                })
                .map(chunk -> AiChatStreamChunkResponse.builder()
                        .sessionId(runtime.session().getId())
                        .delta(chunk.getDelta())
                        .finished(Boolean.TRUE.equals(chunk.getFinished()))
                        .finishReason(chunk.getFinishReason())
                        .knowledgeBaseIds(runtime.retrieval().getKnowledgeBaseIds())
                        .defaultKnowledgeBaseId(runtime.session().getDefaultKnowledgeBaseId())
                        .recentKnowledgeBaseId(runtime.recentKnowledgeBaseId())
                        .citations(contextEmitted.compareAndSet(false, true) ? runtime.citations() : null)
                        .build())
                .doOnComplete(() -> tx.execute(status -> {
                    AiProviderChatResponse rawResponse = AiProviderChatResponse.builder()
                            .content(contentRef.get().toString())
                            .promptTokens(promptTokensRef.get())
                            .completionTokens(completionTokensRef.get())
                            .totalTokens(totalTokensRef.get())
                            .latencyMs((int) Duration.between(runtime.startedAt(), Instant.now()).toMillis())
                            .finishReason(finishReasonRef.get())
                            .build();

                    AiScenePostProcessor.ProcessedAiResult processed =
                            aiScenePostProcessor.process(resolveSceneCode(request, runtime.session()), rawResponse.getContent());

                    AiProviderChatResponse response = AiProviderChatResponse.builder()
                            .content(StringUtils.hasText(processed.displayText()) ? processed.displayText() : rawResponse.getContent())
                            .promptTokens(rawResponse.getPromptTokens())
                            .completionTokens(rawResponse.getCompletionTokens())
                            .totalTokens(rawResponse.getTotalTokens())
                            .latencyMs(rawResponse.getLatencyMs())
                            .finishReason(rawResponse.getFinishReason())
                            .build();

                    AiMessage assistantMessage = saveAssistantMessage(runtime.session(), runtime.promptTemplate(), runtime.model(), response, runtime.retrieval());
                    updateSessionOnSuccess(runtime.session(), request.getContent(), response.getContent(), runtime.retrieval());
                    AiCallLog callLog = writeCallLog(runtime.session(), runtime.currentUserId(), request, runtime.promptTemplate(), runtime.model(), assistantMessage, response, runtime.startedAt(), null);
                    aiKnowledgeResolver.saveRetrievalLogs(callLog, request.getContent(), runtime.retrieval().getHits());
                    return null;
                }))
                .doOnError(ex -> tx.execute(status -> {
                    writeCallLog(runtime.session(), runtime.currentUserId(), request, runtime.promptTemplate(), runtime.model(), null, null,
                            runtime.startedAt(), ex instanceof RuntimeException re ? re : new RuntimeException(ex));
                    return null;
                }));
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
                                           AiKnowledgeResolver.RetrievalResult retrieval) {
        List<KnowledgeRetrievalHit> retrievalHits = retrieval == null ? List.of() : retrieval.getHits();
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
                                        String userContent,
                                        String assistantContent,
                                        AiKnowledgeResolver.RetrievalResult retrieval) {
        session.setLastMessageAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        updateSessionKnowledgeContext(session, retrieval);
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
                                   RuntimeException ex) {
        AiCallLog log = new AiCallLog();
        log.setUserId(currentUserId);
        log.setBizType(session.getBizType() == null ? null : AiCallLog.BizType.valueOf(session.getBizType().name()));
        log.setBizId(session.getBizId());
        log.setSession(session);
        log.setMessage(assistantMessage);
        log.setPromptTemplate(promptTemplate);
        log.setAiModel(model);
        log.setRequestType(request.getRequestType() == null ? AiCallLog.RequestType.CHAT : request.getRequestType());
        log.setRequestText(request.getContent());
        log.setRequestParams(toJson(request.getRequestParams()));
        log.setLatencyMs((int) Duration.between(startedAt, Instant.now()).toMillis());
        log.setCreatedAt(Instant.now());

        if (response != null) {
            log.setResponseText(response.getContent());
            log.setPromptTokens(response.getPromptTokens());
            log.setCompletionTokens(response.getCompletionTokens());
            log.setTotalTokens(response.getTotalTokens());
            log.setStatus(AiCallLog.Status.SUCCESS);
            log.setErrorCode(response.getErrorCode());
            log.setCostAmount(calculateCost(model, response.getPromptTokens(), response.getCompletionTokens()));
        } else {
            log.setStatus(AiCallLog.Status.FAILED);
            if (ex != null) {
                log.setErrorMessage(ex.getMessage());
            }
        }
        return aiCallLogRepository.save(log);
    }

    private String buildRetrievalContextJson(AiSession session, AiKnowledgeResolver.RetrievalResult retrieval) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("type", "knowledge_retrieval");
        context.put("knowledgeBaseIds", retrieval.getKnowledgeBaseIds());
        context.put("defaultKnowledgeBaseId", session.getDefaultKnowledgeBaseId());
        context.put("recentKnowledgeBaseId", resolveRecentKnowledgeBaseId(session, retrieval));
        context.put("topK", retrieval.getTopK());
        context.put("citations", aiKnowledgeResolver.buildCitations(retrieval.getHits()));
        return toJson(context);
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
        Object value = request.getRequestParams().get("topK");
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
        if (request != null && StringUtils.hasText(request.getSceneCode())) {
            return request.getSceneCode();
        }
        return session == null ? null : session.getSceneCode();
    }

    private record StreamRuntime(
            AiSession session,
            AiPromptTemplate promptTemplate,
            AiModel model,
            AiProvider provider,
            AiKnowledgeResolver.RetrievalResult retrieval,
            List<AiProviderMessage> messages,
            Instant startedAt,
            Long currentUserId,
            List<AiCitationResponse> citations,
            Long recentKnowledgeBaseId
    ) {}
}

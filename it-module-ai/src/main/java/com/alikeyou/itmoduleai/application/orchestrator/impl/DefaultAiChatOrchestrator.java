package com.alikeyou.itmoduleai.application.orchestrator.impl;

import com.alikeyou.itmoduleai.application.orchestrator.AiChatOrchestrator;
import com.alikeyou.itmoduleai.application.support.AiContextMessageBuilder;
import com.alikeyou.itmoduleai.application.support.AiKnowledgeResolver;
import com.alikeyou.itmoduleai.application.support.AiModelSelector;
import com.alikeyou.itmoduleai.application.support.AiPromptResolver;
import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.dto.response.AiChatStreamChunkResponse;
import com.alikeyou.itmoduleai.dto.response.AiChatTurnResponse;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

    @Override
    @Transactional
    public AiChatTurnResponse chat(AiChatSendRequest request) {
        AiSession session = loadSession(request.getSessionId());
        AiPromptTemplate promptTemplate = aiPromptResolver.resolve(request.getPromptTemplateId(), session);
        AiModel model = aiModelSelector.select(request.getModelId(), session, promptTemplate);
        AiProvider provider = aiProviderManager.resolve(model);
        AiKnowledgeResolver.RetrievalResult retrieval = aiKnowledgeResolver.retrieve(
                session,
                request.getContent(),
                request.getKnowledgeBaseIds(),
                resolveTopK(request)
        );

        AiMessage userMessage = saveUserMessage(session, request, promptTemplate, model);
        Instant start = Instant.now();
        String providerQuestion = aiKnowledgeResolver.buildKnowledgeAugmentedQuestion(request.getContent(), retrieval.getHits());

        try {
            AiProviderChatResponse providerResponse = provider.chat(AiProviderChatRequest.builder()
                    .model(model)
                    .messages(buildProviderMessages(session.getId(), promptTemplate, providerQuestion))
                    .requestParams(request.getRequestParams())
                    .build());

            AiMessage assistantMessage = saveAssistantMessage(session, promptTemplate, model, providerResponse, retrieval.getHits());
            updateSessionOnSuccess(session, request.getContent(), providerResponse.getContent());
            AiCallLog callLog = writeCallLog(session, request, promptTemplate, model, assistantMessage, providerResponse, start, null);
            aiKnowledgeResolver.saveRetrievalLogs(callLog, request.getContent(), retrieval.getHits());

            return AiChatTurnResponse.builder()
                    .sessionId(session.getId())
                    .userMessageId(userMessage.getId())
                    .assistantMessageId(assistantMessage.getId())
                    .content(providerResponse.getContent())
                    .promptTokens(providerResponse.getPromptTokens())
                    .completionTokens(providerResponse.getCompletionTokens())
                    .totalTokens(providerResponse.getTotalTokens())
                    .latencyMs(providerResponse.getLatencyMs())
                    .finishReason(providerResponse.getFinishReason())
                    .modelId(model.getId())
                    .modelName(model.getModelName())
                    .citations(aiKnowledgeResolver.buildCitations(retrieval.getHits()))
                    .build();
        } catch (RuntimeException ex) {
            writeCallLog(session, request, promptTemplate, model, null, null, start, ex);
            throw ex;
        }
    }

    @Override
    public Flux<AiChatStreamChunkResponse> stream(AiChatSendRequest request) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        StreamRuntime runtime = tx.execute(status -> {
            AiSession session = loadSession(request.getSessionId());
            AiPromptTemplate promptTemplate = aiPromptResolver.resolve(request.getPromptTemplateId(), session);
            AiModel model = aiModelSelector.select(request.getModelId(), session, promptTemplate);
            AiProvider provider = aiProviderManager.resolve(model);
            AiKnowledgeResolver.RetrievalResult retrieval = aiKnowledgeResolver.retrieve(
                    session,
                    request.getContent(),
                    request.getKnowledgeBaseIds(),
                    resolveTopK(request)
            );
            saveUserMessage(session, request, promptTemplate, model);
            String providerQuestion = aiKnowledgeResolver.buildKnowledgeAugmentedQuestion(request.getContent(), retrieval.getHits());
            List<AiProviderMessage> messages = buildProviderMessages(session.getId(), promptTemplate, providerQuestion);
            return new StreamRuntime(session, promptTemplate, model, provider, retrieval, messages, Instant.now());
        });

        if (runtime == null) {
            return Flux.error(new IllegalStateException("初始化 AI 流式会话失败"));
        }

        AtomicReference<StringBuilder> contentRef = new AtomicReference<>(new StringBuilder());
        AtomicReference<Integer> promptTokensRef = new AtomicReference<>();
        AtomicReference<Integer> completionTokensRef = new AtomicReference<>();
        AtomicReference<Integer> totalTokensRef = new AtomicReference<>();
        AtomicReference<String> finishReasonRef = new AtomicReference<>();

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
                        .build())
                .doOnComplete(() -> tx.execute(status -> {
                    AiProviderChatResponse response = AiProviderChatResponse.builder()
                            .content(contentRef.get().toString())
                            .promptTokens(promptTokensRef.get())
                            .completionTokens(completionTokensRef.get())
                            .totalTokens(totalTokensRef.get())
                            .latencyMs((int) Duration.between(runtime.startedAt(), Instant.now()).toMillis())
                            .finishReason(finishReasonRef.get())
                            .build();
                    AiMessage assistantMessage = saveAssistantMessage(runtime.session(), runtime.promptTemplate(), runtime.model(), response, runtime.retrieval().getHits());
                    updateSessionOnSuccess(runtime.session(), request.getContent(), response.getContent());
                    AiCallLog callLog = writeCallLog(runtime.session(), request, runtime.promptTemplate(), runtime.model(), assistantMessage, response, runtime.startedAt(), null);
                    aiKnowledgeResolver.saveRetrievalLogs(callLog, request.getContent(), runtime.retrieval().getHits());
                    return null;
                }))
                .doOnError(ex -> tx.execute(status -> {
                    writeCallLog(runtime.session(), request, runtime.promptTemplate(), runtime.model(), null, null, runtime.startedAt(), ex instanceof RuntimeException re ? re : new RuntimeException(ex));
                    return null;
                }));
    }

    private AiSession loadSession(Long sessionId) {
        return aiSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("AI 会话不存在"));
    }

    private List<AiProviderMessage> buildProviderMessages(Long sessionId, AiPromptTemplate promptTemplate, String userContent) {
        return aiContextMessageBuilder.build(sessionId, promptTemplate, userContent);
    }

    private AiMessage saveUserMessage(AiSession session,
                                      AiChatSendRequest request,
                                      AiPromptTemplate promptTemplate,
                                      AiModel model) {
        AiMessage message = new AiMessage();
        message.setSession(session);
        message.setRole(AiMessage.Role.USER);
        message.setSenderUserId(request.getUserId());
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
                                           List<KnowledgeRetrievalHit> retrievalHits) {
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
        if (retrievalHits != null && !retrievalHits.isEmpty()) {
            KnowledgeRetrievalHit topHit = retrievalHits.get(0);
            if (topHit.getKnowledgeBase() != null) {
                message.setKnowledgeBase(topHit.getKnowledgeBase());
            }
            message.setQuotedChunkIds(toJson(retrievalHits.stream().map(KnowledgeRetrievalHit::getChunkId).toList()));
        }
        return aiMessageRepository.save(message);
    }

    private void updateSessionOnSuccess(AiSession session, String userContent, String assistantContent) {
        session.setLastMessageAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        if (session.getMemoryMode() == AiSession.MemoryMode.SUMMARY) {
            session.setSessionSummary(buildSessionSummary(session.getSessionSummary(), userContent, assistantContent));
            session.setSummaryUpdatedAt(Instant.now());
        }
        aiSessionRepository.save(session);
    }

    private AiCallLog writeCallLog(AiSession session,
                                   AiChatSendRequest request,
                                   AiPromptTemplate promptTemplate,
                                   AiModel model,
                                   AiMessage assistantMessage,
                                   AiProviderChatResponse response,
                                   Instant startedAt,
                                   RuntimeException ex) {
        AiCallLog log = new AiCallLog();
        log.setUserId(request.getUserId());
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
        sb.append("用户：").append(trimForSummary(userContent)).append("\n");
        sb.append("AI：").append(trimForSummary(assistantContent));
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

    private record StreamRuntime(
            AiSession session,
            AiPromptTemplate promptTemplate,
            AiModel model,
            AiProvider provider,
            AiKnowledgeResolver.RetrievalResult retrieval,
            List<AiProviderMessage> messages,
            Instant startedAt
    ) {}
}

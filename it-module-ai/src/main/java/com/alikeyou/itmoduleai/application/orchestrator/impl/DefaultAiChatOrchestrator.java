package com.alikeyou.itmoduleai.application.orchestrator.impl;

import com.alikeyou.itmoduleai.application.orchestrator.AiChatOrchestrator;
import com.alikeyou.itmoduleai.application.support.AiContextMessageBuilder;
import com.alikeyou.itmoduleai.application.support.AiKnowledgeResolver;
import com.alikeyou.itmoduleai.application.support.AiModelSelector;
import com.alikeyou.itmoduleai.application.support.AiPromptResolver;
import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.dto.response.AiChatStreamChunkResponse;
import com.alikeyou.itmoduleai.dto.response.AiChatTurnResponse;
import com.alikeyou.itmoduleai.entity.*;
import com.alikeyou.itmoduleai.provider.AiProvider;
import com.alikeyou.itmoduleai.provider.AiProviderManager;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatRequest;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatResponse;
import com.alikeyou.itmoduleai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultAiChatOrchestrator implements AiChatOrchestrator {

    private final AiSessionRepository aiSessionRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final AiModelRepository aiModelRepository;
    private final AiPromptTemplateRepository aiPromptTemplateRepository;
    private final AiProviderManager aiProviderManager;
    private final AiModelSelector aiModelSelector;
    private final AiPromptResolver aiPromptResolver;
    private final AiContextMessageBuilder aiContextMessageBuilder;
    private final AiKnowledgeResolver aiKnowledgeResolver;

    @Override
    @Transactional
    public AiChatTurnResponse chat(AiChatSendRequest request) {
        AiSession session = loadSession(request.getSessionId());
        AiPromptTemplate promptTemplate = aiPromptResolver.resolve(request.getPromptTemplateId(), session);
        AiModel model = aiModelSelector.select(request.getModelId(), session, promptTemplate);
        AiProvider provider = aiProviderManager.resolve(model);
        List<Long> knowledgeBaseIds = aiKnowledgeResolver.resolveKnowledgeBaseIds(session.getId(), request.getKnowledgeBaseIds());

        AiMessage userMessage = saveUserMessage(session, request, promptTemplate, model, knowledgeBaseIds);
        Instant start = Instant.now();
        AiProviderChatResponse providerResponse = null;
        RuntimeException error = null;

        try {
            providerResponse = provider.chat(AiProviderChatRequest.builder()
                    .model(model)
                    .messages(aiContextMessageBuilder.build(session.getId(), promptTemplate, request.getContent()))
                    .requestParams(request.getRequestParams())
                    .build());

            AiMessage assistantMessage = saveAssistantMessage(session, request, promptTemplate, model, knowledgeBaseIds, providerResponse);
            writeCallLog(session, userMessage, assistantMessage, promptTemplate, model, request, providerResponse, start, null);
            updateSession(session);

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
                    .citations(aiKnowledgeResolver.buildPlaceholderCitations(knowledgeBaseIds))
                    .build();
        } catch (RuntimeException ex) {
            error = ex;
            writeCallLog(session, userMessage, null, promptTemplate, model, request, providerResponse, start, ex);
            throw ex;
        }
    }

    @Override
    public Flux<AiChatStreamChunkResponse> stream(AiChatSendRequest request) {
        AiSession session = loadSession(request.getSessionId());
        AiPromptTemplate promptTemplate = aiPromptResolver.resolve(request.getPromptTemplateId(), session);
        AiModel model = aiModelSelector.select(request.getModelId(), session, promptTemplate);
        AiProvider provider = aiProviderManager.resolve(model);

        return provider.streamChat(AiProviderChatRequest.builder()
                        .model(model)
                        .messages(aiContextMessageBuilder.build(session.getId(), promptTemplate, request.getContent()))
                        .requestParams(request.getRequestParams())
                        .build())
                .map(item -> AiChatStreamChunkResponse.builder()
                        .sessionId(session.getId())
                        .delta(item.getDelta())
                        .finished(item.getFinished())
                        .finishReason(item.getFinishReason())
                        .build());
    }

    private AiSession loadSession(Long sessionId) {
        return aiSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("AI 会话不存在"));
    }

    private AiMessage saveUserMessage(AiSession session,
                                      AiChatSendRequest request,
                                      AiPromptTemplate promptTemplate,
                                      AiModel model,
                                      List<Long> knowledgeBaseIds) {
        AiMessage message = new AiMessage();
        message.setSession(session);
        message.setRole(AiMessage.Role.USER);
        message.setSenderUserId(request.getUserId());
        message.setContent(request.getContent());
        message.setPromptTemplate(promptTemplate);
        message.setModel(model);
        message.setQuotedChunkIds(knowledgeBaseIds == null || knowledgeBaseIds.isEmpty() ? null : knowledgeBaseIds.toString());
        message.setStatus(AiMessage.Status.SUCCESS);
        message.setCreatedAt(Instant.now());
        return aiMessageRepository.save(message);
    }

    private AiMessage saveAssistantMessage(AiSession session,
                                           AiChatSendRequest request,
                                           AiPromptTemplate promptTemplate,
                                           AiModel model,
                                           List<Long> knowledgeBaseIds,
                                           AiProviderChatResponse response) {
        AiMessage message = new AiMessage();
        message.setSession(session);
        message.setRole(AiMessage.Role.ASSISTANT);
        message.setContent(response.getContent());
        message.setPromptTemplate(promptTemplate);
        message.setModel(model);
        message.setQuotedChunkIds(knowledgeBaseIds == null || knowledgeBaseIds.isEmpty() ? null : knowledgeBaseIds.toString());
        message.setPromptTokens(response.getPromptTokens());
        message.setCompletionTokens(response.getCompletionTokens());
        message.setTotalTokens(response.getTotalTokens());
        message.setLatencyMs(response.getLatencyMs());
        message.setFinishReason(response.getFinishReason());
        message.setStatus(AiMessage.Status.SUCCESS);
        message.setCreatedAt(Instant.now());
        return aiMessageRepository.save(message);
    }

    private void updateSession(AiSession session) {
        session.setLastMessageAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        aiSessionRepository.save(session);
    }

    private void writeCallLog(AiSession session,
                              AiMessage userMessage,
                              AiMessage assistantMessage,
                              AiPromptTemplate promptTemplate,
                              AiModel model,
                              AiChatSendRequest request,
                              AiProviderChatResponse response,
                              Instant start,
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
        log.setRequestParams(request.getRequestParams() == null ? null : request.getRequestParams().toString());
        log.setCreatedAt(Instant.now());
        log.setLatencyMs((int) Duration.between(start, Instant.now()).toMillis());
        if (response != null) {
            log.setResponseText(response.getContent());
            log.setPromptTokens(response.getPromptTokens());
            log.setCompletionTokens(response.getCompletionTokens());
            log.setTotalTokens(response.getTotalTokens());
            log.setStatus(AiCallLog.Status.SUCCESS);
            log.setCostAmount(calculateCost(model, response.getPromptTokens(), response.getCompletionTokens()));
        } else {
            log.setStatus(AiCallLog.Status.FAILED);
            log.setErrorMessage(ex == null ? null : ex.getMessage());
        }
        aiCallLogRepository.save(log);
    }

    private BigDecimal calculateCost(AiModel model, Integer promptTokens, Integer completionTokens) {
        if (model == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal inputUnit = model.getCostInputPer1m() == null ? BigDecimal.ZERO : model.getCostInputPer1m();
        BigDecimal outputUnit = model.getCostOutputPer1m() == null ? BigDecimal.ZERO : model.getCostOutputPer1m();
        BigDecimal promptCost = BigDecimal.valueOf(promptTokens == null ? 0 : promptTokens).multiply(inputUnit).divide(BigDecimal.valueOf(1_000_000L), 8, java.math.RoundingMode.HALF_UP);
        BigDecimal completionCost = BigDecimal.valueOf(completionTokens == null ? 0 : completionTokens).multiply(outputUnit).divide(BigDecimal.valueOf(1_000_000L), 8, java.math.RoundingMode.HALF_UP);
        return promptCost.add(completionCost);
    }
}

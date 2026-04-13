package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.application.orchestrator.AiChatOrchestrator;
import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionBindKnowledgeBaseRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.dto.response.AiChatStreamChunkResponse;
import com.alikeyou.itmoduleai.dto.response.AiChatTurnResponse;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.service.AiSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.Objects;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatOrchestrator aiChatOrchestrator;
    private final AiSessionService aiSessionService;
    private final AiCurrentUserProvider currentUserProvider;

    @PostMapping("/turn")
    public ApiResponse<AiChatTurnResponse> chat(@RequestBody AiChatSendRequest request) {
        AiChatSendRequest prepared = prepareRequest(request);
        return ApiResponse.ok("sent", aiChatOrchestrator.chat(prepared));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AiChatStreamChunkResponse> stream(@RequestBody AiChatSendRequest request) {
        AiChatSendRequest prepared = prepareRequest(request);
        return aiChatOrchestrator.stream(prepared);
    }

    private AiChatSendRequest prepareRequest(AiChatSendRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body must not be null");
        }
        currentUserProvider.requireCurrentUserId();
        if (!StringUtils.hasText(request.getContent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be blank");
        }
        normalizeAnalysisFields(request);
        if (request.getSessionId() == null) {
            AiSession session = aiSessionService.createSession(buildSessionCreateRequest(request));
            request.setSessionId(session.getId());
        } else if (hasKnowledgeBindingChange(request)) {
            aiSessionService.bindKnowledgeBases(request.getSessionId(), buildBindKnowledgeBaseRequest(request));
        }
        return request;
    }

    private AiSessionCreateRequest buildSessionCreateRequest(AiChatSendRequest request) {
        AiSessionCreateRequest createRequest = new AiSessionCreateRequest();
        createRequest.setBizType(request.getBizType());
        createRequest.setBizId(request.getBizId());
        createRequest.setProjectId(request.getProjectId());
        createRequest.setAnalysisProfile(request.getAnalysisMode());
        createRequest.setSceneCode(resolveSceneCode(request));
        createRequest.setSessionTitle(resolveSessionTitle(request));
        createRequest.setMemoryMode(request.getMemoryMode());
        createRequest.setActiveModelId(request.getModelId());
        createRequest.setPromptTemplateId(request.getPromptTemplateId());
        createRequest.setDefaultKnowledgeBaseId(resolveDefaultKnowledgeBaseId(request));
        createRequest.setKnowledgeBaseIds(request.getKnowledgeBaseIds());
        return createRequest;
    }

    private AiSessionBindKnowledgeBaseRequest buildBindKnowledgeBaseRequest(AiChatSendRequest request) {
        AiSessionBindKnowledgeBaseRequest bindRequest = new AiSessionBindKnowledgeBaseRequest();
        bindRequest.setKnowledgeBaseIds(request.getKnowledgeBaseIds());
        bindRequest.setDefaultKnowledgeBaseId(resolveDefaultKnowledgeBaseId(request));
        return bindRequest;
    }

    private boolean hasKnowledgeBindingChange(AiChatSendRequest request) {
        return request.getDefaultKnowledgeBaseId() != null
                || (request.getKnowledgeBaseIds() != null && request.getKnowledgeBaseIds().stream().anyMatch(Objects::nonNull));
    }

    private String resolveSceneCode(AiChatSendRequest request) {
        if (StringUtils.hasText(request.getSceneCode())) {
            return request.getSceneCode().trim();
        }
        AiSession.BizType bizType = request.getBizType() == null ? AiSession.BizType.GENERAL : request.getBizType();
        return switch (bizType) {
            case GENERAL -> "general.chat";
            case PROJECT -> "project.assistant";
            case BLOG -> "blog.assistant";
            case CIRCLE -> "circle.assistant";
            case PAID_CONTENT -> "paid-content.assistant";
        };
    }

    private String resolveSessionTitle(AiChatSendRequest request) {
        if (StringUtils.hasText(request.getSessionTitle())) {
            return request.getSessionTitle().trim();
        }
        String content = request.getContent();
        if (!StringUtils.hasText(content)) {
            return "New chat";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 30) {
            return normalized;
        }
        return normalized.substring(0, 30);
    }

    private Long resolveDefaultKnowledgeBaseId(AiChatSendRequest request) {
        if (request.getDefaultKnowledgeBaseId() != null) {
            return request.getDefaultKnowledgeBaseId();
        }
        if (request.getKnowledgeBaseIds() == null || request.getKnowledgeBaseIds().isEmpty()) {
            return null;
        }
        return request.getKnowledgeBaseIds().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void normalizeAnalysisFields(AiChatSendRequest request) {
        if (request == null) {
            return;
        }
        request.setAnalysisMode(request.getAnalysisMode());
        request.setStrictGrounding(request.getStrictGrounding());
        request.setEntryFile(trimToNull(request.getEntryFile()));
        request.setSymbolHint(trimToNull(request.getSymbolHint()));
        Integer traceDepth = request.getTraceDepth();
        if (traceDepth != null) {
            request.setTraceDepth(Math.max(0, Math.min(traceDepth, 3)));
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

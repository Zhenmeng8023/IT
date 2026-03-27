package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.application.orchestrator.AiChatOrchestrator;
import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.AiChatSendRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.dto.response.AiChatStreamChunkResponse;
import com.alikeyou.itmoduleai.dto.response.AiChatTurnResponse;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.service.AiSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.Objects;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatOrchestrator aiChatOrchestrator;
    private final AiSessionService aiSessionService;

    @PostMapping("/turn")
    public ApiResponse<AiChatTurnResponse> chat(@RequestBody AiChatSendRequest request) {
        AiChatSendRequest prepared = prepareRequest(request);
        return ApiResponse.ok("发送成功", aiChatOrchestrator.chat(prepared));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AiChatStreamChunkResponse> stream(@RequestBody AiChatSendRequest request) {
        AiChatSendRequest prepared = prepareRequest(request);
        return aiChatOrchestrator.stream(prepared);
    }

    private AiChatSendRequest prepareRequest(AiChatSendRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空");
        }
        if (request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content不能为空");
        }
        if (request.getSessionId() == null) {
            AiSession session = aiSessionService.createSession(buildSessionCreateRequest(request));
            request.setSessionId(session.getId());
        }
        return request;
    }

    private AiSessionCreateRequest buildSessionCreateRequest(AiChatSendRequest request) {
        AiSessionCreateRequest createRequest = new AiSessionCreateRequest();
        createRequest.setUserId(request.getUserId());
        createRequest.setBizType(request.getBizType());
        createRequest.setBizId(request.getBizId());
        createRequest.setProjectId(request.getProjectId());
        createRequest.setSceneCode(resolveSceneCode(request));
        createRequest.setSessionTitle(resolveSessionTitle(request));
        createRequest.setMemoryMode(request.getMemoryMode());
        createRequest.setActiveModelId(request.getModelId());
        createRequest.setPromptTemplateId(request.getPromptTemplateId());
        createRequest.setDefaultKnowledgeBaseId(resolveDefaultKnowledgeBaseId(request));
        return createRequest;
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
            return "新建对话";
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
}
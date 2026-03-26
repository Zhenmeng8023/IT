package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.AiMessageCreateRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionBindKnowledgeBaseRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.entity.AiSessionKnowledgeBase;
import com.alikeyou.itmoduleai.service.AiSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/sessions")
@RequiredArgsConstructor
public class AiSessionController {

    private final AiSessionService aiSessionService;

    @PostMapping
    public ApiResponse<AiSession> create(@RequestBody AiSessionCreateRequest request) {
        return ApiResponse.ok("创建成功", aiSessionService.createSession(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiSession> get(@PathVariable Long id) {
        return ApiResponse.ok(aiSessionService.getById(id));
    }

    @GetMapping
    public ApiResponse<Page<AiSession>> page(@RequestParam Long userId,
                                             @RequestParam(required = false) AiSession.BizType bizType,
                                             Pageable pageable) {
        return ApiResponse.ok(aiSessionService.pageUserSessions(userId, bizType, pageable));
    }

    @PostMapping("/{sessionId}/messages")
    public ApiResponse<AiMessage> createMessage(@PathVariable Long sessionId,
                                                @RequestBody AiMessageCreateRequest request) {
        return ApiResponse.ok("发送成功", aiSessionService.createMessage(sessionId, request));
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<Page<AiMessage>> pageMessages(@PathVariable Long sessionId, Pageable pageable) {
        return ApiResponse.ok(aiSessionService.pageMessages(sessionId, pageable));
    }

    @PutMapping("/{sessionId}/knowledge-bases")
    public ApiResponse<List<AiSessionKnowledgeBase>> bindKnowledgeBases(@PathVariable Long sessionId,
                                                                        @RequestBody AiSessionBindKnowledgeBaseRequest request) {
        return ApiResponse.ok("绑定成功", aiSessionService.bindKnowledgeBases(sessionId, request.getKnowledgeBaseIds()));
    }

    @PutMapping("/{sessionId}/archive")
    public ApiResponse<Void> archive(@PathVariable Long sessionId) {
        aiSessionService.archive(sessionId);
        return ApiResponse.ok("归档成功", null);
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> delete(@PathVariable Long sessionId) {
        aiSessionService.delete(sessionId);
        return ApiResponse.ok("删除成功", null);
    }
}

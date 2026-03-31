package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.AiSessionBindKnowledgeBaseRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.dto.response.AiMessageVO;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.entity.AiSessionKnowledgeBase;
import com.alikeyou.itmoduleai.service.AiSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/ai/sessions")
@RequiredArgsConstructor
public class AiSessionController {

    private final AiSessionService aiSessionService;

    @PostMapping
    public ApiResponse<AiSession> create(@RequestBody AiSessionCreateRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId不能为空");
        }
        return ApiResponse.ok("创建成功", aiSessionService.createSession(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiSession> get(@PathVariable Long id) {
        return ApiResponse.ok(aiSessionService.getById(id));
    }

    @GetMapping
    public ApiResponse<Page<AiSession>> page(@RequestParam Long userId,
                                             @RequestParam(required = false) AiSession.BizType bizType,
                                             @RequestParam(required = false) Long knowledgeBaseId,
                                             @RequestParam(required = false) AiSession.Status status,
                                             Pageable pageable) {
        return ApiResponse.ok(
                aiSessionService.pageUserSessions(userId, bizType, knowledgeBaseId, status, pageable)
        );
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<Page<AiMessageVO>> pageMessages(@PathVariable Long sessionId, Pageable pageable) {
        return ApiResponse.ok(aiSessionService.pageMessages(sessionId, pageable));
    }

    @PutMapping("/{sessionId}/knowledge-bases")
    public ApiResponse<List<AiSessionKnowledgeBase>> bindKnowledgeBases(
            @PathVariable Long sessionId,
            @RequestBody(required = false) AiSessionBindKnowledgeBaseRequest request
    ) {
        List<Long> knowledgeBaseIds = request == null ? List.of() : request.getKnowledgeBaseIds();
        return ApiResponse.ok("绑定成功", aiSessionService.bindKnowledgeBases(sessionId, knowledgeBaseIds));
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

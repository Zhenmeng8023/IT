package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.AiSessionBindKnowledgeBaseRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.dto.response.AiMessageVO;
import com.alikeyou.itmoduleai.dto.response.AiSessionVO;
import com.alikeyou.itmoduleai.entity.AiSession;
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

@RestController
@RequestMapping("/api/ai/sessions")
@RequiredArgsConstructor
public class AiSessionController {

    private final AiSessionService aiSessionService;

    @PostMapping
    public ApiResponse<AiSessionVO> create(@RequestBody AiSessionCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body must not be null");
        }
        AiSession session = aiSessionService.createSession(request);
        return ApiResponse.ok("created", aiSessionService.getSession(session.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiSessionVO> get(@PathVariable Long id) {
        return ApiResponse.ok(aiSessionService.getSession(id));
    }

    @GetMapping
    public ApiResponse<Page<AiSessionVO>> page(@RequestParam(required = false) Long userId,
                                               @RequestParam(required = false) AiSession.BizType bizType,
                                               @RequestParam(required = false) Long knowledgeBaseId,
                                               @RequestParam(required = false) AiSession.Status status,
                                               Pageable pageable) {
        return ApiResponse.ok(
                aiSessionService.pageCurrentUserSessions(bizType, knowledgeBaseId, status, pageable)
        );
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<Page<AiMessageVO>> pageMessages(@PathVariable Long sessionId, Pageable pageable) {
        return ApiResponse.ok(aiSessionService.pageMessages(sessionId, pageable));
    }

    @PutMapping("/{sessionId}/knowledge-bases")
    public ApiResponse<AiSessionVO> bindKnowledgeBases(
            @PathVariable Long sessionId,
            @RequestBody(required = false) AiSessionBindKnowledgeBaseRequest request
    ) {
        return ApiResponse.ok("bound", aiSessionService.bindKnowledgeBases(sessionId, request));
    }

    @PutMapping("/{sessionId}/archive")
    public ApiResponse<Void> archive(@PathVariable Long sessionId) {
        aiSessionService.archive(sessionId);
        return ApiResponse.ok("archived", null);
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> delete(@PathVariable Long sessionId) {
        aiSessionService.delete(sessionId);
        return ApiResponse.ok("deleted", null);
    }
}

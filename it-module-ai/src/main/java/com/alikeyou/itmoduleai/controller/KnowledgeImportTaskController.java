package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/knowledge-import-tasks")
@RequiredArgsConstructor
public class KnowledgeImportTaskController {

    private final KnowledgeImportTaskService knowledgeImportTaskService;
    private final KnowledgeAccessGuard knowledgeAccessGuard;

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeImportTask> getTask(@PathVariable Long taskId) {
        knowledgeAccessGuard.requireImportTaskRead(taskId);
        return ApiResponse.ok(knowledgeImportTaskService.getTask(taskId));
    }

    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<List<KnowledgeImportTask>> listByKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        return ApiResponse.ok(knowledgeImportTaskService.listByKnowledgeBase(knowledgeBaseId));
    }

    @PostMapping("/{taskId}/cancel")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeImportTask> cancelTask(@PathVariable Long taskId) {
        knowledgeAccessGuard.requireImportTaskEdit(taskId);
        return ApiResponse.ok("已发送取消请求", knowledgeImportTaskService.cancelTask(taskId));
    }
}

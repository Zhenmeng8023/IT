package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/knowledge-import-tasks")
@RequiredArgsConstructor
public class KnowledgeImportTaskController {

    private final KnowledgeImportTaskService knowledgeImportTaskService;

    @GetMapping("/{taskId}")
    public ApiResponse<KnowledgeImportTask> getTask(@PathVariable Long taskId) {
        return ApiResponse.ok(knowledgeImportTaskService.getTask(taskId));
    }

    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    public ApiResponse<List<KnowledgeImportTask>> listByKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        return ApiResponse.ok(knowledgeImportTaskService.listByKnowledgeBase(knowledgeBaseId));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<KnowledgeImportTask> cancelTask(@PathVariable Long taskId) {
        return ApiResponse.ok("已发送取消请求", knowledgeImportTaskService.cancelTask(taskId));
    }
}

package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseMemberCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeIndexTaskCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    public ApiResponse<KnowledgeBase> create(@RequestBody KnowledgeBaseCreateRequest request) {
        return ApiResponse.ok("创建成功", knowledgeBaseService.createKnowledgeBase(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBase> update(@PathVariable Long id, @RequestBody KnowledgeBaseCreateRequest request) {
        return ApiResponse.ok("更新成功", knowledgeBaseService.updateKnowledgeBase(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBase> get(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeBaseService.getById(id));
    }

    @GetMapping("/owner/{ownerId}")
    public ApiResponse<Page<KnowledgeBase>> pageByOwner(@PathVariable Long ownerId, Pageable pageable) {
        return ApiResponse.ok(knowledgeBaseService.pageByOwner(ownerId, pageable));
    }

    @GetMapping("/project/{projectId}")
    public ApiResponse<Page<KnowledgeBase>> pageByProject(@PathVariable Long projectId, Pageable pageable) {
        return ApiResponse.ok(knowledgeBaseService.pageByProject(projectId, pageable));
    }

    @PostMapping("/{knowledgeBaseId}/documents")
    public ApiResponse<KnowledgeDocument> addDocument(@PathVariable Long knowledgeBaseId,
                                                      @RequestBody KnowledgeDocumentCreateRequest request) {
        KnowledgeDocument document = knowledgeBaseService.addDocument(knowledgeBaseId, request);
        String message = switch (document.getStatus()) {
            case INDEXED -> "上传并入库完成";
            case FAILED -> "上传完成，但入库失败";
            default -> "上传完成";
        };
        return ApiResponse.ok(message, document);
    }

    @GetMapping("/{knowledgeBaseId}/documents")
    public ApiResponse<Page<KnowledgeDocument>> pageDocuments(@PathVariable Long knowledgeBaseId, Pageable pageable) {
        return ApiResponse.ok(knowledgeBaseService.pageDocuments(knowledgeBaseId, pageable));
    }

    @GetMapping("/documents/{documentId}/chunks")
    public ApiResponse<List<KnowledgeChunk>> listChunks(@PathVariable Long documentId) {
        return ApiResponse.ok(knowledgeBaseService.listChunks(documentId));
    }

    @PostMapping("/{knowledgeBaseId}/members")
    public ApiResponse<KnowledgeBaseMember> addMember(@PathVariable Long knowledgeBaseId,
                                                      @RequestBody KnowledgeBaseMemberCreateRequest request) {
        return ApiResponse.ok("添加成功", knowledgeBaseService.addMember(knowledgeBaseId, request));
    }

    @GetMapping("/{knowledgeBaseId}/members")
    public ApiResponse<List<KnowledgeBaseMember>> listMembers(@PathVariable Long knowledgeBaseId) {
        return ApiResponse.ok(knowledgeBaseService.listMembers(knowledgeBaseId));
    }

    @DeleteMapping("/{knowledgeBaseId}/members/{memberId}")
    public ApiResponse<Void> removeMember(@PathVariable Long knowledgeBaseId, @PathVariable Long memberId) {
        knowledgeBaseService.removeMember(knowledgeBaseId, memberId);
        return ApiResponse.ok("移除成功", null);
    }

    @PostMapping("/{knowledgeBaseId}/index-tasks")
    public ApiResponse<KnowledgeIndexTask> createIndexTask(
            @PathVariable Long knowledgeBaseId,
            @RequestBody(required = false) KnowledgeIndexTaskCreateRequest request) {

        KnowledgeIndexTask task = knowledgeBaseService.createIndexTask(knowledgeBaseId, request);

        String message = switch (task.getStatus()) {
            case SUCCESS -> "任务执行完成";
            case FAILED -> "任务执行失败";
            default -> "任务已创建";
        };

        return ApiResponse.ok(message, task);
    }

    @GetMapping("/{knowledgeBaseId}/index-tasks")
    public ApiResponse<List<KnowledgeIndexTask>> listKnowledgeBaseTasks(@PathVariable Long knowledgeBaseId) {
        return ApiResponse.ok(knowledgeBaseService.listKnowledgeBaseTasks(knowledgeBaseId));
    }

    @GetMapping("/documents/{documentId}/index-tasks")
    public ApiResponse<List<KnowledgeIndexTask>> listDocumentTasks(@PathVariable Long documentId) {
        return ApiResponse.ok(knowledgeBaseService.listDocumentTasks(documentId));
    }
}
package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.common.KnowledgeDocumentBinary;
import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseMemberCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentZipDownloadRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeIndexTaskCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/ai/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeImportTaskService knowledgeImportTaskService;
    private final KnowledgeAccessGuard knowledgeAccessGuard;
    private final AiCurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<KnowledgeBase> create(@RequestBody KnowledgeBaseCreateRequest request) {
        if (request != null && request.getScopeType() == KnowledgeBase.ScopeType.PERSONAL) {
            request.setOwnerId(currentUserProvider.requireCurrentUserId());
        }
        return ApiResponse.ok("创建成功", knowledgeBaseService.createKnowledgeBase(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<KnowledgeBase> update(@PathVariable Long id, @RequestBody KnowledgeBaseCreateRequest request) {
        knowledgeAccessGuard.requireKnowledgeBaseEdit(id);
        return ApiResponse.ok("更新成功", knowledgeBaseService.updateKnowledgeBase(id, request));
    }

    @GetMapping("/my")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<Page<KnowledgeBase>> pageMine(Pageable pageable) {
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        return ApiResponse.ok(knowledgeAccessGuard.pageKnowledgeBasesByOwner(currentUserId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<KnowledgeBase> get(@PathVariable Long id) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(id);
        return ApiResponse.ok(knowledgeBaseService.getById(id));
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<Page<KnowledgeBase>> pageByOwner(@PathVariable Long ownerId, Pageable pageable) {
        return ApiResponse.ok(knowledgeAccessGuard.pageKnowledgeBasesByOwner(ownerId, pageable));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<Page<KnowledgeBase>> pageByProject(@PathVariable Long projectId, Pageable pageable) {
        return ApiResponse.ok(knowledgeAccessGuard.pageKnowledgeBasesByProject(projectId, pageable));
    }

    @PostMapping("/{knowledgeBaseId}/documents")
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<KnowledgeDocument> addDocument(@PathVariable Long knowledgeBaseId, @RequestBody KnowledgeDocumentCreateRequest request) {
        knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        KnowledgeDocument document = knowledgeBaseService.addDocument(knowledgeBaseId, request);
        return ApiResponse.ok(resolveDocumentMessage(document), document);
    }

    @PostMapping(value = "/{knowledgeBaseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<List<KnowledgeDocument>> uploadDocuments(
            @PathVariable Long knowledgeBaseId,
            @RequestPart("files") MultipartFile[] files,
            @ModelAttribute KnowledgeDocumentCreateRequest request
    ) {
        knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        List<KnowledgeDocument> documents = knowledgeBaseService.uploadDocuments(
                knowledgeBaseId,
                files == null ? List.of() : Arrays.asList(files),
                request
        );
        long successCount = documents.stream()
                .filter(item -> item.getStatus() == KnowledgeDocument.Status.INDEXED || item.getStatus() == KnowledgeDocument.Status.UPLOADED)
                .count();
        long failedCount = documents.stream()
                .filter(item -> item.getStatus() == KnowledgeDocument.Status.FAILED)
                .count();
        String message = failedCount > 0
                ? String.format("上传完成，成功 %d 个，失败 %d 个", successCount, failedCount)
                : String.format("上传完成，共 %d 个文件", documents.size());
        return ApiResponse.ok(message, documents);
    }

    @PostMapping(value = "/{knowledgeBaseId}/documents/upload-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<KnowledgeImportTask> uploadZip(
            @PathVariable Long knowledgeBaseId,
            @RequestPart("file") MultipartFile file,
            @ModelAttribute KnowledgeDocumentCreateRequest request
    ) {
        knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        KnowledgeImportTask task = knowledgeImportTaskService.createZipImportTask(knowledgeBaseId, file, request);
        return ApiResponse.ok("ZIP 已接收，开始后台导入", task);
    }

    @GetMapping("/{knowledgeBaseId}/documents")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<Page<KnowledgeDocument>> pageDocuments(@PathVariable Long knowledgeBaseId, Pageable pageable) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.pageDocuments(knowledgeBaseId, pageable));
    }

    @GetMapping("/documents/{documentId}/chunks")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<List<KnowledgeChunk>> listChunks(@PathVariable Long documentId) {
        knowledgeAccessGuard.requireDocumentRead(documentId);
        return ApiResponse.ok(knowledgeBaseService.listChunks(documentId));
    }

    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        knowledgeAccessGuard.requireDocumentRead(documentId);
        KnowledgeDocumentBinary binary = knowledgeBaseService.downloadDocument(documentId);
        return buildBinaryResponse(binary);
    }

    @PostMapping("/{knowledgeBaseId}/documents/download-zip")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ResponseEntity<byte[]> downloadDocumentsZip(
            @PathVariable Long knowledgeBaseId,
            @RequestBody(required = false) KnowledgeDocumentZipDownloadRequest request
    ) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        List<Long> documentIds = request == null ? null : request.getDocumentIds();
        KnowledgeDocumentBinary binary = knowledgeBaseService.downloadDocumentsZip(knowledgeBaseId, documentIds);
        return buildBinaryResponse(binary);
    }

    @PostMapping("/{knowledgeBaseId}/members")
    @PreAuthorize("@aiPermissionGuard.canManageFrontKnowledgeBaseMember()")
    public ApiResponse<KnowledgeBaseMember> addMember(@PathVariable Long knowledgeBaseId, @RequestBody KnowledgeBaseMemberCreateRequest request) {
        knowledgeAccessGuard.requireKnowledgeBaseOwner(knowledgeBaseId);
        return ApiResponse.ok("添加成功", knowledgeBaseService.addMember(knowledgeBaseId, request));
    }

    @GetMapping("/{knowledgeBaseId}/members")
    @PreAuthorize("@aiPermissionGuard.canManageFrontKnowledgeBaseMember()")
    public ApiResponse<List<KnowledgeBaseMember>> listMembers(@PathVariable Long knowledgeBaseId) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.listMembers(knowledgeBaseId));
    }

    @DeleteMapping("/{knowledgeBaseId}/members/{memberId}")
    @PreAuthorize("@aiPermissionGuard.canManageFrontKnowledgeBaseMember()")
    public ApiResponse<Void> removeMember(@PathVariable Long knowledgeBaseId, @PathVariable Long memberId) {
        knowledgeAccessGuard.requireKnowledgeBaseOwner(knowledgeBaseId);
        knowledgeBaseService.removeMember(knowledgeBaseId, memberId);
        return ApiResponse.ok("移除成功", null);
    }

    @PostMapping("/{knowledgeBaseId}/index-tasks")
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<KnowledgeIndexTask> createIndexTask(
            @PathVariable Long knowledgeBaseId,
            @RequestBody(required = false) KnowledgeIndexTaskCreateRequest request
    ) {
        knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        KnowledgeIndexTask task = knowledgeBaseService.createIndexTask(knowledgeBaseId, request);
        String message = switch (task.getStatus()) {
            case SUCCESS -> "任务执行完成";
            case FAILED -> "任务执行失败";
            default -> "任务已创建";
        };
        return ApiResponse.ok(message, task);
    }

    @GetMapping("/{knowledgeBaseId}/index-tasks")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<List<KnowledgeIndexTask>> listKnowledgeBaseTasks(@PathVariable Long knowledgeBaseId) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.listKnowledgeBaseTasks(knowledgeBaseId));
    }

    @GetMapping("/documents/{documentId}/index-tasks")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<List<KnowledgeIndexTask>> listDocumentTasks(@PathVariable Long documentId) {
        knowledgeAccessGuard.requireDocumentRead(documentId);
        return ApiResponse.ok(knowledgeBaseService.listDocumentTasks(documentId));
    }

    private String resolveDocumentMessage(KnowledgeDocument document) {
        if (document == null || document.getStatus() == null) {
            return "上传完成";
        }
        return switch (document.getStatus()) {
            case INDEXED -> "上传并入库完成";
            case FAILED -> "上传完成，但入库失败";
            default -> "上传完成";
        };
    }

    private ResponseEntity<byte[]> buildBinaryResponse(KnowledgeDocumentBinary binary) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(binary.getContentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(binary.getFileName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(binary.getContent().length);
        return ResponseEntity.ok().headers(headers).body(binary.getContent());
    }
}

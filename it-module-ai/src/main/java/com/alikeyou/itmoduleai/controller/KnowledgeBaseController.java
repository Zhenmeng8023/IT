package com.alikeyou.itmoduleai.controller;

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
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
        return ApiResponse.ok(resolveDocumentMessage(document), document);
    }

    @PostMapping(value = "/{knowledgeBaseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<KnowledgeDocument>> uploadDocuments(@PathVariable Long knowledgeBaseId,
                                                                @RequestPart("files") MultipartFile[] files,
                                                                @ModelAttribute KnowledgeDocumentCreateRequest request) {
        List<KnowledgeDocument> documents = knowledgeBaseService.uploadDocuments(
                knowledgeBaseId,
                files == null ? List.of() : Arrays.asList(files),
                request
        );
        long successCount = documents.stream()
                .filter(item -> item.getStatus() == KnowledgeDocument.Status.INDEXED
                        || item.getStatus() == KnowledgeDocument.Status.UPLOADED)
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
    public ApiResponse<List<KnowledgeDocument>> uploadZipDocuments(@PathVariable Long knowledgeBaseId,
                                                                   @RequestPart("file") MultipartFile file,
                                                                   @ModelAttribute KnowledgeDocumentCreateRequest request) {
        List<KnowledgeDocument> documents = knowledgeBaseService.uploadZipDocuments(knowledgeBaseId, file, request);
        long successCount = documents.stream()
                .filter(item -> item.getStatus() == KnowledgeDocument.Status.INDEXED
                        || item.getStatus() == KnowledgeDocument.Status.UPLOADED)
                .count();
        long failedCount = documents.stream()
                .filter(item -> item.getStatus() == KnowledgeDocument.Status.FAILED)
                .count();
        String message = failedCount > 0
                ? String.format("ZIP 导入完成，成功 %d 个，失败 %d 个", successCount, failedCount)
                : String.format("ZIP 导入完成，共 %d 个文件", documents.size());
        return ApiResponse.ok(message, documents);
    }

    @GetMapping("/{knowledgeBaseId}/documents")
    public ApiResponse<Page<KnowledgeDocument>> pageDocuments(@PathVariable Long knowledgeBaseId, Pageable pageable) {
        return ApiResponse.ok(knowledgeBaseService.pageDocuments(knowledgeBaseId, pageable));
    }

    @GetMapping("/documents/{documentId}/chunks")
    public ApiResponse<List<KnowledgeChunk>> listChunks(@PathVariable Long documentId) {
        return ApiResponse.ok(knowledgeBaseService.listChunks(documentId));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        KnowledgeDocumentBinary binary = knowledgeBaseService.downloadDocument(documentId);
        return buildBinaryResponse(binary);
    }

    @PostMapping("/{knowledgeBaseId}/documents/download-zip")
    public ResponseEntity<byte[]> downloadDocumentsZip(@PathVariable Long knowledgeBaseId,
                                                       @RequestBody(required = false) KnowledgeDocumentZipDownloadRequest request) {
        List<Long> documentIds = request == null ? null : request.getDocumentIds();
        KnowledgeDocumentBinary binary = knowledgeBaseService.downloadDocumentsZip(knowledgeBaseId, documentIds);
        return buildBinaryResponse(binary);
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
    public ApiResponse<KnowledgeIndexTask> createIndexTask(@PathVariable Long knowledgeBaseId,
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
        return ResponseEntity.ok()
                .headers(headers)
                .body(binary.getContent());
    }
}
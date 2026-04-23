package com.alikeyou.itmoduleai.controller.front;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.common.KnowledgeDocumentBinary;
import com.alikeyou.itmoduleai.dto.front.request.FrontKnowledgeBaseMemberCreateRequest;
import com.alikeyou.itmoduleai.dto.front.request.FrontKnowledgeBaseUpsertRequest;
import com.alikeyou.itmoduleai.dto.front.request.FrontKnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.dto.front.response.FrontKnowledgeBaseMemberResponse;
import com.alikeyou.itmoduleai.dto.front.response.FrontKnowledgeBaseResponse;
import com.alikeyou.itmoduleai.dto.front.response.FrontKnowledgeDocumentResponse;
import com.alikeyou.itmoduleai.dto.front.response.FrontKnowledgeImportTaskResponse;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentZipDownloadRequest;
import com.alikeyou.itmoduleai.dto.response.KnowledgeEmbeddingStatusResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.security.AiPermissionGuard;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/ai/front/knowledge-bases")
@RequiredArgsConstructor
public class FrontKnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeImportTaskService knowledgeImportTaskService;
    private final KnowledgeAccessGuard knowledgeAccessGuard;
    private final AiCurrentUserProvider currentUserProvider;
    private final AiPermissionGuard aiPermissionGuard;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;

    @PostMapping
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<FrontKnowledgeBaseResponse> create(@RequestBody FrontKnowledgeBaseUpsertRequest request) {
        requireRequestBody(request, "knowledge base request is required");
        KnowledgeBase.ScopeType targetScope = resolveCreateScope(request);
        aiPermissionGuard.requireFrontKnowledgeBaseCreate(targetScope);
        KnowledgeBase knowledgeBase = knowledgeBaseService.createKnowledgeBase(
                request.toServiceRequest(currentUserProvider.requireCurrentUserId())
        );
        return ApiResponse.ok("Created successfully", FrontKnowledgeBaseResponse.from(knowledgeBase));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<FrontKnowledgeBaseResponse> update(@PathVariable Long id,
                                                          @RequestBody FrontKnowledgeBaseUpsertRequest request) {
        requireRequestBody(request, "knowledge base request is required");
        KnowledgeBase existing = requireFrontEditableKnowledgeBase(id);
        KnowledgeBase.ScopeType targetScope = resolveUpdateScope(existing, request);
        aiPermissionGuard.requireFrontKnowledgeBaseEdit(targetScope);
        KnowledgeBase updated = knowledgeBaseService.updateKnowledgeBase(id, request.toServiceRequest(null));
        return ApiResponse.ok("Updated successfully", FrontKnowledgeBaseResponse.from(updated));
    }

    @GetMapping("/my")
    @PreAuthorize("@aiPermissionGuard.canReadMyFrontKnowledgeBase()")
    public ApiResponse<Page<FrontKnowledgeBaseResponse>> pageMine(Pageable pageable) {
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        Page<KnowledgeBase> page = knowledgeAccessGuard.pageKnowledgeBasesByOwner(currentUserId, pageable);
        return ApiResponse.ok(mapKnowledgeBasePage(page));
    }

    @GetMapping("/projects/{projectId}")
    @PreAuthorize("@aiPermissionGuard.canReadProjectFrontKnowledgeBase()")
    public ApiResponse<Page<FrontKnowledgeBaseResponse>> pageByProject(@PathVariable Long projectId, Pageable pageable) {
        Page<KnowledgeBase> page = knowledgeAccessGuard.pageKnowledgeBasesByProject(projectId, pageable);
        return ApiResponse.ok(mapKnowledgeBasePage(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<FrontKnowledgeBaseResponse> get(@PathVariable Long id) {
        KnowledgeBase knowledgeBase = requireFrontReadableKnowledgeBase(id);
        return ApiResponse.ok(FrontKnowledgeBaseResponse.from(knowledgeBase));
    }

    @PostMapping("/{knowledgeBaseId}/documents")
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<FrontKnowledgeDocumentResponse> addDocument(@PathVariable Long knowledgeBaseId,
                                                                   @RequestBody FrontKnowledgeDocumentCreateRequest request) {
        requireRequestBody(request, "document request is required");
        requireFrontEditableKnowledgeBase(knowledgeBaseId);
        KnowledgeDocument document = knowledgeBaseService.addDocument(
                knowledgeBaseId,
                request.toServiceRequest(currentUserProvider.requireCurrentUserId())
        );
        return ApiResponse.ok(resolveDocumentMessage(document), FrontKnowledgeDocumentResponse.from(document));
    }

    @PostMapping(value = "/{knowledgeBaseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<List<FrontKnowledgeDocumentResponse>> uploadDocuments(@PathVariable Long knowledgeBaseId,
                                                                             @RequestPart("files") MultipartFile[] files,
                                                                             @ModelAttribute FrontKnowledgeDocumentCreateRequest request) {
        requireFrontEditableKnowledgeBase(knowledgeBaseId);
        List<KnowledgeDocument> documents = knowledgeBaseService.uploadDocuments(
                knowledgeBaseId,
                files == null ? List.of() : List.of(files),
                request.toServiceRequest(currentUserProvider.requireCurrentUserId())
        );
        return ApiResponse.ok(resolveUploadMessage(documents), documents.stream().map(FrontKnowledgeDocumentResponse::from).toList());
    }

    @PostMapping(value = "/{knowledgeBaseId}/documents/upload-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@aiPermissionGuard.canEditFrontKnowledgeBase()")
    public ApiResponse<FrontKnowledgeImportTaskResponse> uploadZip(@PathVariable Long knowledgeBaseId,
                                                                   @RequestPart("file") MultipartFile file,
                                                                   @ModelAttribute FrontKnowledgeDocumentCreateRequest request) {
        requireFrontEditableKnowledgeBase(knowledgeBaseId);
        KnowledgeImportTask task = knowledgeImportTaskService.createZipImportTask(
                knowledgeBaseId,
                file,
                request.toServiceRequest(currentUserProvider.requireCurrentUserId())
        );
        return ApiResponse.ok("ZIP import accepted", FrontKnowledgeImportTaskResponse.from(task));
    }

    @GetMapping("/{knowledgeBaseId}/documents")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<Page<FrontKnowledgeDocumentResponse>> pageDocuments(@PathVariable Long knowledgeBaseId, Pageable pageable) {
        requireFrontReadableKnowledgeBase(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.pageDocuments(knowledgeBaseId, pageable).map(FrontKnowledgeDocumentResponse::from));
    }

    @GetMapping("/documents/{documentId}/chunks")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<List<KnowledgeChunk>> listChunks(@PathVariable Long documentId) {
        requireFrontReadableDocument(documentId);
        return ApiResponse.ok(knowledgeBaseService.listChunks(documentId));
    }

    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        requireFrontReadableDocument(documentId);
        KnowledgeDocumentBinary binary = knowledgeBaseService.downloadDocument(documentId);
        return buildBinaryResponse(binary);
    }

    @PostMapping("/{knowledgeBaseId}/documents/download-zip")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ResponseEntity<byte[]> downloadDocumentsZip(
            @PathVariable Long knowledgeBaseId,
            @RequestBody(required = false) KnowledgeDocumentZipDownloadRequest request
    ) {
        requireFrontReadableKnowledgeBase(knowledgeBaseId);
        List<Long> documentIds = request == null ? null : request.getDocumentIds();
        KnowledgeDocumentBinary binary = knowledgeBaseService.downloadDocumentsZip(knowledgeBaseId, documentIds);
        return buildBinaryResponse(binary);
    }

    @GetMapping("/{knowledgeBaseId}/import-tasks")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<List<FrontKnowledgeImportTaskResponse>> listImportTasks(@PathVariable Long knowledgeBaseId) {
        requireFrontReadableKnowledgeBase(knowledgeBaseId);
        return ApiResponse.ok(knowledgeImportTaskService.listByKnowledgeBase(knowledgeBaseId).stream()
                .map(FrontKnowledgeImportTaskResponse::from)
                .toList());
    }

    @GetMapping("/import-tasks/{taskId}")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<FrontKnowledgeImportTaskResponse> getImportTask(@PathVariable Long taskId) {
        KnowledgeImportTask task = requireFrontReadableImportTask(taskId);
        return ApiResponse.ok(FrontKnowledgeImportTaskResponse.from(task));
    }

    @GetMapping("/{knowledgeBaseId}/index-tasks")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<List<KnowledgeIndexTask>> listKnowledgeBaseTasks(@PathVariable Long knowledgeBaseId) {
        requireFrontReadableKnowledgeBase(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.listKnowledgeBaseTasks(knowledgeBaseId));
    }

    @GetMapping("/documents/{documentId}/index-tasks")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<List<KnowledgeIndexTask>> listDocumentTasks(@PathVariable Long documentId) {
        requireFrontReadableDocument(documentId);
        return ApiResponse.ok(knowledgeBaseService.listDocumentTasks(documentId));
    }

    @GetMapping("/{knowledgeBaseId}/embedding-status")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<KnowledgeEmbeddingStatusResponse> getKnowledgeBaseEmbeddingStatus(@PathVariable Long knowledgeBaseId) {
        requireFrontReadableKnowledgeBase(knowledgeBaseId);
        return ApiResponse.ok(knowledgeEmbeddingService.getKnowledgeBaseEmbeddingStatus(knowledgeBaseId));
    }

    @GetMapping("/documents/{documentId}/embedding-status")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<KnowledgeEmbeddingStatusResponse> getDocumentEmbeddingStatus(@PathVariable Long documentId) {
        requireFrontReadableDocument(documentId);
        return ApiResponse.ok(knowledgeEmbeddingService.getDocumentEmbeddingStatus(documentId));
    }

    @PostMapping("/{knowledgeBaseId}/members")
    @PreAuthorize("@aiPermissionGuard.canManageFrontKnowledgeBaseMember()")
    public ApiResponse<FrontKnowledgeBaseMemberResponse> addMember(@PathVariable Long knowledgeBaseId,
                                                                   @RequestBody FrontKnowledgeBaseMemberCreateRequest request) {
        KnowledgeBase knowledgeBase = requireFrontOwnerKnowledgeBase(knowledgeBaseId);
        validateFrontMemberRequest(request);
        KnowledgeBaseMember member = knowledgeBaseService.addMember(knowledgeBaseId, request.toServiceRequest());
        return ApiResponse.ok("Member added", FrontKnowledgeBaseMemberResponse.from(member));
    }

    @GetMapping("/{knowledgeBaseId}/members")
    @PreAuthorize("@aiPermissionGuard.canReadFrontKnowledgeBase()")
    public ApiResponse<List<FrontKnowledgeBaseMemberResponse>> listMembers(@PathVariable Long knowledgeBaseId) {
        requireFrontReadableKnowledgeBase(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.listMembers(knowledgeBaseId).stream()
                .map(FrontKnowledgeBaseMemberResponse::from)
                .toList());
    }

    @DeleteMapping("/{knowledgeBaseId}/members/{memberId}")
    @PreAuthorize("@aiPermissionGuard.canManageFrontKnowledgeBaseMember()")
    public ApiResponse<Void> removeMember(@PathVariable Long knowledgeBaseId, @PathVariable Long memberId) {
        requireFrontOwnerKnowledgeBase(knowledgeBaseId);
        knowledgeBaseService.removeMember(knowledgeBaseId, memberId);
        return ApiResponse.ok("Member removed", null);
    }

    private KnowledgeBase requireFrontReadableKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        aiPermissionGuard.requireFrontKnowledgeBaseRead(knowledgeBase.getScopeType());
        return knowledgeBase;
    }

    private KnowledgeBase requireFrontEditableKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        aiPermissionGuard.requireFrontKnowledgeBaseEdit(knowledgeBase.getScopeType());
        return knowledgeBase;
    }

    private KnowledgeBase requireFrontOwnerKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseOwner(knowledgeBaseId);
        aiPermissionGuard.requireFrontKnowledgeBaseMemberManage(knowledgeBase.getScopeType());
        return knowledgeBase;
    }

    private KnowledgeDocument requireFrontReadableDocument(Long documentId) {
        KnowledgeDocument document = knowledgeAccessGuard.requireDocumentRead(documentId);
        aiPermissionGuard.requireFrontKnowledgeBaseRead(document.getKnowledgeBase().getScopeType());
        return document;
    }

    private KnowledgeImportTask requireFrontReadableImportTask(Long taskId) {
        KnowledgeImportTask task = knowledgeAccessGuard.requireImportTaskRead(taskId);
        aiPermissionGuard.requireFrontKnowledgeBaseRead(task.getKnowledgeBase().getScopeType());
        return task;
    }

    private KnowledgeBase.ScopeType resolveCreateScope(FrontKnowledgeBaseUpsertRequest request) {
        KnowledgeBase.ScopeType scopeType = request == null || request.getScopeType() == null
                ? KnowledgeBase.ScopeType.PERSONAL
                : request.getScopeType();
        validateFrontScope(scopeType);
        return scopeType;
    }

    private KnowledgeBase.ScopeType resolveUpdateScope(KnowledgeBase existing, FrontKnowledgeBaseUpsertRequest request) {
        KnowledgeBase.ScopeType scopeType = request == null || request.getScopeType() == null
                ? existing.getScopeType()
                : request.getScopeType();
        validateFrontScope(scopeType);
        return scopeType;
    }

    private void validateFrontScope(KnowledgeBase.ScopeType scopeType) {
        if (scopeType == KnowledgeBase.ScopeType.PLATFORM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Front knowledge base API does not support platform scope");
        }
    }

    private void validateFrontMemberRequest(FrontKnowledgeBaseMemberCreateRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (request.getRoleCode() == KnowledgeBaseMember.RoleCode.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Front knowledge base API does not support assigning OWNER role");
        }
    }

    private void requireRequestBody(Object request, String message) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private Page<FrontKnowledgeBaseResponse> mapKnowledgeBasePage(Page<KnowledgeBase> page) {
        List<KnowledgeBase> filtered = page.getContent().stream()
                .filter(item -> item.getScopeType() != KnowledgeBase.ScopeType.PLATFORM)
                .toList();
        long filteredOut = page.getContent().size() - filtered.size();
        List<FrontKnowledgeBaseResponse> content = filtered.stream()
                .map(FrontKnowledgeBaseResponse::from)
                .toList();
        long totalElements = filteredOut == 0 ? page.getTotalElements() : Math.max(content.size(), page.getTotalElements() - filteredOut);
        return new PageImpl<>(content, page.getPageable(), totalElements);
    }

    private String resolveDocumentMessage(KnowledgeDocument document) {
        if (document == null || document.getStatus() == null) {
            return "File accepted, indexing is in progress";
        }
        return switch (document.getStatus()) {
            case FAILED -> "File accepted, but indexing failed";
            default -> "File accepted, indexing is in progress";
        };
    }

    private String resolveUploadMessage(List<KnowledgeDocument> documents) {
        long acceptedCount = documents == null ? 0 : documents.stream()
                .filter(item -> item.getStatus() != KnowledgeDocument.Status.FAILED)
                .count();
        long failedCount = documents == null ? 0 : documents.stream()
                .filter(item -> item.getStatus() == KnowledgeDocument.Status.FAILED)
                .count();
        if (acceptedCount > 0 && failedCount == 0) {
            return "Files accepted, indexing tasks created";
        }
        if (failedCount > 0) {
            return String.format("Files accepted, indexing tasks created for %d file(s), %d failed", acceptedCount, failedCount);
        }
        return "Files accepted, indexing is in progress";
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

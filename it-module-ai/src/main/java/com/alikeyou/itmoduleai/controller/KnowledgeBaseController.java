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
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeImportTaskService knowledgeImportTaskService;

    @PostMapping
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeBase> create(@RequestBody KnowledgeBaseCreateRequest request) {
        if (request != null && request.getScopeType() == KnowledgeBase.ScopeType.PERSONAL) {
            request.setOwnerId(resolveCurrentUserId());
        }
        return ApiResponse.ok("创建成功", knowledgeBaseService.createKnowledgeBase(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeBase> update(@PathVariable Long id, @RequestBody KnowledgeBaseCreateRequest request) {
        assertCanEdit(id);
        return ApiResponse.ok("更新成功", knowledgeBaseService.updateKnowledgeBase(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeBase> get(@PathVariable Long id) {
        assertCanRead(id);
        return ApiResponse.ok(knowledgeBaseService.getById(id));
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<Page<KnowledgeBase>> pageByOwner(@PathVariable Long ownerId, Pageable pageable) {
        Long effectiveOwnerId = ownerId;
        if (!hasAuthority("view:ai:log") && !hasAuthority("view:ai:model-admin") && !hasAuthority("view:ai:prompt-template")) {
            Long currentUserId = resolveCurrentUserId();
            if (ownerId == null || !currentUserId.equals(ownerId)) {
                effectiveOwnerId = currentUserId;
            }
        }
        return ApiResponse.ok(knowledgeBaseService.pageByOwner(effectiveOwnerId, pageable));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<Page<KnowledgeBase>> pageByProject(@PathVariable Long projectId, Pageable pageable) {
        return ApiResponse.ok(knowledgeBaseService.pageByProject(projectId, pageable));
    }

    @PostMapping("/{knowledgeBaseId}/documents")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeDocument> addDocument(@PathVariable Long knowledgeBaseId, @RequestBody KnowledgeDocumentCreateRequest request) {
        assertCanEdit(knowledgeBaseId);
        KnowledgeDocument document = knowledgeBaseService.addDocument(knowledgeBaseId, request);
        return ApiResponse.ok(resolveDocumentMessage(document), document);
    }

    @PostMapping(value = "/{knowledgeBaseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<List<KnowledgeDocument>> uploadDocuments(
            @PathVariable Long knowledgeBaseId,
            @RequestPart("files") MultipartFile[] files,
            @ModelAttribute KnowledgeDocumentCreateRequest request
    ) {
        assertCanEdit(knowledgeBaseId);
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
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeImportTask> uploadZip(
            @PathVariable Long knowledgeBaseId,
            @RequestPart("file") MultipartFile file,
            @ModelAttribute KnowledgeDocumentCreateRequest request
    ) {
        assertCanEdit(knowledgeBaseId);
        KnowledgeImportTask task = knowledgeImportTaskService.createZipImportTask(knowledgeBaseId, file, request);
        return ApiResponse.ok("ZIP 已接收，开始后台导入", task);
    }

    @GetMapping("/{knowledgeBaseId}/documents")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<Page<KnowledgeDocument>> pageDocuments(@PathVariable Long knowledgeBaseId, Pageable pageable) {
        assertCanRead(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.pageDocuments(knowledgeBaseId, pageable));
    }

    @GetMapping("/documents/{documentId}/chunks")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<List<KnowledgeChunk>> listChunks(@PathVariable Long documentId) {
        return ApiResponse.ok(knowledgeBaseService.listChunks(documentId));
    }

    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        KnowledgeDocumentBinary binary = knowledgeBaseService.downloadDocument(documentId);
        return buildBinaryResponse(binary);
    }

    @PostMapping("/{knowledgeBaseId}/documents/download-zip")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ResponseEntity<byte[]> downloadDocumentsZip(
            @PathVariable Long knowledgeBaseId,
            @RequestBody(required = false) KnowledgeDocumentZipDownloadRequest request
    ) {
        assertCanRead(knowledgeBaseId);
        List<Long> documentIds = request == null ? null : request.getDocumentIds();
        KnowledgeDocumentBinary binary = knowledgeBaseService.downloadDocumentsZip(knowledgeBaseId, documentIds);
        return buildBinaryResponse(binary);
    }

    @PostMapping("/{knowledgeBaseId}/members")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeBaseMember> addMember(@PathVariable Long knowledgeBaseId, @RequestBody KnowledgeBaseMemberCreateRequest request) {
        assertCanManageMembers(knowledgeBaseId);
        return ApiResponse.ok("添加成功", knowledgeBaseService.addMember(knowledgeBaseId, request));
    }

    @GetMapping("/{knowledgeBaseId}/members")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<List<KnowledgeBaseMember>> listMembers(@PathVariable Long knowledgeBaseId) {
        assertCanRead(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.listMembers(knowledgeBaseId));
    }

    @DeleteMapping("/{knowledgeBaseId}/members/{memberId}")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<Void> removeMember(@PathVariable Long knowledgeBaseId, @PathVariable Long memberId) {
        assertCanManageMembers(knowledgeBaseId);
        knowledgeBaseService.removeMember(knowledgeBaseId, memberId);
        return ApiResponse.ok("移除成功", null);
    }

    @PostMapping("/{knowledgeBaseId}/index-tasks")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeIndexTask> createIndexTask(
            @PathVariable Long knowledgeBaseId,
            @RequestBody(required = false) KnowledgeIndexTaskCreateRequest request
    ) {
        assertCanEdit(knowledgeBaseId);
        KnowledgeIndexTask task = knowledgeBaseService.createIndexTask(knowledgeBaseId, request);
        String message = switch (task.getStatus()) {
            case SUCCESS -> "任务执行完成";
            case FAILED -> "任务执行失败";
            default -> "任务已创建";
        };
        return ApiResponse.ok(message, task);
    }

    @GetMapping("/{knowledgeBaseId}/index-tasks")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<List<KnowledgeIndexTask>> listKnowledgeBaseTasks(@PathVariable Long knowledgeBaseId) {
        assertCanRead(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.listKnowledgeBaseTasks(knowledgeBaseId));
    }

    @GetMapping("/documents/{documentId}/index-tasks")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<List<KnowledgeIndexTask>> listDocumentTasks(@PathVariable Long documentId) {
        return ApiResponse.ok(knowledgeBaseService.listDocumentTasks(documentId));
    }

    private void assertCanRead(Long knowledgeBaseId) {
        KnowledgeBaseMember.RoleCode roleCode = resolveRole(knowledgeBaseId, resolveCurrentUserId());
        if (roleCode == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有知识库成员才能访问该知识库");
        }
    }

    private void assertCanEdit(Long knowledgeBaseId) {
        KnowledgeBaseMember.RoleCode roleCode = resolveRole(knowledgeBaseId, resolveCurrentUserId());
        if (roleCode == KnowledgeBaseMember.RoleCode.OWNER || roleCode == KnowledgeBaseMember.RoleCode.EDITOR) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有知识库 OWNER / EDITOR 才能执行该操作");
    }

    private void assertCanManageMembers(Long knowledgeBaseId) {
        KnowledgeBaseMember.RoleCode roleCode = resolveRole(knowledgeBaseId, resolveCurrentUserId());
        if (roleCode == KnowledgeBaseMember.RoleCode.OWNER) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有知识库 OWNER 才能管理成员");
    }

    private KnowledgeBaseMember.RoleCode resolveRole(Long knowledgeBaseId, Long userId) {
        if (knowledgeBaseId == null || userId == null) {
            return null;
        }
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(knowledgeBaseId);
        if (knowledgeBase != null && userId.equals(knowledgeBase.getOwnerId())) {
            return KnowledgeBaseMember.RoleCode.OWNER;
        }
        List<KnowledgeBaseMember> members = knowledgeBaseService.listMembers(knowledgeBaseId);
        if (members == null) {
            return null;
        }
        for (KnowledgeBaseMember member : members) {
            if (member != null && userId.equals(member.getUserId())) {
                return member.getRoleCode();
            }
        }
        return null;
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authority == null || authority.isBlank()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return false;
        }
        for (GrantedAuthority grantedAuthority : authorities) {
            if (grantedAuthority != null && authority.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录状态已失效");
        }
        Long userId = extractUserId(authentication.getPrincipal());
        if (userId == null) {
            userId = extractUserId(authentication.getDetails());
        }
        if (userId == null) {
            userId = parseLong(authentication.getName());
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法识别当前登录用户");
        }
        return userId;
    }

    private Long extractUserId(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Number number) {
            return number.longValue();
        }
        if (source instanceof CharSequence sequence) {
            return parseLong(sequence.toString());
        }
        if (source instanceof Map<?, ?> map) {
            for (String key : List.of("id", "userId", "uid")) {
                Object value = map.get(key);
                Long parsed = extractUserId(value);
                if (parsed != null) {
                    return parsed;
                }
            }
            return null;
        }
        for (String methodName : List.of("getId", "getUserId", "getUid")) {
            try {
                Method method = source.getClass().getMethod(methodName);
                Object value = method.invoke(source);
                Long parsed = extractUserId(value);
                if (parsed != null) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
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

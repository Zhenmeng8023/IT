package com.alikeyou.itmoduleai.controller.admin;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.application.support.AiKnowledgeResolver;
import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.admin.request.AdminPlatformKnowledgeBaseCreateRequest;
import com.alikeyou.itmoduleai.dto.admin.request.AdminPlatformKnowledgeBaseUpdateRequest;
import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.common.KnowledgeDocumentBinary;
import com.alikeyou.itmoduleai.dto.request.KnowledgeChunkPreviewRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentZipDownloadRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeEmbeddingBackfillRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeIndexTaskCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeSearchDebugRequest;
import com.alikeyou.itmoduleai.dto.response.EmbeddingProfileView;
import com.alikeyou.itmoduleai.dto.response.KnowledgeChunkPreviewResponse;
import com.alikeyou.itmoduleai.dto.response.KnowledgeEmbeddingStatusResponse;
import com.alikeyou.itmoduleai.dto.response.KnowledgeSearchDebugResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
import com.alikeyou.itmoduleai.service.policy.KnowledgeBaseScopePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
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
@RequestMapping("/api/admin/ai/platform-knowledge-bases")
@RequiredArgsConstructor
@PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
public class AdminPlatformKnowledgeBaseController {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeImportTaskService knowledgeImportTaskService;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;
    private final KnowledgeChunkingService knowledgeChunkingService;
    private final AiKnowledgeResolver aiKnowledgeResolver;
    private final KnowledgeAccessGuard knowledgeAccessGuard;
    private final KnowledgeBaseScopePolicy knowledgeBaseScopePolicy;
    private final AiCurrentUserProvider currentUserProvider;

    @GetMapping
    public ApiResponse<Page<KnowledgeBase>> page(Pageable pageable) {
        return ApiResponse.ok(knowledgeBaseRepository.findByScopeTypeOrderByUpdatedAtDesc(KnowledgeBase.ScopeType.PLATFORM, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBase> get(@PathVariable Long id) {
        return ApiResponse.ok(requirePlatformReadableKnowledgeBase(id));
    }

    @PostMapping
    public ApiResponse<KnowledgeBase> create(@RequestBody AdminPlatformKnowledgeBaseCreateRequest request) {
        requireRequestBody(request, "knowledge base request is required");
        KnowledgeBase created = knowledgeBaseService.createKnowledgeBase(request.toServiceRequest());
        return ApiResponse.ok("Created successfully", knowledgeBaseScopePolicy.requirePlatformScope(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBase> update(@PathVariable Long id,
                                             @RequestBody AdminPlatformKnowledgeBaseUpdateRequest request) {
        requireRequestBody(request, "knowledge base request is required");
        requirePlatformEditableKnowledgeBase(id);
        KnowledgeBase updated = knowledgeBaseService.updateKnowledgeBase(id, request.toServiceRequest());
        return ApiResponse.ok("Updated successfully", knowledgeBaseScopePolicy.requirePlatformScope(updated));
    }

    @PostMapping("/{knowledgeBaseId}/documents")
    public ApiResponse<KnowledgeDocument> addDocument(@PathVariable Long knowledgeBaseId,
                                                      @RequestBody KnowledgeDocumentCreateRequest request) {
        requireRequestBody(request, "document request is required");
        requirePlatformEditableKnowledgeBase(knowledgeBaseId);
        KnowledgeDocument document = knowledgeBaseService.addDocument(knowledgeBaseId, normalizeDocumentRequest(request));
        return ApiResponse.ok(resolveDocumentMessage(document), document);
    }

    @PostMapping(value = "/{knowledgeBaseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<KnowledgeDocument>> uploadDocuments(@PathVariable Long knowledgeBaseId,
                                                                @RequestPart("files") MultipartFile[] files,
                                                                @ModelAttribute KnowledgeDocumentCreateRequest request) {
        requirePlatformEditableKnowledgeBase(knowledgeBaseId);
        List<KnowledgeDocument> documents = knowledgeBaseService.uploadDocuments(
                knowledgeBaseId,
                files == null ? List.of() : List.of(files),
                normalizeDocumentRequest(request)
        );
        long acceptedCount = documents.stream().filter(item -> item.getStatus() != KnowledgeDocument.Status.FAILED).count();
        long failedCount = documents.stream().filter(item -> item.getStatus() == KnowledgeDocument.Status.FAILED).count();
        if (failedCount == 0) {
            return ApiResponse.ok("Files accepted, indexing tasks created", documents);
        }
        return ApiResponse.ok(String.format("Files accepted, indexing tasks created for %d file(s), %d failed", acceptedCount, failedCount), documents);
    }

    @PostMapping(value = "/{knowledgeBaseId}/documents/upload-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KnowledgeImportTask> uploadZip(@PathVariable Long knowledgeBaseId,
                                                      @RequestPart("file") MultipartFile file,
                                                      @ModelAttribute KnowledgeDocumentCreateRequest request) {
        requirePlatformEditableKnowledgeBase(knowledgeBaseId);
        KnowledgeImportTask task = knowledgeImportTaskService.createZipImportTask(
                knowledgeBaseId,
                file,
                normalizeDocumentRequest(request)
        );
        return ApiResponse.ok("ZIP import accepted", task);
    }

    @GetMapping("/{knowledgeBaseId}/documents")
    public ApiResponse<Page<KnowledgeDocument>> pageDocuments(@PathVariable Long knowledgeBaseId, Pageable pageable) {
        requirePlatformReadableKnowledgeBase(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.pageDocuments(knowledgeBaseId, pageable));
    }

    @GetMapping("/import-tasks/{taskId}")
    public ApiResponse<KnowledgeImportTask> getImportTask(@PathVariable Long taskId) {
        KnowledgeImportTask task = knowledgeAccessGuard.requireImportTaskRead(taskId);
        knowledgeBaseScopePolicy.requirePlatformScope(task.getKnowledgeBase());
        return ApiResponse.ok(task);
    }

    @GetMapping("/documents/{documentId}/chunks")
    public ApiResponse<List<KnowledgeChunk>> listChunks(@PathVariable Long documentId) {
        requirePlatformReadableDocument(documentId);
        return ApiResponse.ok(knowledgeBaseService.listChunks(documentId));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        requirePlatformReadableDocument(documentId);
        KnowledgeDocumentBinary binary = knowledgeBaseService.downloadDocument(documentId);
        return buildBinaryResponse(binary);
    }

    @PostMapping("/{knowledgeBaseId}/documents/download-zip")
    public ResponseEntity<byte[]> downloadDocumentsZip(@PathVariable Long knowledgeBaseId,
                                                       @RequestBody(required = false) KnowledgeDocumentZipDownloadRequest request) {
        requirePlatformReadableKnowledgeBase(knowledgeBaseId);
        List<Long> documentIds = request == null ? null : request.getDocumentIds();
        KnowledgeDocumentBinary binary = knowledgeBaseService.downloadDocumentsZip(knowledgeBaseId, documentIds);
        return buildBinaryResponse(binary);
    }

    @GetMapping("/{knowledgeBaseId}/index-tasks")
    public ApiResponse<List<KnowledgeIndexTask>> listKnowledgeBaseTasks(@PathVariable Long knowledgeBaseId) {
        requirePlatformReadableKnowledgeBase(knowledgeBaseId);
        return ApiResponse.ok(knowledgeBaseService.listKnowledgeBaseTasks(knowledgeBaseId));
    }

    @PostMapping("/{knowledgeBaseId}/index-tasks")
    public ApiResponse<KnowledgeIndexTask> createIndexTask(@PathVariable Long knowledgeBaseId,
                                                           @RequestBody(required = false) KnowledgeIndexTaskCreateRequest request) {
        requirePlatformEditableKnowledgeBase(knowledgeBaseId);
        KnowledgeIndexTask task = knowledgeBaseService.createIndexTask(knowledgeBaseId, request);
        String message = switch (task.getStatus()) {
            case SUCCESS -> "Task completed";
            case FAILED -> "Task failed";
            default -> "Index task accepted";
        };
        return ApiResponse.ok(message, task);
    }

    @GetMapping("/documents/{documentId}/index-tasks")
    public ApiResponse<List<KnowledgeIndexTask>> listDocumentTasks(@PathVariable Long documentId) {
        requirePlatformReadableDocument(documentId);
        return ApiResponse.ok(knowledgeBaseService.listDocumentTasks(documentId));
    }

    @GetMapping("/{knowledgeBaseId}/embedding-status")
    public ApiResponse<KnowledgeEmbeddingStatusResponse> getKnowledgeBaseEmbeddingStatus(@PathVariable Long knowledgeBaseId) {
        requirePlatformReadableKnowledgeBase(knowledgeBaseId);
        return ApiResponse.ok(knowledgeEmbeddingService.getKnowledgeBaseEmbeddingStatus(knowledgeBaseId));
    }

    @GetMapping("/documents/{documentId}/embedding-status")
    public ApiResponse<KnowledgeEmbeddingStatusResponse> getDocumentEmbeddingStatus(@PathVariable Long documentId) {
        requirePlatformReadableDocument(documentId);
        return ApiResponse.ok(knowledgeEmbeddingService.getDocumentEmbeddingStatus(documentId));
    }

    @PostMapping("/{knowledgeBaseId}/embedding-backfill")
    public ApiResponse<KnowledgeEmbeddingStatusResponse> backfillKnowledgeBaseEmbeddings(@PathVariable Long knowledgeBaseId,
                                                                                          @RequestBody(required = false) KnowledgeEmbeddingBackfillRequest request) {
        requirePlatformEditableKnowledgeBase(knowledgeBaseId);
        KnowledgeEmbeddingStatusResponse result = knowledgeEmbeddingService.backfillKnowledgeBaseEmbeddings(
                knowledgeBaseId,
                request == null ? null : request.getProvider(),
                request == null ? null : request.getModelName(),
                request == null ? null : request.getDimension()
        );
        return ApiResponse.ok("Embedding backfill completed", result);
    }

    @PostMapping("/documents/{documentId}/embedding-backfill")
    public ApiResponse<KnowledgeEmbeddingStatusResponse> backfillDocumentEmbeddings(@PathVariable Long documentId,
                                                                                     @RequestBody(required = false) KnowledgeEmbeddingBackfillRequest request) {
        requirePlatformEditableDocument(documentId);
        KnowledgeEmbeddingStatusResponse result = knowledgeEmbeddingService.backfillDocumentEmbeddings(
                documentId,
                request == null ? null : request.getProvider(),
                request == null ? null : request.getModelName(),
                request == null ? null : request.getDimension()
        );
        return ApiResponse.ok("Embedding backfill completed", result);
    }

    @PostMapping("/documents/{documentId}/chunk-preview")
    public ApiResponse<List<KnowledgeChunkPreviewResponse>> previewDocumentChunks(@PathVariable Long documentId,
                                                                                  @RequestBody(required = false) KnowledgeChunkPreviewRequest request) {
        KnowledgeDocument document = requirePlatformReadableDocument(documentId);
        KnowledgeBase knowledgeBase = knowledgeBaseScopePolicy.requirePlatformScope(document.getKnowledgeBase());
        List<KnowledgeChunkPreviewResponse> preview = knowledgeChunkingService.previewChunks(
                knowledgeBase,
                document,
                StringUtils.hasText(request == null ? null : request.getText()) ? request.getText() : document.getContentText(),
                request == null ? new KnowledgeChunkPreviewRequest() : request
        );
        return ApiResponse.ok("Chunk preview completed", preview);
    }

    @PostMapping("/{knowledgeBaseId}/search-debug")
    public ApiResponse<KnowledgeSearchDebugResponse> searchDebug(@PathVariable Long knowledgeBaseId,
                                                                 @RequestBody KnowledgeSearchDebugRequest request) {
        if (request == null || !StringUtils.hasText(request.getQuery())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }
        requirePlatformReadableKnowledgeBase(knowledgeBaseId);
        AiKnowledgeResolver.RetrievalResult retrieval = aiKnowledgeResolver.retrieve(
                null,
                request.getQuery(),
                List.of(knowledgeBaseId),
                request.getTopK(),
                request.getMode(),
                request.getStrictGrounding(),
                request.getEntryFile(),
                request.getSymbolHint(),
                request.getTraceDepth(),
                request.getActionCode()
        );
        List<KnowledgeSearchDebugResponse.HitItem> items = retrieval.getHits().stream()
                .map(this::toHitItem)
                .toList();
        return ApiResponse.ok("Search debug completed", KnowledgeSearchDebugResponse.builder()
                .knowledgeBaseId(knowledgeBaseId)
                .query(request.getQuery())
                .topK(retrieval.getTopK())
                .mode(retrieval.getMode() == null ? null : retrieval.getMode().name())
                .rerankProfile(retrieval.getRerankProfile())
                .strictGrounding(retrieval.isStrictGrounding())
                .groundingStatus(retrieval.getGroundingStatus() == null ? null : retrieval.getGroundingStatus().name())
                .refused(retrieval.isRefused())
                .refusalReason(retrieval.getRefusalReason())
                .hitCount(items.size())
                .vectorCandidateCount(retrieval.getVectorCandidateCount())
                .keywordCandidateCount(retrieval.getKeywordCandidateCount())
                .availableEmbeddingCount(retrieval.getAvailableEmbeddingCount())
                .providerFilteredEmbeddingCount(retrieval.getProviderFilteredEmbeddingCount())
                .modelFilteredEmbeddingCount(retrieval.getModelFilteredEmbeddingCount())
                .statusFilteredEmbeddingCount(retrieval.getStatusFilteredEmbeddingCount())
                .degradeReason(retrieval.getDegradeReason())
                .finalContextSource(retrieval.getFinalContextSource())
                .embeddingProfile(EmbeddingProfileView.from(retrieval.getEmbeddingProfile()))
                .hits(items)
                .build());
    }

    private KnowledgeBase requirePlatformReadableKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        return knowledgeBaseScopePolicy.requirePlatformScope(knowledgeBase);
    }

    private KnowledgeBase requirePlatformEditableKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        return knowledgeBaseScopePolicy.requirePlatformScope(knowledgeBase);
    }

    private KnowledgeDocument requirePlatformReadableDocument(Long documentId) {
        KnowledgeDocument document = knowledgeAccessGuard.requireDocumentRead(documentId);
        knowledgeBaseScopePolicy.requirePlatformScope(document.getKnowledgeBase());
        return document;
    }

    private KnowledgeDocument requirePlatformEditableDocument(Long documentId) {
        KnowledgeDocument document = knowledgeAccessGuard.requireDocumentEdit(documentId);
        knowledgeBaseScopePolicy.requirePlatformScope(document.getKnowledgeBase());
        return document;
    }

    private KnowledgeDocumentCreateRequest normalizeDocumentRequest(KnowledgeDocumentCreateRequest request) {
        KnowledgeDocumentCreateRequest normalized = request == null ? new KnowledgeDocumentCreateRequest() : request;
        normalized.setUploadedBy(currentUserProvider.resolveCurrentUserId());
        return normalized;
    }

    private void requireRequestBody(Object request, String message) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
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

    private KnowledgeSearchDebugResponse.HitItem toHitItem(KnowledgeRetrievalHit hit) {
        return KnowledgeSearchDebugResponse.HitItem.builder()
                .knowledgeBaseId(hit.getKnowledgeBaseId())
                .knowledgeBaseName(hit.getKnowledgeBaseName())
                .documentId(hit.getDocumentId())
                .documentTitle(hit.getDocumentTitle())
                .fileName(hit.getFileName())
                .archiveEntryPath(hit.getArchiveEntryPath())
                .path(hit.getPath())
                .chunkId(hit.getChunkId())
                .chunkIndex(hit.getChunkIndex())
                .snippet(hit.getSnippet())
                .score(hit.getScore())
                .keywordScore(hit.getKeywordScore())
                .vectorScore(hit.getVectorScore())
                .graphScore(hit.getGraphScore())
                .rerankScore(hit.getRerankScore())
                .retrievalMethod(hit.getRetrievalMethod() == null ? null : hit.getRetrievalMethod().name())
                .stageCode(hit.getStageCode() == null ? null : hit.getStageCode().name())
                .phase(hit.getPhase())
                .candidateSource(hit.getCandidateSource() == null ? null : hit.getCandidateSource().name())
                .hitReasonJson(hit.getHitReasonJson())
                .scoreDetailJson(hit.getScoreDetailJson())
                .rankNo(hit.getRankNo())
                .language(hit.getLanguage())
                .symbolName(hit.getSymbolName())
                .symbolType(hit.getSymbolType())
                .startLine(hit.getStartLine())
                .endLine(hit.getEndLine())
                .sectionName(hit.getSectionName())
                .build();
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

package com.alikeyou.itmoduleai.controller;

import com.alikeyou.itmoduleai.application.support.AiKnowledgeResolver;
import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.dto.request.KnowledgeChunkPreviewRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeEmbeddingBackfillRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeSearchDebugRequest;
import com.alikeyou.itmoduleai.dto.response.EmbeddingProfileView;
import com.alikeyou.itmoduleai.dto.response.KnowledgeChunkPreviewResponse;
import com.alikeyou.itmoduleai.dto.response.KnowledgeEmbeddingStatusResponse;
import com.alikeyou.itmoduleai.dto.response.KnowledgeSearchDebugResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/ai/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeDebugController {

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkingService knowledgeChunkingService;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;
    private final AiKnowledgeResolver aiKnowledgeResolver;
    private final KnowledgeAccessGuard knowledgeAccessGuard;

    @PostMapping("/documents/{documentId}/chunk-preview")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<List<KnowledgeChunkPreviewResponse>> previewDocumentChunks(
            @PathVariable Long documentId,
            @RequestBody(required = false) KnowledgeChunkPreviewRequest request
    ) {
        KnowledgeDocument document = knowledgeAccessGuard.requireDocumentRead(documentId);
        KnowledgeBase knowledgeBase = document.getKnowledgeBase();
        List<KnowledgeChunkPreviewResponse> preview = knowledgeChunkingService.previewChunks(
                knowledgeBase,
                document,
                StringUtils.hasText(request == null ? null : request.getText()) ? request.getText() : document.getContentText(),
                request == null ? new KnowledgeChunkPreviewRequest() : request
        );
        return ApiResponse.ok("切片预览完成", preview);
    }

    @PostMapping("/{knowledgeBaseId}/embedding-backfill")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeEmbeddingStatusResponse> backfillKnowledgeBaseEmbeddings(
            @PathVariable Long knowledgeBaseId,
            @RequestBody(required = false) KnowledgeEmbeddingBackfillRequest request
    ) {
        knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        KnowledgeEmbeddingStatusResponse result = knowledgeEmbeddingService.backfillKnowledgeBaseEmbeddings(
                knowledgeBaseId,
                request == null ? null : request.getProvider(),
                request == null ? null : request.getModelName(),
                request == null ? null : request.getDimension()
        );
        return ApiResponse.ok("知识库向量回填完成", result);
    }

    @PostMapping("/documents/{documentId}/embedding-backfill")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeEmbeddingStatusResponse> backfillDocumentEmbeddings(
            @PathVariable Long documentId,
            @RequestBody(required = false) KnowledgeEmbeddingBackfillRequest request
    ) {
        knowledgeAccessGuard.requireDocumentEdit(documentId);
        KnowledgeEmbeddingStatusResponse result = knowledgeEmbeddingService.backfillDocumentEmbeddings(
                documentId,
                request == null ? null : request.getProvider(),
                request == null ? null : request.getModelName(),
                request == null ? null : request.getDimension()
        );
        return ApiResponse.ok("文档向量回填完成", result);
    }

    @GetMapping("/{knowledgeBaseId}/embedding-status")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeEmbeddingStatusResponse> getKnowledgeBaseEmbeddingStatus(@PathVariable Long knowledgeBaseId) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        return ApiResponse.ok(knowledgeEmbeddingService.getKnowledgeBaseEmbeddingStatus(knowledgeBaseId));
    }

    @GetMapping("/documents/{documentId}/embedding-status")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeEmbeddingStatusResponse> getDocumentEmbeddingStatus(@PathVariable Long documentId) {
        knowledgeAccessGuard.requireDocumentRead(documentId);
        return ApiResponse.ok(knowledgeEmbeddingService.getDocumentEmbeddingStatus(documentId));
    }

    @PostMapping("/{knowledgeBaseId}/search-debug")
    @PreAuthorize("hasAuthority('view:knowledge-base')")
    public ApiResponse<KnowledgeSearchDebugResponse> searchDebug(
            @PathVariable Long knowledgeBaseId,
            @RequestBody KnowledgeSearchDebugRequest request
    ) {
        if (request == null || !StringUtils.hasText(request.getQuery())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "query 不能为空");
        }
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
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
        return ApiResponse.ok("检索调试完成", KnowledgeSearchDebugResponse.builder()
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
}

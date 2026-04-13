package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.dto.request.KnowledgeChunkPreviewRequest;
import com.alikeyou.itmoduleai.dto.response.KnowledgeChunkPreviewResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;

import java.util.List;

public interface KnowledgeChunkingService {
    List<KnowledgeChunkPreviewResponse> previewChunks(
            KnowledgeBase knowledgeBase,
            KnowledgeDocument document,
            String rawText,
            KnowledgeChunkPreviewRequest request
    );

    IndexBuildResult buildIndexDraft(
            KnowledgeBase knowledgeBase,
            KnowledgeDocument document,
            String rawText,
            KnowledgeChunkPreviewRequest request
    );

    record IndexBuildResult(
            List<ChunkDraft> chunks,
            List<SymbolDraft> symbols,
            List<ReferenceDraft> references
    ) {
        public static IndexBuildResult empty() {
            return new IndexBuildResult(List.of(), List.of(), List.of());
        }
    }

    record ChunkDraft(
            Integer chunkIndex,
            String title,
            String content,
            Integer tokenCount,
            Integer charCount,
            Integer startOffset,
            Integer endOffset,
            Integer startLine,
            Integer endLine,
            String metadataJson,
            String language,
            String path,
            String symbolName,
            String symbolType,
            String sectionName
    ) {
    }

    record SymbolDraft(
            String symbolKey,
            String symbolName,
            String qualifiedName,
            String symbolKind,
            String symbolScope,
            String signature,
            String returnType,
            String visibility,
            Boolean declaration,
            Boolean exported,
            Integer startLine,
            Integer endLine,
            Integer bodyStartLine,
            Integer bodyEndLine,
            Integer chunkIndex,
            String language,
            String filePath,
            String metadataJson
    ) {
    }

    record ReferenceDraft(
            String referenceKey,
            String fromSymbolKey,
            String refKind,
            String refName,
            String targetQualifiedName,
            Integer startLine,
            Integer endLine,
            Integer fromChunkIndex,
            String resolutionStatus,
            String fromFilePath,
            String language,
            String metadataJson
    ) {
    }
}

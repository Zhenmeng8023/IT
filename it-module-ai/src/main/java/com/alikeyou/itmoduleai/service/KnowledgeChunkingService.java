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
}

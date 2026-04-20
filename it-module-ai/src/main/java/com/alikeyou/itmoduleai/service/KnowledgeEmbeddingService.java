package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.dto.response.KnowledgeEmbeddingStatusResponse;

import java.util.List;

public interface KnowledgeEmbeddingService {
    KnowledgeEmbeddingStatusResponse getKnowledgeBaseEmbeddingStatus(Long knowledgeBaseId);

    KnowledgeEmbeddingStatusResponse getDocumentEmbeddingStatus(Long documentId);

    KnowledgeEmbeddingStatusResponse backfillDocumentEmbeddings(Long documentId, String provider, String modelName, Integer dimension);

    KnowledgeEmbeddingStatusResponse backfillKnowledgeBaseEmbeddings(Long knowledgeBaseId, String provider, String modelName, Integer dimension);

    List<Double> embedText(String text, String provider, String modelName, Integer dimension);

    List<Double> parseVectorPayload(String vectorPayload);
}

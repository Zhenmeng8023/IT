package com.alikeyou.itmoduleai.service;

public interface KnowledgeIndexTaskPatchService {

    boolean isKnowledgeBaseEmbeddingRunning(Long knowledgeBaseId);

    Long resolveKnowledgeBaseIdByDocumentId(Long documentId);

    Long startKnowledgeBaseEmbeddingTask(Long knowledgeBaseId);

    Long startDocumentEmbeddingTask(Long documentId);

    void markSuccess(Long taskId);

    void markFailed(Long taskId, String errorMessage);
}

package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.repository.KnowledgeIndexTaskPatchRepository;
import com.alikeyou.itmoduleai.service.KnowledgeIndexTaskPatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class KnowledgeIndexTaskPatchServiceImpl implements KnowledgeIndexTaskPatchService {

    private final KnowledgeIndexTaskPatchRepository knowledgeIndexTaskPatchRepository;

    @Override
    public boolean isKnowledgeBaseEmbeddingRunning(Long knowledgeBaseId) {
        return knowledgeIndexTaskPatchRepository.existsRunningEmbeddingTaskByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    public Long resolveKnowledgeBaseIdByDocumentId(Long documentId) {
        Long knowledgeBaseId = knowledgeIndexTaskPatchRepository.findKnowledgeBaseIdByDocumentId(documentId);
        if (knowledgeBaseId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "知识文档不存在或未绑定知识库");
        }
        return knowledgeBaseId;
    }

    @Override
    public Long startKnowledgeBaseEmbeddingTask(Long knowledgeBaseId) {
        return knowledgeIndexTaskPatchRepository.insertEmbeddingTask(knowledgeBaseId, null);
    }

    @Override
    public Long startDocumentEmbeddingTask(Long documentId) {
        Long knowledgeBaseId = resolveKnowledgeBaseIdByDocumentId(documentId);
        return knowledgeIndexTaskPatchRepository.insertEmbeddingTask(knowledgeBaseId, documentId);
    }

    @Override
    public void markSuccess(Long taskId) {
        knowledgeIndexTaskPatchRepository.markSuccess(taskId);
    }

    @Override
    public void markFailed(Long taskId, String errorMessage) {
        knowledgeIndexTaskPatchRepository.markFailed(taskId, errorMessage);
    }
}

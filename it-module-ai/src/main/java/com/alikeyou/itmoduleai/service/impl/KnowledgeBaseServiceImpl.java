package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@Transactional
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase) {
        knowledgeBase.setStatus(KnowledgeBase.Status.DRAFT);
        knowledgeBase.setDocCount(0);
        knowledgeBase.setChunkCount(0);
        knowledgeBase.setCreatedAt(Instant.now());
        knowledgeBase.setUpdatedAt(Instant.now());

        KnowledgeBase saved = knowledgeBaseRepository.save(knowledgeBase);
        log.info("创建知识库成功：{}", saved.getName());
        return saved;
    }

    @Override
    public KnowledgeBase updateKnowledgeBase(Long id, KnowledgeBase knowledgeBase) {
        KnowledgeBase existing = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识库不存在"));

        existing.setName(knowledgeBase.getName());
        existing.setDescription(knowledgeBase.getDescription());
        existing.setVisibility(knowledgeBase.getVisibility());
        existing.setDefaultTopK(knowledgeBase.getDefaultTopK());
        existing.setUpdatedAt(Instant.now());

        KnowledgeBase updated = knowledgeBaseRepository.save(existing);
        log.info("更新知识库成功：{}", updated.getName());
        return updated;
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        knowledgeBaseRepository.deleteById(id);
        log.info("删除知识库成功：{}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBase getKnowledgeBase(Long id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识库不存在"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeBase> getUserKnowledgeBases(Long userId, Pageable pageable) {
        return knowledgeBaseRepository.findByOwner_Id(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeBase> getProjectKnowledgeBases(Long projectId, Pageable pageable) {
        return knowledgeBaseRepository.findByProjectId(projectId, pageable);
    }

    @Override
    public KnowledgeDocument uploadDocument(Long knowledgeBaseId, KnowledgeDocument document) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new RuntimeException("知识库不存在"));

        document.setKnowledgeBase(knowledgeBase);
        document.setStatus(KnowledgeDocument.Status.UPLOADED);
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());

        KnowledgeDocument saved = knowledgeDocumentRepository.save(document);

        // 更新知识库文档数量
        Long docCount = knowledgeDocumentRepository.countByKnowledgeBaseId(knowledgeBaseId);
        knowledgeBase.setDocCount(docCount.intValue());
        knowledgeBaseRepository.save(knowledgeBase);

        log.info("上传文档到知识库成功：{}, 知识库：{}", document.getTitle(), knowledgeBase.getName());
        return saved;
    }

    @Override
    public void deleteDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));

        Long knowledgeBaseId = document.getKnowledgeBase().getId();
        knowledgeDocumentRepository.delete(document);

        // 更新知识库文档数量
        Long docCount = knowledgeDocumentRepository.countByKnowledgeBaseId(knowledgeBaseId);
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId).orElse(null);
        if (knowledgeBase != null) {
            knowledgeBase.setDocCount(docCount.intValue());
            knowledgeBaseRepository.save(knowledgeBase);
        }

        log.info("删除文档成功：{}", documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeDocument> getKnowledgeDocuments(Long knowledgeBaseId, Pageable pageable) {
        return knowledgeDocumentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(knowledgeBaseId, pageable);
    }

    @Override
    public void buildIndex(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new RuntimeException("知识库不存在"));

        knowledgeBase.setStatus(KnowledgeBase.Status.INDEXING);
        knowledgeBaseRepository.save(knowledgeBase);

        try {
            // TODO: 实现文档解析和向量化逻辑
            // 1. 获取所有待索引的文档
            // 2. 对每个文档进行分块处理
            // 3. 调用向量化 API 生成向量
            // 4. 保存向量到 knowledge_chunk 表

            knowledgeBase.setStatus(KnowledgeBase.Status.ACTIVE);
            knowledgeBase.setLastIndexedAt(Instant.now());
            log.info("知识库索引构建成功：{}", knowledgeBaseId);
        } catch (Exception e) {
            knowledgeBase.setStatus(KnowledgeBase.Status.FAILED);
            log.error("知识库索引构建失败：{}", knowledgeBaseId, e);
            throw e;
        } finally {
            knowledgeBase.setUpdatedAt(Instant.now());
            knowledgeBaseRepository.save(knowledgeBase);
        }
    }
}

package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     */
    KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * 更新知识库
     */
    KnowledgeBase updateKnowledgeBase(Long id, KnowledgeBase knowledgeBase);

    /**
     * 删除知识库
     */
    void deleteKnowledgeBase(Long id);

    /**
     * 获取知识库详情
     */
    KnowledgeBase getKnowledgeBase(Long id);

    /**
     * 获取用户的知识库列表
     */
    Page<KnowledgeBase> getUserKnowledgeBases(Long userId, Pageable pageable);

    /**
     * 获取项目的知识库列表
     */
    Page<KnowledgeBase> getProjectKnowledgeBases(Long projectId, Pageable pageable);

    /**
     * 上传文档到知识库
     */
    KnowledgeDocument uploadDocument(Long knowledgeBaseId, KnowledgeDocument document);

    /**
     * 从知识库删除文档
     */
    void deleteDocument(Long documentId);

    /**
     * 获取知识库文档列表
     */
    Page<KnowledgeDocument> getKnowledgeDocuments(Long knowledgeBaseId, Pageable pageable);

    /**
     * 构建知识库索引
     */
    void buildIndex(Long knowledgeBaseId);
}

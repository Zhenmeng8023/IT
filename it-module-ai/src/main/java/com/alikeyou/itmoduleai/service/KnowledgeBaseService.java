package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseMemberCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeIndexTaskCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface KnowledgeBaseService {

    KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request);

    KnowledgeBase updateKnowledgeBase(Long id, KnowledgeBaseCreateRequest request);

    KnowledgeBase getById(Long id);

    Page<KnowledgeBase> pageByOwner(Long ownerId, Pageable pageable);

    Page<KnowledgeBase> pageByProject(Long projectId, Pageable pageable);

    KnowledgeDocument addDocument(Long knowledgeBaseId, KnowledgeDocumentCreateRequest request);

    Page<KnowledgeDocument> pageDocuments(Long knowledgeBaseId, Pageable pageable);

    List<KnowledgeChunk> listChunks(Long documentId);

    KnowledgeBaseMember addMember(Long knowledgeBaseId, KnowledgeBaseMemberCreateRequest request);

    void removeMember(Long knowledgeBaseId, Long memberId);

    List<KnowledgeBaseMember> listMembers(Long knowledgeBaseId);

    KnowledgeIndexTask createIndexTask(Long knowledgeBaseId, KnowledgeIndexTaskCreateRequest request);

    List<KnowledgeIndexTask> listDocumentTasks(Long documentId);
}

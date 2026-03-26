package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseMemberCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeIndexTaskCreateRequest;
import com.alikeyou.itmoduleai.entity.*;
import com.alikeyou.itmoduleai.repository.*;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseMemberRepository knowledgeBaseMemberRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeIndexTaskRepository knowledgeIndexTaskRepository;

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request) {
        KnowledgeBase entity = new KnowledgeBase();
        entity.setScopeType(request.getScopeType());
        entity.setProjectId(request.getProjectId());
        entity.setOwnerId(request.getOwnerId());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSourceType(request.getSourceType());
        entity.setEmbeddingProvider(request.getEmbeddingProvider());
        entity.setEmbeddingModel(request.getEmbeddingModel());
        entity.setChunkStrategy(request.getChunkStrategy());
        entity.setDefaultTopK(request.getDefaultTopK());
        entity.setVisibility(request.getVisibility());
        entity.setStatus(KnowledgeBase.Status.DRAFT);
        entity.setDocCount(0);
        entity.setChunkCount(0);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return knowledgeBaseRepository.save(entity);
    }

    @Override
    public KnowledgeBase updateKnowledgeBase(Long id, KnowledgeBaseCreateRequest request) {
        KnowledgeBase entity = getById(id);
        entity.setScopeType(request.getScopeType());
        entity.setProjectId(request.getProjectId());
        entity.setOwnerId(request.getOwnerId());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSourceType(request.getSourceType());
        entity.setEmbeddingProvider(request.getEmbeddingProvider());
        entity.setEmbeddingModel(request.getEmbeddingModel());
        entity.setChunkStrategy(request.getChunkStrategy());
        entity.setDefaultTopK(request.getDefaultTopK());
        entity.setVisibility(request.getVisibility());
        entity.setUpdatedAt(Instant.now());
        return knowledgeBaseRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBase getById(Long id) {
        return knowledgeBaseRepository.findById(id).orElseThrow(() -> new RuntimeException("知识库不存在"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeBase> pageByOwner(Long ownerId, Pageable pageable) {
        return knowledgeBaseRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeBase> pageByProject(Long projectId, Pageable pageable) {
        return knowledgeBaseRepository.findByProjectIdOrderByUpdatedAtDesc(projectId, pageable);
    }

    @Override
    public KnowledgeDocument addDocument(Long knowledgeBaseId, KnowledgeDocumentCreateRequest request) {
        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
        KnowledgeDocument entity = new KnowledgeDocument();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setSourceType(request.getSourceType());
        entity.setSourceRefId(request.getSourceRefId());
        entity.setTitle(request.getTitle());
        entity.setContentText(request.getContentText());
        entity.setContentHash(request.getContentHash());
        entity.setStatus(KnowledgeDocument.Status.UPLOADED);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        KnowledgeDocument saved = knowledgeDocumentRepository.save(entity);
        knowledgeBase.setDocCount(Math.toIntExact(knowledgeDocumentRepository.countByKnowledgeBase_Id(knowledgeBaseId)));
        knowledgeBase.setUpdatedAt(Instant.now());
        knowledgeBaseRepository.save(knowledgeBase);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeDocument> pageDocuments(Long knowledgeBaseId, Pageable pageable) {
        return knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByUpdatedAtDesc(knowledgeBaseId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeChunk> listChunks(Long documentId) {
        return knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(documentId);
    }

    @Override
    public KnowledgeBaseMember addMember(Long knowledgeBaseId, KnowledgeBaseMemberCreateRequest request) {
        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
        if (knowledgeBaseMemberRepository.existsByKnowledgeBase_IdAndUserId(knowledgeBaseId, request.getUserId())) {
            throw new RuntimeException("成员已存在");
        }
        KnowledgeBaseMember entity = new KnowledgeBaseMember();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setUserId(request.getUserId());
        entity.setRoleCode(request.getRoleCode());
        entity.setCreatedAt(Instant.now());
        return knowledgeBaseMemberRepository.save(entity);
    }

    @Override
    public void removeMember(Long knowledgeBaseId, Long memberId) {
        KnowledgeBaseMember entity = knowledgeBaseMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("知识库成员不存在"));
        if (!entity.getKnowledgeBase().getId().equals(knowledgeBaseId)) {
            throw new RuntimeException("成员不属于当前知识库");
        }
        knowledgeBaseMemberRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseMember> listMembers(Long knowledgeBaseId) {
        return knowledgeBaseMemberRepository.findByKnowledgeBase_IdOrderByIdAsc(knowledgeBaseId);
    }

    @Override
    public KnowledgeIndexTask createIndexTask(Long knowledgeBaseId, KnowledgeIndexTaskCreateRequest request) {
        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
        KnowledgeIndexTask entity = new KnowledgeIndexTask();
        entity.setKnowledgeBase(knowledgeBase);
        if (request.getDocumentId() != null) {
            entity.setDocument(knowledgeDocumentRepository.findById(request.getDocumentId()).orElse(null));
        }
        entity.setTaskType(request.getTaskType());
        entity.setStatus(KnowledgeIndexTask.Status.PENDING);
        entity.setRetryCount(0);
        entity.setCreatedAt(Instant.now());
        return knowledgeIndexTaskRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeIndexTask> listDocumentTasks(Long documentId) {
        return knowledgeIndexTaskRepository.findByDocument_IdOrderByCreatedAtDesc(documentId);
    }
}

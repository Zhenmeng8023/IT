package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.request.AiMessageCreateRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.entity.*;
import com.alikeyou.itmoduleai.repository.*;
import com.alikeyou.itmoduleai.service.AiSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AiSessionServiceImpl implements AiSessionService {

    private final AiSessionRepository aiSessionRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiSessionKnowledgeBaseRepository aiSessionKnowledgeBaseRepository;
    private final AiModelRepository aiModelRepository;
    private final AiPromptTemplateRepository aiPromptTemplateRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    @Override
    public AiSession createSession(AiSessionCreateRequest request) {
        AiSession entity = new AiSession();
        entity.setUserId(request.getUserId());
        entity.setBizType(request.getBizType() == null ? AiSession.BizType.GENERAL : request.getBizType());
        entity.setBizId(request.getBizId());
        entity.setProjectId(request.getProjectId());
        entity.setSceneCode(request.getSceneCode());
        entity.setSessionTitle(request.getSessionTitle());
        entity.setMemoryMode(request.getMemoryMode() == null ? AiSession.MemoryMode.SHORT : request.getMemoryMode());
        entity.setExtConfig(request.getExtConfig());
        entity.setStatus(AiSession.Status.ACTIVE);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setLastMessageAt(now);
        if (request.getActiveModelId() != null) {
            entity.setActiveModel(aiModelRepository.findById(request.getActiveModelId()).orElse(null));
        }
        if (request.getPromptTemplateId() != null) {
            entity.setPromptTemplate(aiPromptTemplateRepository.findById(request.getPromptTemplateId()).orElse(null));
        }
        if (request.getDefaultKnowledgeBaseId() != null) {
            entity.setDefaultKnowledgeBase(knowledgeBaseRepository.findById(request.getDefaultKnowledgeBaseId()).orElse(null));
        }
        return aiSessionRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public AiSession getById(Long id) {
        return aiSessionRepository.findById(id).orElseThrow(() -> new RuntimeException("AI 会话不存在"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiSession> pageUserSessions(Long userId, AiSession.BizType bizType, Pageable pageable) {
        if (bizType == null) {
            return aiSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
        }
        return aiSessionRepository.findByUserIdAndBizTypeOrderByUpdatedAtDesc(userId, bizType, pageable);
    }

    @Override
    public AiMessage createMessage(Long sessionId, AiMessageCreateRequest request) {
        AiSession session = getById(sessionId);
        AiMessage entity = new AiMessage();
        entity.setSession(session);
        entity.setRole(request.getRole() == null ? AiMessage.Role.USER : request.getRole());
        entity.setSenderUserId(request.getSenderUserId());
        entity.setContent(request.getContent());
        entity.setContentTokens(request.getContentTokens());
        entity.setPromptTokens(request.getPromptTokens());
        entity.setCompletionTokens(request.getCompletionTokens());
        entity.setTotalTokens(request.getTotalTokens());
        entity.setQuotedChunkIds(request.getQuotedChunkIds());
        entity.setToolCallJson(request.getToolCallJson());
        entity.setLatencyMs(request.getLatencyMs());
        entity.setFinishReason(request.getFinishReason());
        entity.setStatus(request.getStatus() == null ? AiMessage.Status.SUCCESS : request.getStatus());
        entity.setCreatedAt(Instant.now());
        if (request.getPromptTemplateId() != null) {
            entity.setPromptTemplate(aiPromptTemplateRepository.findById(request.getPromptTemplateId()).orElse(null));
        }
        if (request.getModelId() != null) {
            entity.setModel(aiModelRepository.findById(request.getModelId()).orElse(null));
        }
        if (request.getKnowledgeBaseId() != null) {
            entity.setKnowledgeBase(knowledgeBaseRepository.findById(request.getKnowledgeBaseId()).orElse(null));
        }
        session.setLastMessageAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        aiSessionRepository.save(session);
        return aiMessageRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiMessage> pageMessages(Long sessionId, Pageable pageable) {
        return aiMessageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId, pageable);
    }

    @Override
    public List<AiSessionKnowledgeBase> bindKnowledgeBases(Long sessionId, List<Long> knowledgeBaseIds) {
        AiSession session = getById(sessionId);
        aiSessionKnowledgeBaseRepository.deleteBySession_Id(sessionId);
        List<AiSessionKnowledgeBase> list = new ArrayList<>();
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return list;
        }
        int i = 1;
        for (Long knowledgeBaseId : knowledgeBaseIds) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                    .orElseThrow(() -> new RuntimeException("知识库不存在"));
            AiSessionKnowledgeBase entity = new AiSessionKnowledgeBase();
            entity.setSession(session);
            entity.setKnowledgeBase(kb);
            entity.setPriority(i++);
            entity.setCreatedAt(Instant.now());
            list.add(entity);
        }
        return aiSessionKnowledgeBaseRepository.saveAll(list);
    }

    @Override
    public void archive(Long sessionId) {
        AiSession entity = getById(sessionId);
        entity.setStatus(AiSession.Status.ARCHIVED);
        entity.setUpdatedAt(Instant.now());
        aiSessionRepository.save(entity);
    }

    @Override
    public void delete(Long sessionId) {
        AiSession entity = getById(sessionId);
        entity.setStatus(AiSession.Status.DELETED);
        entity.setUpdatedAt(Instant.now());
        aiSessionRepository.save(entity);
    }
}

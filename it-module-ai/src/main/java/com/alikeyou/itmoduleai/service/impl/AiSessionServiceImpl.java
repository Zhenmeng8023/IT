package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.request.AiMessageCreateRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.entity.AiSessionKnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.repository.AiMessageRepository;
import com.alikeyou.itmoduleai.repository.AiModelRepository;
import com.alikeyou.itmoduleai.repository.AiPromptTemplateRepository;
import com.alikeyou.itmoduleai.repository.AiSessionKnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.AiSessionRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.service.AiSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        if (request == null) {
            throw new IllegalArgumentException("会话创建请求不能为空");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId不能为空");
        }

        AiSession.BizType bizType = request.getBizType() == null ? AiSession.BizType.GENERAL : request.getBizType();
        AiSession.MemoryMode memoryMode = request.getMemoryMode() == null ? AiSession.MemoryMode.SHORT : request.getMemoryMode();
        Instant now = Instant.now();

        AiSession entity = new AiSession();
        entity.setUserId(request.getUserId());
        entity.setBizType(bizType);
        entity.setBizId(request.getBizId());
        entity.setProjectId(request.getProjectId());
        entity.setSceneCode(resolveSceneCode(request, bizType));
        entity.setSessionTitle(resolveSessionTitle(request, bizType));
        entity.setMemoryMode(memoryMode);
        entity.setExtConfig(request.getExtConfig());
        entity.setStatus(AiSession.Status.ACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setLastMessageAt(now);

        if (request.getActiveModelId() != null) {
            entity.setActiveModel(loadModel(request.getActiveModelId()));
        }
        if (request.getPromptTemplateId() != null) {
            entity.setPromptTemplate(loadPromptTemplate(request.getPromptTemplateId()));
        }
        if (request.getDefaultKnowledgeBaseId() != null) {
            entity.setDefaultKnowledgeBase(loadKnowledgeBase(request.getDefaultKnowledgeBaseId()));
        }

        return aiSessionRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public AiSession getById(Long id) {
        return aiSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AI 会话不存在"));
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
        if (request == null) {
            throw new IllegalArgumentException("消息创建请求不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("content不能为空");
        }

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
            entity.setPromptTemplate(loadPromptTemplate(request.getPromptTemplateId()));
        }
        if (request.getModelId() != null) {
            entity.setModel(loadModel(request.getModelId()));
        }
        if (request.getKnowledgeBaseId() != null) {
            entity.setKnowledgeBase(loadKnowledgeBase(request.getKnowledgeBaseId()));
        }

        Instant now = Instant.now();
        session.setLastMessageAt(now);
        session.setUpdatedAt(now);
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

        List<Long> normalizedIds = knowledgeBaseIds == null
                ? List.of()
                : knowledgeBaseIds.stream().filter(Objects::nonNull).distinct().toList();

        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        List<AiSessionKnowledgeBase> list = new ArrayList<>();
        int priority = 1;
        for (Long knowledgeBaseId : normalizedIds) {
            KnowledgeBase kb = loadKnowledgeBase(knowledgeBaseId);
            AiSessionKnowledgeBase entity = new AiSessionKnowledgeBase();
            entity.setSession(session);
            entity.setKnowledgeBase(kb);
            entity.setPriority(priority++);
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

    private String resolveSceneCode(AiSessionCreateRequest request, AiSession.BizType bizType) {
        if (StringUtils.hasText(request.getSceneCode())) {
            return request.getSceneCode().trim();
        }
        return switch (bizType) {
            case GENERAL -> "general.chat";
            case PROJECT -> "project.assistant";
            case BLOG -> "blog.assistant";
            case CIRCLE -> "circle.assistant";
            case PAID_CONTENT -> "paid-content.assistant";
        };
    }

    private String resolveSessionTitle(AiSessionCreateRequest request, AiSession.BizType bizType) {
        if (StringUtils.hasText(request.getSessionTitle())) {
            return request.getSessionTitle().trim();
        }
        return switch (bizType) {
            case GENERAL -> "新建对话";
            case PROJECT -> "项目助手";
            case BLOG -> "博客助手";
            case CIRCLE -> "圈子助手";
            case PAID_CONTENT -> "内容助手";
        };
    }

    private AiModel loadModel(Long id) {
        return aiModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI 模型不存在: " + id));
    }

    private AiPromptTemplate loadPromptTemplate(Long id) {
        return aiPromptTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("提示词模板不存在: " + id));
    }

    private KnowledgeBase loadKnowledgeBase(Long id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + id));
    }
}
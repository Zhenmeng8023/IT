package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.request.AiMessageCreateRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionBindKnowledgeBaseRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.dto.response.AiCitationResponse;
import com.alikeyou.itmoduleai.dto.response.AiKnowledgeBaseBindingVO;
import com.alikeyou.itmoduleai.dto.response.AiMessageVO;
import com.alikeyou.itmoduleai.dto.response.AiSessionVO;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.entity.AiSessionKnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.repository.AiCallLogRepository;
import com.alikeyou.itmoduleai.repository.AiMessageRepository;
import com.alikeyou.itmoduleai.repository.AiModelRepository;
import com.alikeyou.itmoduleai.repository.AiPromptTemplateRepository;
import com.alikeyou.itmoduleai.repository.AiSessionKnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.AiSessionRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseMemberRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.service.AiSessionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AiSessionServiceImpl implements AiSessionService {

    private final AiSessionRepository aiSessionRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final AiSessionKnowledgeBaseRepository aiSessionKnowledgeBaseRepository;
    private final AiModelRepository aiModelRepository;
    private final AiPromptTemplateRepository aiPromptTemplateRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseMemberRepository knowledgeBaseMemberRepository;
    private final AiCurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    @Override
    public AiSession createSession(AiSessionCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("session create request must not be null");
        }
        Long currentUserId = currentUserProvider.requireCurrentUserId();

        AiSession.BizType bizType = request.getBizType() == null ? AiSession.BizType.GENERAL : request.getBizType();
        AiSession.MemoryMode memoryMode = request.getMemoryMode() == null ? AiSession.MemoryMode.SHORT : request.getMemoryMode();
        Instant now = Instant.now();

        AiSession entity = new AiSession();
        entity.setUserId(currentUserId);
        entity.setBizType(bizType);
        entity.setBizId(request.getBizId());
        entity.setProjectId(request.getProjectId());
        entity.setAnalysisProfile(request.getAnalysisProfile());
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

        AiSession saved = aiSessionRepository.save(entity);
        AiSessionBindKnowledgeBaseRequest bindingRequest = new AiSessionBindKnowledgeBaseRequest();
        bindingRequest.setKnowledgeBaseIds(request.getKnowledgeBaseIds());
        bindingRequest.setDefaultKnowledgeBaseId(request.getDefaultKnowledgeBaseId());
        return applyKnowledgeBaseBindings(saved, bindingRequest, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public AiSession getById(Long id) {
        return loadOwnedSession(id, currentUserProvider.requireCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public AiSessionVO getSession(Long id) {
        return toSessionVO(loadOwnedSession(id, currentUserProvider.requireCurrentUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiSessionVO> pageCurrentUserSessions(AiSession.BizType bizType,
                                                     Long knowledgeBaseId,
                                                     AiSession.Status status,
                                                     Pageable pageable) {
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        AiSession.Status effectiveStatus = status == null ? AiSession.Status.ACTIVE : status;
        return aiSessionRepository.searchUserSessions(currentUserId, bizType, knowledgeBaseId, effectiveStatus, pageable)
                .map(this::toSessionVO);
    }

    @Override
    public AiMessage createMessage(Long sessionId, AiMessageCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("message create request must not be null");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("content must not be blank");
        }

        Long currentUserId = currentUserProvider.requireCurrentUserId();
        AiSession session = loadOwnedSession(sessionId, currentUserId);

        AiMessage.Role role = request.getRole() == null ? AiMessage.Role.USER : request.getRole();
        AiMessage entity = new AiMessage();
        entity.setSession(session);
        entity.setRole(role);
        entity.setSenderUserId(role == AiMessage.Role.USER ? currentUserId : null);
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
            entity.setKnowledgeBase(loadAccessibleKnowledgeBase(request.getKnowledgeBaseId(), currentUserId));
        }

        Instant now = Instant.now();
        session.setLastMessageAt(now);
        session.setUpdatedAt(now);
        aiSessionRepository.save(session);

        return aiMessageRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiMessageVO> pageMessages(Long sessionId, Pageable pageable) {
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        loadOwnedSession(sessionId, currentUserId);
        return aiMessageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId, pageable)
                .map(this::toMessageVO);
    }

    @Override
    public AiSessionVO bindKnowledgeBases(Long sessionId, AiSessionBindKnowledgeBaseRequest request) {
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        AiSession session = loadOwnedSession(sessionId, currentUserId);
        AiSession saved = applyKnowledgeBaseBindings(session, request, currentUserId);
        return toSessionVO(saved);
    }

    @Override
    public void archive(Long sessionId) {
        AiSession entity = loadOwnedSession(sessionId, currentUserProvider.requireCurrentUserId());
        entity.setStatus(AiSession.Status.ARCHIVED);
        entity.setUpdatedAt(Instant.now());
        aiSessionRepository.save(entity);
    }

    @Override
    public void delete(Long sessionId) {
        AiSession entity = loadOwnedSession(sessionId, currentUserProvider.requireCurrentUserId());
        entity.setStatus(AiSession.Status.DELETED);
        entity.setUpdatedAt(Instant.now());
        aiSessionRepository.save(entity);
    }

    private AiSession applyKnowledgeBaseBindings(AiSession session,
                                                 AiSessionBindKnowledgeBaseRequest request,
                                                 Long currentUserId) {
        AiSessionBindKnowledgeBaseRequest effectiveRequest = request == null ? new AiSessionBindKnowledgeBaseRequest() : request;
        List<Long> normalizedIds = normalizeKnowledgeBaseIds(
                effectiveRequest.getKnowledgeBaseIds(),
                effectiveRequest.getDefaultKnowledgeBaseId()
        );
        Map<Long, KnowledgeBase> knowledgeBaseMap = loadAccessibleKnowledgeBases(normalizedIds, currentUserId);

        aiSessionKnowledgeBaseRepository.deleteBySession_Id(session.getId());
        aiSessionKnowledgeBaseRepository.flush();
        Instant now = Instant.now();

        if (normalizedIds.isEmpty()) {
            session.setDefaultKnowledgeBase(null);
            session.setUpdatedAt(now);
            return aiSessionRepository.save(session);
        }

        Long defaultId = effectiveRequest.getDefaultKnowledgeBaseId();
        if (defaultId == null || !knowledgeBaseMap.containsKey(defaultId)) {
            defaultId = normalizedIds.get(0);
        }

        session.setDefaultKnowledgeBase(knowledgeBaseMap.get(defaultId));
        session.setUpdatedAt(now);
        AiSession saved = aiSessionRepository.save(session);

        List<AiSessionKnowledgeBase> bindings = new ArrayList<>();
        int priority = 1;
        for (Long knowledgeBaseId : normalizedIds) {
            KnowledgeBase kb = knowledgeBaseMap.get(knowledgeBaseId);
            AiSessionKnowledgeBase entity = new AiSessionKnowledgeBase();
            entity.setSession(saved);
            entity.setKnowledgeBase(kb);
            entity.setPriority(priority++);
            entity.setCreatedAt(now);
            bindings.add(entity);
        }
        aiSessionKnowledgeBaseRepository.saveAll(bindings);
        return saved;
    }

    private List<Long> normalizeKnowledgeBaseIds(List<Long> knowledgeBaseIds, Long defaultKnowledgeBaseId) {
        Set<Long> ids = new LinkedHashSet<>();
        if (knowledgeBaseIds != null) {
            knowledgeBaseIds.stream().filter(Objects::nonNull).forEach(ids::add);
        }
        if (defaultKnowledgeBaseId != null) {
            ids.add(defaultKnowledgeBaseId);
        }
        return new ArrayList<>(ids);
    }

    private Map<Long, KnowledgeBase> loadAccessibleKnowledgeBases(List<Long> knowledgeBaseIds, Long currentUserId) {
        Map<Long, KnowledgeBase> result = new LinkedHashMap<>();
        for (Long id : knowledgeBaseIds) {
            result.put(id, loadAccessibleKnowledgeBase(id, currentUserId));
        }
        return result;
    }

    private AiSession loadOwnedSession(Long sessionId, Long currentUserId) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId must not be null");
        }
        AiSession session = aiSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI session does not exist"));
        if (!Objects.equals(session.getUserId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AI session access denied");
        }
        return session;
    }

    private AiSessionVO toSessionVO(AiSession entity) {
        List<AiSessionKnowledgeBase> bindings = aiSessionKnowledgeBaseRepository.findBySession_IdOrderByPriorityAscIdAsc(entity.getId());
        Long defaultKnowledgeBaseId = entity.getDefaultKnowledgeBaseId();
        Long recentKnowledgeBaseId = resolveRecentKnowledgeBaseId(entity);
        List<AiKnowledgeBaseBindingVO> bindingVOs = new ArrayList<>();
        Set<Long> boundIds = new LinkedHashSet<>();

        for (AiSessionKnowledgeBase binding : bindings) {
            AiKnowledgeBaseBindingVO vo = toBindingVO(binding, defaultKnowledgeBaseId, recentKnowledgeBaseId);
            if (vo.getKnowledgeBaseId() != null) {
                boundIds.add(vo.getKnowledgeBaseId());
            }
            bindingVOs.add(vo);
        }

        if (defaultKnowledgeBaseId != null && !boundIds.contains(defaultKnowledgeBaseId)) {
            KnowledgeBase defaultKnowledgeBase = entity.getDefaultKnowledgeBase();
            bindingVOs.add(AiKnowledgeBaseBindingVO.builder()
                    .sessionId(entity.getId())
                    .knowledgeBaseId(defaultKnowledgeBaseId)
                    .knowledgeBaseName(defaultKnowledgeBase == null ? null : defaultKnowledgeBase.getName())
                    .scopeType(defaultKnowledgeBase == null ? null : defaultKnowledgeBase.getScopeType())
                    .visibility(defaultKnowledgeBase == null ? null : defaultKnowledgeBase.getVisibility())
                    .status(defaultKnowledgeBase == null ? null : defaultKnowledgeBase.getStatus())
                    .priority(0)
                    .defaultKnowledgeBase(true)
                    .recentKnowledgeBase(Objects.equals(defaultKnowledgeBaseId, recentKnowledgeBaseId))
                    .createdAt(entity.getCreatedAt())
                    .build());
            boundIds.add(defaultKnowledgeBaseId);
        }

        return AiSessionVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .bizType(entity.getBizType())
                .bizId(entity.getBizId())
                .projectId(entity.getProjectId())
                .sceneCode(entity.getSceneCode())
                .sessionTitle(entity.getSessionTitle())
                .memoryMode(entity.getMemoryMode())
                .activeModelId(entity.getActiveModelId())
                .promptTemplateId(entity.getPromptTemplateId())
                .defaultKnowledgeBaseId(defaultKnowledgeBaseId)
                .recentKnowledgeBaseId(recentKnowledgeBaseId)
                .boundKnowledgeBaseIds(new ArrayList<>(boundIds))
                .boundKnowledgeBases(bindingVOs)
                .sessionSummary(entity.getSessionSummary())
                .extConfig(entity.getExtConfig())
                .status(entity.getStatus())
                .lastMessageAt(entity.getLastMessageAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AiKnowledgeBaseBindingVO toBindingVO(AiSessionKnowledgeBase binding,
                                                Long defaultKnowledgeBaseId,
                                                Long recentKnowledgeBaseId) {
        KnowledgeBase kb = binding.getKnowledgeBase();
        Long knowledgeBaseId = kb == null ? null : kb.getId();
        return AiKnowledgeBaseBindingVO.builder()
                .id(binding.getId())
                .sessionId(binding.getSession() == null ? null : binding.getSession().getId())
                .knowledgeBaseId(knowledgeBaseId)
                .knowledgeBaseName(kb == null ? null : kb.getName())
                .scopeType(kb == null ? null : kb.getScopeType())
                .visibility(kb == null ? null : kb.getVisibility())
                .status(kb == null ? null : kb.getStatus())
                .priority(binding.getPriority())
                .defaultKnowledgeBase(Objects.equals(knowledgeBaseId, defaultKnowledgeBaseId))
                .recentKnowledgeBase(Objects.equals(knowledgeBaseId, recentKnowledgeBaseId))
                .createdAt(binding.getCreatedAt())
                .build();
    }

    private AiMessageVO toMessageVO(AiMessage entity) {
        Map<String, Object> toolContext = readJsonMap(entity.getToolCallJson());
        Map<String, Object> retrievalSummary = readJsonMap(entity.getRetrievalSummaryJson());
        Map<String, Object> groundingReport = readJsonMap(entity.getGroundingJson());
        List<Long> knowledgeBaseIds = readLongList(toolContext.get("knowledgeBaseIds"));
        if (knowledgeBaseIds.isEmpty() && entity.getKnowledgeBase() != null) {
            knowledgeBaseIds = List.of(entity.getKnowledgeBase().getId());
        }
        Long recentKnowledgeBaseId = readLong(toolContext.get("recentKnowledgeBaseId"));
        if (recentKnowledgeBaseId == null && entity.getKnowledgeBase() != null) {
            recentKnowledgeBaseId = entity.getKnowledgeBase().getId();
        }
        Long defaultKnowledgeBaseId = readLong(toolContext.get("defaultKnowledgeBaseId"));
        if (defaultKnowledgeBaseId == null && entity.getSession() != null) {
            defaultKnowledgeBaseId = entity.getSession().getDefaultKnowledgeBaseId();
        }
        List<AiCitationResponse> citations = readCitations(entity.getCitationJson());
        if (citations.isEmpty()) {
            citations = readCitations(toolContext.get("citations"));
        }
        Long callLogId = entity.getRole() == AiMessage.Role.ASSISTANT && entity.getId() != null
                ? aiCallLogRepository.findTopByMessage_IdOrderByCreatedAtDesc(entity.getId())
                .map(AiCallLog::getId)
                .orElse(null)
                : null;

        return AiMessageVO.builder()
                .id(entity.getId())
                .sessionId(entity.getSession() == null ? null : entity.getSession().getId())
                .callLogId(callLogId)
                .role(entity.getRole())
                .senderUserId(entity.getSenderUserId())
                .content(entity.getContent())
                .contentTokens(entity.getContentTokens())
                .promptTokens(entity.getPromptTokens())
                .completionTokens(entity.getCompletionTokens())
                .totalTokens(entity.getTotalTokens())
                .promptTemplateId(entity.getPromptTemplate() == null ? null : entity.getPromptTemplate().getId())
                .modelId(entity.getModel() == null ? null : entity.getModel().getId())
                .knowledgeBaseId(entity.getKnowledgeBase() == null ? null : entity.getKnowledgeBase().getId())
                .knowledgeBaseIds(knowledgeBaseIds)
                .defaultKnowledgeBaseId(defaultKnowledgeBaseId)
                .recentKnowledgeBaseId(recentKnowledgeBaseId)
                .citations(citations)
                .quotedChunkIds(entity.getQuotedChunkIds())
                .toolCallJson(entity.getToolCallJson())
                .retrievalSummary(retrievalSummary.isEmpty() ? null : retrievalSummary)
                .groundingStatus(entity.getGroundingStatus())
                .grounding(groundingReport.isEmpty() ? null : groundingReport)
                .streamState(entity.getStreamState())
                .latencyMs(entity.getLatencyMs())
                .finishReason(entity.getFinishReason())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Long resolveRecentKnowledgeBaseId(AiSession session) {
        Long fromExtConfig = readLong(readJsonMap(session.getExtConfig()).get("recentKnowledgeBaseId"));
        if (fromExtConfig != null) {
            return fromExtConfig;
        }
        return aiMessageRepository.findTopBySession_IdAndKnowledgeBaseIsNotNullOrderByCreatedAtDesc(session.getId())
                .map(AiMessage::getKnowledgeBase)
                .map(KnowledgeBase::getId)
                .orElse(null);
    }

    private Map<String, Object> readJsonMap(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private List<Long> readLongList(Object value) {
        if (value == null) {
            return List.of();
        }
        List<?> values;
        if (value instanceof List<?> list) {
            values = list;
        } else {
            values = List.of(value);
        }
        List<Long> result = new ArrayList<>();
        for (Object item : values) {
            Long parsed = readLong(item);
            if (parsed != null && !result.contains(parsed)) {
                result.add(parsed);
            }
        }
        return result;
    }

    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String text = String.valueOf(value).trim();
            return StringUtils.hasText(text) ? Long.parseLong(text) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<AiCitationResponse> readCitations(Object value) {
        if (value == null) {
            return List.of();
        }
        try {
            if (value instanceof String raw && StringUtils.hasText(raw)) {
                return objectMapper.readValue(raw, new TypeReference<List<AiCitationResponse>>() {});
            }
            return objectMapper.convertValue(value, new TypeReference<List<AiCitationResponse>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
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
            case GENERAL -> "New chat";
            case PROJECT -> "Project assistant";
            case BLOG -> "Blog assistant";
            case CIRCLE -> "Circle assistant";
            case PAID_CONTENT -> "Content assistant";
        };
    }

    private AiModel loadModel(Long id) {
        return aiModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI model does not exist: " + id));
    }

    private AiPromptTemplate loadPromptTemplate(Long id) {
        return aiPromptTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("prompt template does not exist: " + id));
    }

    private KnowledgeBase loadAccessibleKnowledgeBase(Long id, Long currentUserId) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("knowledge base does not exist: " + id));
        if (!canUseKnowledgeBase(knowledgeBase, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "knowledge base access denied");
        }
        return knowledgeBase;
    }

    private boolean canUseKnowledgeBase(KnowledgeBase knowledgeBase, Long currentUserId) {
        if (knowledgeBase == null || currentUserId == null) {
            return false;
        }
        if (Objects.equals(knowledgeBase.getOwnerId(), currentUserId)) {
            return true;
        }
        if (knowledgeBase.getVisibility() == KnowledgeBase.Visibility.PUBLIC) {
            return true;
        }
        return knowledgeBaseMemberRepository.existsByKnowledgeBase_IdAndUserId(knowledgeBase.getId(), currentUserId);
    }
}

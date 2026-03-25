package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.AiCallLogDTO;
import com.alikeyou.itmoduleai.dto.AiRetrievalLogDTO;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.repository.AiCallLogRepository;
import com.alikeyou.itmoduleai.repository.AiRetrievalLogRepository;
import com.alikeyou.itmoduleai.service.AiLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AiLogServiceImpl implements AiLogService {

    @Autowired
    private AiCallLogRepository aiCallLogRepository;

    @Autowired
    private AiRetrievalLogRepository aiRetrievalLogRepository;

    @Override
    public Long logAiCall(AiCallLogDTO logDTO) {
        AiCallLog aiCallLog = new AiCallLog();

        if (logDTO.getUserId() != null) {
            var user = new com.alikeyou.itmodulecommon.entity.UserInfo();
            user.setId(logDTO.getUserId());
            aiCallLog.setUser(user);
        }

        if (logDTO.getConversationId() != null) {
            var conversation = new com.alikeyou.itmoduleinteractive.entity.Conversation();
            conversation.setId(logDTO.getConversationId());
            aiCallLog.setConversation(conversation);
        }

        if (logDTO.getMessageId() != null) {
            var message = new com.alikeyou.itmoduleinteractive.entity.Message();
            message.setId(logDTO.getMessageId());
            aiCallLog.setMessage(message);
        }

        if (logDTO.getPromptTemplateId() != null) {
            var promptTemplate = new com.alikeyou.itmoduleai.entity.AiPromptTemplate();
            promptTemplate.setId(logDTO.getPromptTemplateId());
            aiCallLog.setPromptTemplate(promptTemplate);
        }

        if (logDTO.getAiModelId() != null) {
            var aiModel = new com.alikeyou.itmoduleai.entity.AiModel();
            aiModel.setId(logDTO.getAiModelId());
            aiCallLog.setAiModel(aiModel);
        }

        aiCallLog.setRequestType(AiCallLog.RequestType.valueOf(logDTO.getRequestType()));
        aiCallLog.setRequestText(logDTO.getRequestText());
        aiCallLog.setResponseText(logDTO.getResponseText());
        aiCallLog.setRequestParams(logDTO.getRequestParams());
        aiCallLog.setPromptTokens(logDTO.getPromptTokens());
        aiCallLog.setCompletionTokens(logDTO.getCompletionTokens());
        aiCallLog.setTotalTokens(logDTO.getTotalTokens());
        aiCallLog.setCostAmount(logDTO.getCostAmount());
        aiCallLog.setLatencyMs(logDTO.getLatencyMs());
        aiCallLog.setStatus(AiCallLog.Status.valueOf(logDTO.getStatus()));
        aiCallLog.setErrorCode(logDTO.getErrorCode());
        aiCallLog.setErrorMessage(logDTO.getErrorMessage());
        aiCallLog.setCreatedAt(Instant.now());

        AiCallLog saved = aiCallLogRepository.save(aiCallLog);
        log.info("记录 AI 调用日志：userId={}, tokens={}, cost={}", logDTO.getUserId(), logDTO.getTotalTokens(), logDTO.getCostAmount());
        return saved.getId();
    }

    @Override
    public Long logAiRetrieval(AiRetrievalLogDTO logDTO) {
        AiRetrievalLog aiRetrievalLog = new AiRetrievalLog();

        // 设置 call_log 关联
        var callLog = new AiCallLog();
        callLog.setId(logDTO.getCallLogId());
        aiRetrievalLog.setCallLog(callLog);

        if (logDTO.getKnowledgeBaseId() != null) {
            var knowledgeBase = new com.alikeyou.itmoduleai.entity.KnowledgeBase();
            knowledgeBase.setId(logDTO.getKnowledgeBaseId());
            aiRetrievalLog.setKnowledgeBase(knowledgeBase);
        }
        if (logDTO.getDocumentId() != null) {
            var document = new com.alikeyou.itmoduleai.entity.KnowledgeDocument();
            document.setId(logDTO.getDocumentId());
            aiRetrievalLog.setDocument(document);
        }
        if (logDTO.getChunkId() != null) {
            var chunk = new com.alikeyou.itmoduleai.entity.KnowledgeChunk();
            chunk.setId(logDTO.getChunkId());
            aiRetrievalLog.setChunk(chunk);
        }

        aiRetrievalLog.setQueryText(logDTO.getQueryText());
        aiRetrievalLog.setScore(logDTO.getScore());
        aiRetrievalLog.setRankNo(logDTO.getRankNo());
        aiRetrievalLog.setRetrievalMethod(AiRetrievalLog.RetrievalMethod.valueOf(logDTO.getRetrievalMethod()));
        aiRetrievalLog.setCreatedAt(Instant.now());

        AiRetrievalLog saved = aiRetrievalLogRepository.save(aiRetrievalLog);
        log.debug("记录 AI 检索日志：callLogId={}, score={}", logDTO.getCallLogId(), logDTO.getScore());
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiCallLogDTO> getCallLogs(Long userId, Pageable pageable) {
        Page<AiCallLog> logs = aiCallLogRepository.findByUserId(userId, pageable);
        List<AiCallLogDTO> dtoList = logs.stream().map(this::convertToDTO).collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, logs.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiRetrievalLogDTO> getRetrievalLogs(Long callLogId) {
        List<AiRetrievalLog> logs = aiRetrievalLogRepository.findByCallLogId(callLogId);
        return logs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AiUsageStats getUserStats(Long userId, Instant startDate, Instant endDate) {
        List<AiCallLog> logs = aiCallLogRepository.findUserLogsByDateRange(userId, startDate, endDate);

        long totalCalls = logs.size();
        long successfulCalls = logs.stream()
                .filter(item -> item.getStatus() == AiCallLog.Status.SUCCESS)
                .count();

        long totalTokens = logs.stream()
                .mapToLong(item -> item.getTotalTokens() != null ? item.getTotalTokens() : 0)
                .sum();

        double totalCost = logs.stream()
                .mapToDouble(item -> item.getCostAmount() != null ? item.getCostAmount().doubleValue() : 0.0)
                .sum();

        return new AiUsageStats(totalCalls, successfulCalls, totalTokens, totalCost);
    }

    private AiCallLogDTO convertToDTO(AiCallLog aiCallLog) {
        return AiCallLogDTO.builder()
                .userId(aiCallLog.getUser() != null ? aiCallLog.getUser().getId() : null)
                .conversationId(aiCallLog.getConversation() != null ? aiCallLog.getConversation().getId() : null)
                .messageId(aiCallLog.getMessage() != null ? aiCallLog.getMessage().getId() : null)
                .promptTemplateId(aiCallLog.getPromptTemplate() != null ? aiCallLog.getPromptTemplate().getId() : null)
                .aiModelId(aiCallLog.getAiModel() != null ? aiCallLog.getAiModel().getId() : null)
                .requestType(aiCallLog.getRequestType().name())
                .requestText(aiCallLog.getRequestText())
                .responseText(aiCallLog.getResponseText())
                .requestParams(aiCallLog.getRequestParams())
                .promptTokens(aiCallLog.getPromptTokens())
                .completionTokens(aiCallLog.getCompletionTokens())
                .totalTokens(aiCallLog.getTotalTokens())
                .costAmount(aiCallLog.getCostAmount())
                .latencyMs(aiCallLog.getLatencyMs())
                .status(aiCallLog.getStatus().name())
                .errorCode(aiCallLog.getErrorCode())
                .errorMessage(aiCallLog.getErrorMessage())
                .build();
    }

    private AiRetrievalLogDTO convertToDTO(AiRetrievalLog aiRetrievalLog) {
        return AiRetrievalLogDTO.builder()
                .callLogId(aiRetrievalLog.getCallLog().getId())
                .knowledgeBaseId(aiRetrievalLog.getKnowledgeBase() != null ? aiRetrievalLog.getKnowledgeBase().getId() : null)
                .documentId(aiRetrievalLog.getDocument() != null ? aiRetrievalLog.getDocument().getId() : null)
                .chunkId(aiRetrievalLog.getChunk() != null ? aiRetrievalLog.getChunk().getId() : null)
                .queryText(aiRetrievalLog.getQueryText())
                .score(aiRetrievalLog.getScore())
                .rankNo(aiRetrievalLog.getRankNo())
                .retrievalMethod(aiRetrievalLog.getRetrievalMethod().name())
                .build();
    }
}

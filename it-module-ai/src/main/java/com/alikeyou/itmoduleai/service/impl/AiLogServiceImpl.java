package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.request.AiFeedbackCreateRequest;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiFeedbackLog;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.repository.AiCallLogRepository;
import com.alikeyou.itmoduleai.repository.AiFeedbackLogRepository;
import com.alikeyou.itmoduleai.repository.AiMessageRepository;
import com.alikeyou.itmoduleai.repository.AiRetrievalLogRepository;
import com.alikeyou.itmoduleai.service.AiLogService;
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
public class AiLogServiceImpl implements AiLogService {

    private final AiCallLogRepository aiCallLogRepository;
    private final AiRetrievalLogRepository aiRetrievalLogRepository;
    private final AiFeedbackLogRepository aiFeedbackLogRepository;
    private final AiMessageRepository aiMessageRepository;

    @Override
    public AiCallLog saveCallLog(AiCallLog entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return aiCallLogRepository.save(entity);
    }

    @Override
    public List<AiRetrievalLog> saveRetrievalLogs(List<AiRetrievalLog> entities) {
        Instant now = Instant.now();
        entities.forEach(item -> {
            if (item.getCreatedAt() == null) {
                item.setCreatedAt(now);
            }
        });
        return aiRetrievalLogRepository.saveAll(entities);
    }

    @Override
    public AiFeedbackLog saveFeedback(AiFeedbackCreateRequest request) {
        AiFeedbackLog entity = new AiFeedbackLog();
        entity.setUserId(request.getUserId());
        entity.setFeedbackType(request.getFeedbackType());
        entity.setCommentText(request.getCommentText());
        entity.setCreatedAt(Instant.now());
        if (request.getCallLogId() != null) {
            entity.setCallLog(aiCallLogRepository.findById(request.getCallLogId()).orElse(null));
        }
        if (request.getMessageId() != null) {
            entity.setMessage(aiMessageRepository.findById(request.getMessageId()).orElse(null));
        }
        return aiFeedbackLogRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiCallLog> pageUserCallLogs(Long userId, Pageable pageable) {
        return aiCallLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiCallLog> pageSessionCallLogs(Long sessionId, Pageable pageable) {
        return aiCallLogRepository.findBySession_IdOrderByCreatedAtDesc(sessionId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiRetrievalLog> listRetrievalLogs(Long callLogId) {
        return aiRetrievalLogRepository.findByCallLog_IdOrderByRankNoAsc(callLogId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiFeedbackLog> listMessageFeedbacks(Long messageId) {
        return aiFeedbackLogRepository.findByMessage_IdOrderByCreatedAtDesc(messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiFeedbackLog> listUserFeedbacks(Long userId) {
        return aiFeedbackLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}

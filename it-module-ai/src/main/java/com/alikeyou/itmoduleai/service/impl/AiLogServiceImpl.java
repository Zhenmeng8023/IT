package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.request.AiFeedbackCreateRequest;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiFeedbackLog;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.repository.AiCallLogRepository;
import com.alikeyou.itmoduleai.repository.AiFeedbackLogRepository;
import com.alikeyou.itmoduleai.repository.AiMessageRepository;
import com.alikeyou.itmoduleai.repository.AiRetrievalLogRepository;
import com.alikeyou.itmoduleai.repository.AiSessionRepository;
import com.alikeyou.itmoduleai.service.AiLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class AiLogServiceImpl implements AiLogService {

    private final AiCallLogRepository aiCallLogRepository;
    private final AiRetrievalLogRepository aiRetrievalLogRepository;
    private final AiFeedbackLogRepository aiFeedbackLogRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiSessionRepository aiSessionRepository;
    private final AiCurrentUserProvider currentUserProvider;

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
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "feedback request must not be null");
        }
        AiFeedbackLog entity = new AiFeedbackLog();
        entity.setUserId(currentUserProvider.requireCurrentUserId());
        entity.setFeedbackType(request.getFeedbackType());
        entity.setCommentText(request.getCommentText());
        entity.setCreatedAt(Instant.now());
        if (request.getCallLogId() != null) {
            entity.setCallLog(requireCallLogReadable(request.getCallLogId()));
        }
        if (request.getMessageId() != null) {
            entity.setMessage(requireMessageReadable(request.getMessageId()));
        }
        return aiFeedbackLogRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiCallLog> pageUserCallLogs(Long userId, Pageable pageable) {
        Long effectiveUserId = isLogAdmin() ? userId : currentUserProvider.requireCurrentUserId();
        return aiCallLogRepository.findByUserIdOrderByCreatedAtDesc(effectiveUserId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiCallLog> pageSessionCallLogs(Long sessionId, Pageable pageable) {
        requireSessionReadable(sessionId);
        return aiCallLogRepository.findBySession_IdOrderByCreatedAtDesc(sessionId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiRetrievalLog> listRetrievalLogs(Long callLogId) {
        requireCallLogReadable(callLogId);
        return aiRetrievalLogRepository.findByCallLog_IdOrderByRankNoAsc(callLogId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiFeedbackLog> listMessageFeedbacks(Long messageId) {
        requireMessageReadable(messageId);
        return aiFeedbackLogRepository.findByMessage_IdOrderByCreatedAtDesc(messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiFeedbackLog> listUserFeedbacks(Long userId) {
        Long effectiveUserId = isLogAdmin() ? userId : currentUserProvider.requireCurrentUserId();
        return aiFeedbackLogRepository.findByUserIdOrderByCreatedAtDesc(effectiveUserId);
    }

    private boolean isLogAdmin() {
        return currentUserProvider.hasAuthority("view:ai:log");
    }

    private void requireSessionReadable(Long sessionId) {
        if (isLogAdmin()) {
            return;
        }
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId must not be null");
        }
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        AiSession session = aiSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI session does not exist"));
        if (!Objects.equals(session.getUserId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AI log access denied");
        }
    }

    private AiCallLog requireCallLogReadable(Long callLogId) {
        AiCallLog callLog = loadCallLogRequired(callLogId);
        if (isLogAdmin()) {
            return callLog;
        }
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        if (Objects.equals(callLog.getUserId(), currentUserId)) {
            return callLog;
        }
        AiSession session = callLog.getSession();
        if (session != null && Objects.equals(session.getUserId(), currentUserId)) {
            return callLog;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AI log access denied");
    }

    private AiMessage requireMessageReadable(Long messageId) {
        AiMessage message = loadMessageRequired(messageId);
        if (isLogAdmin()) {
            return message;
        }
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        AiSession session = message.getSession();
        if (session != null && Objects.equals(session.getUserId(), currentUserId)) {
            return message;
        }
        if (Objects.equals(message.getSenderUserId(), currentUserId)) {
            return message;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AI log access denied");
    }

    private AiCallLog loadCallLogRequired(Long callLogId) {
        if (callLogId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "callLogId must not be null");
        }
        return aiCallLogRepository.findById(callLogId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI call log does not exist"));
    }

    private AiMessage loadMessageRequired(Long messageId) {
        if (messageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messageId must not be null");
        }
        return aiMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI message does not exist"));
    }
}

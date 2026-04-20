package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.dto.request.AiFeedbackCreateRequest;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiFeedbackLog;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AiLogService {

    AiCallLog saveCallLog(AiCallLog entity);

    List<AiRetrievalLog> saveRetrievalLogs(List<AiRetrievalLog> entities);

    AiCallLog getCallLog(Long callLogId);

    AiFeedbackLog saveFeedback(AiFeedbackCreateRequest request);

    Page<AiCallLog> pageUserCallLogs(Long userId, Pageable pageable);

    Page<AiCallLog> pageSessionCallLogs(Long sessionId, Pageable pageable);

    List<AiRetrievalLog> listRetrievalLogs(Long callLogId);

    List<AiFeedbackLog> listMessageFeedbacks(Long messageId);

    List<AiFeedbackLog> listUserFeedbacks(Long userId);
}

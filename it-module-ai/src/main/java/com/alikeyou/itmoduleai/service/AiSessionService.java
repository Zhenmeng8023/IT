package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.dto.request.AiMessageCreateRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.entity.AiSessionKnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AiSessionService {

    AiSession createSession(AiSessionCreateRequest request);

    AiSession getById(Long id);

    Page<AiSession> pageUserSessions(Long userId,
                                     AiSession.BizType bizType,
                                     Long knowledgeBaseId,
                                     AiSession.Status status,
                                     Pageable pageable);

    AiMessage createMessage(Long sessionId, AiMessageCreateRequest request);

    Page<AiMessage> pageMessages(Long sessionId, Pageable pageable);

    List<AiSessionKnowledgeBase> bindKnowledgeBases(Long sessionId, List<Long> knowledgeBaseIds);

    void archive(Long sessionId);

    void delete(Long sessionId);
}
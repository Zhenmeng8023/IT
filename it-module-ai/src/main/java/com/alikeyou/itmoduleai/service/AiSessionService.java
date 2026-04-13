package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.dto.request.AiMessageCreateRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionBindKnowledgeBaseRequest;
import com.alikeyou.itmoduleai.dto.request.AiSessionCreateRequest;
import com.alikeyou.itmoduleai.dto.response.AiMessageVO;
import com.alikeyou.itmoduleai.dto.response.AiSessionVO;
import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AiSessionService {

    AiSession createSession(AiSessionCreateRequest request);

    AiSession getById(Long id);

    AiSessionVO getSession(Long id);

    Page<AiSessionVO> pageCurrentUserSessions(AiSession.BizType bizType,
                                               Long knowledgeBaseId,
                                               AiSession.Status status,
                                               Pageable pageable);

    AiMessage createMessage(Long sessionId, AiMessageCreateRequest request);

    Page<AiMessageVO> pageMessages(Long sessionId, Pageable pageable);

    AiSessionVO bindKnowledgeBases(Long sessionId, AiSessionBindKnowledgeBaseRequest request);

    void archive(Long sessionId);

    void delete(Long sessionId);
}

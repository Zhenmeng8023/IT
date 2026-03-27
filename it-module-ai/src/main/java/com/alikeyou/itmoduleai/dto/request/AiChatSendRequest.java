package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiSession;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AiChatSendRequest {

    private Long sessionId;
    private Long userId;
    private String content;
    private Long modelId;
    private Long promptTemplateId;
    private AiCallLog.RequestType requestType;
    private List<Long> knowledgeBaseIds;
    private Map<String, Object> requestParams;

    private AiSession.BizType bizType;
    private Long bizId;
    private Long projectId;
    private String sceneCode;
    private String sessionTitle;
    private AiSession.MemoryMode memoryMode;
    private Long defaultKnowledgeBaseId;
}
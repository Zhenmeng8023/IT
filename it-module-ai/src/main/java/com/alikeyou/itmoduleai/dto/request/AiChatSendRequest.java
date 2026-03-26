package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.AiCallLog;
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
}

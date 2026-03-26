package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.AiSession;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSessionCreateRequest {

    private Long userId;
    private AiSession.BizType bizType;
    private Long bizId;
    private Long projectId;
    private String sceneCode;
    private String sessionTitle;
    private AiSession.MemoryMode memoryMode;
    private Long activeModelId;
    private Long promptTemplateId;
    private Long defaultKnowledgeBaseId;
    private String extConfig;
}

package com.alikeyou.itmoduleai.dto.response;

import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import com.alikeyou.itmoduleai.enums.GroundingStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AiChatStreamChunkResponse {

    private Long sessionId;
    private Long assistantMessageId;
    private Long callLogId;
    private String delta;
    private Boolean finished;
    private String finishReason;
    private List<Long> knowledgeBaseIds;
    private Long defaultKnowledgeBaseId;
    private Long recentKnowledgeBaseId;
    private AiAnalysisMode analysisMode;
    private Boolean strictGrounding;
    private String entryFile;
    private String symbolHint;
    private Integer traceDepth;
    private GroundingStatus groundingStatus;
    private Map<String, Object> retrievalSummary;
    private List<AiCitationResponse> citations;
}

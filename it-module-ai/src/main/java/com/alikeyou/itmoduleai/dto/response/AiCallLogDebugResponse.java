package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AiCallLogDebugResponse {
    private Long id;
    private Long userId;
    private Long sessionId;
    private Long messageId;
    private Long aiModelId;
    private String aiModelName;
    private String aiModelProviderCode;
    private String requestType;
    private String requestStage;
    private String status;
    private String requestText;
    private String responseText;
    private String requestParamsJson;
    private String retrievalSummaryJson;
    private String groundingReportJson;
    private String metadataJson;
    private String degradeReason;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal costAmount;
    private Integer latencyMs;
    private String errorCode;
    private String errorMessage;
    private Instant createdAt;
    private EmbeddingProfileView embeddingProfile;
    private String finalContextSource;
    private Map<String, Object> requestParams;
    private Map<String, Object> retrievalSummary;
    private Map<String, Object> groundingReport;
    private Map<String, Object> metadata;
    private List<AiRetrievalLogDebugResponse> retrievals;
}

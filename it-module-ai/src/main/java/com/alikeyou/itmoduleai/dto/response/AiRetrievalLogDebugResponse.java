package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class AiRetrievalLogDebugResponse {
    private Long id;
    private Long callLogId;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long documentId;
    private String documentTitle;
    private Long chunkId;
    private String chunkEmbeddingProvider;
    private String chunkEmbeddingModel;
    private String queryText;
    private String retrievalMethod;
    private String stageCode;
    private String phase;
    private Integer stageOrder;
    private String candidateSource;
    private BigDecimal score;
    private BigDecimal scoreKeyword;
    private BigDecimal scoreVector;
    private BigDecimal scoreGraph;
    private BigDecimal scoreRerank;
    private String rerankModel;
    private String groundingStatus;
    private String groundingEvidenceJson;
    private String degradeReason;
    private String hitReasonJson;
    private String scoreDetailJson;
    private Integer rankNo;
    private String metadataJson;
    private Instant createdAt;
}

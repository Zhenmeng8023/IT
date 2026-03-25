package com.alikeyou.itmoduleai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRetrievalLogDTO {
    private Long callLogId;
    private Long knowledgeBaseId;
    private Long documentId;
    private Long chunkId;
    private String queryText;
    private BigDecimal score;
    private Integer rankNo;
    private String retrievalMethod;
}
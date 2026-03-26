package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AiCitationResponse {

    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long documentId;
    private String documentTitle;
    private Long chunkId;
    private Integer chunkIndex;
    private String chunkTitle;
    private String snippet;
    private BigDecimal score;
    private Integer rankNo;
}

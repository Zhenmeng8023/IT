package com.alikeyou.itmoduleai.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCitationResponse {

    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    private Long documentId;
    private String documentTitle;
    private String fileName;
    private String archiveEntryPath;
    private String path;
    private Long chunkId;
    private Integer chunkIndex;
    private String chunkTitle;
    private String snippet;
    private BigDecimal score;
    private BigDecimal keywordScore;
    private BigDecimal vectorScore;
    private Integer rankNo;
    private String language;
    private String symbolName;
    private String symbolType;
    private Integer startLine;
    private Integer endLine;
    private String sectionName;
}

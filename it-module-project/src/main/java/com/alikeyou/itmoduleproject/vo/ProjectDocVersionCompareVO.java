package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectDocVersionCompareVO {
    private Long docId;
    private String docTitle;
    private String docType;
    private Integer currentVersionNo;

    private Integer leftVersionNo;
    private String leftContent;
    private Long leftEditedBy;
    private String leftChangeSummary;
    private LocalDateTime leftCreatedAt;

    private Integer rightVersionNo;
    private String rightContent;
    private Long rightEditedBy;
    private String rightChangeSummary;
    private LocalDateTime rightCreatedAt;
}

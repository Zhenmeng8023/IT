package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectDocVersionVO {
    private Long id;
    private Long docId;
    private Integer versionNo;
    private String contentSnapshot;
    private String changeSummary;
    private Long editedBy;
    private LocalDateTime createdAt;
    private String docTitle;
    private String docType;
}

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
    private String titleSnapshot;
    private String docTypeSnapshot;
    private String contentSnapshot;
    private String contentFormatSnapshot;
    private String summarySnapshot;
    private Boolean isMainReadmeSnapshot;
    private Boolean isPinnedHomeSnapshot;
    private Integer sortNoSnapshot;
    private String statusSnapshot;
    private String changeSummary;
    private Long operatorId;
    private String actionType;
    private LocalDateTime createdAt;
}

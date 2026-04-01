package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectDocListItemVO {
    private Long id;
    private Long projectId;
    private String title;
    private String docType;
    private String contentFormat;
    private String summary;
    private Boolean isMainReadme;
    private Boolean isPinnedHome;
    private Integer sortNo;
    private String status;
    private Integer latestVersionNo;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

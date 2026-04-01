package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectDocCreateRequest {
    private String title;
    private String docType;
    private String content;
    private String contentFormat;
    private String summary;
    private String changeSummary;
    private Boolean isMainReadme;
    private Boolean isPinnedHome;
    private Integer sortNo;
    private String status;
}

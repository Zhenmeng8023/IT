package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectDocCreateRequest {
    private String title;
    private String docType;
    private String status;
    private String visibility;
    private Boolean isPrimary;
    private String content;
    private String changeSummary;
}

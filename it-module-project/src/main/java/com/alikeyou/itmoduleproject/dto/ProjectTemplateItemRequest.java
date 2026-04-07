package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectTemplateItemRequest {
    private String itemType;
    private String name;
    private String path;
    private String content;
    private String docType;
    private String status;
    private String visibility;
    private Boolean isPrimary;
    private String priority;
    private String version;
    private String fileType;
    private Integer sortOrder;
}

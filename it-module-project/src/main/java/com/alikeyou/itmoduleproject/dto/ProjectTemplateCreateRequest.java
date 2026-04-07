package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectTemplateCreateRequest {
    private String name;
    private String description;
    private String category;
    private Boolean isPublic;
    private String readmeTitle;
    private String readmeContent;
    private List<ProjectTemplateItemRequest> items;
}

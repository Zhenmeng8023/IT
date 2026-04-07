package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectTemplateSelectableItemVO {
    private Long id;
    private String itemType;
    private String name;
    private String path;
    private String fileExt;
    private Long fileSize;
    private String status;
    private String subType;
    private String summary;
    private String action;
    private LocalDateTime createdAt;
    private Boolean primary;
}

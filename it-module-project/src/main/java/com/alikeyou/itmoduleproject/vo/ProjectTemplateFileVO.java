package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectTemplateFileVO {
    private Long id;
    private Long templateId;
    private String itemType;
    private String itemKey;
    private String groupName;
    private Long sourceId;
    private String fileName;
    private String filePath;
    private String fileExt;
    private Long fileSize;
    private String mimeType;
    private Boolean includeContent;
    private String version;
    private Integer sortOrder;
    private String payloadJson;
    private String previewText;
    private LocalDateTime createdAt;
}

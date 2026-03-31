package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskChecklistItemVO {
    private Long id;
    private Long taskId;
    private String content;
    private Boolean checked;
    private Integer sortOrder;
    private Long createdBy;
    private String creatorName;
    private Long checkedBy;
    private String checkerName;
    private LocalDateTime checkedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectTaskVO {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private Long assigneeId;
    private String assigneeName;
    private String assigneeAvatar;
    private Long createdBy;
    private String creatorName;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private Long completedBy;
    private LocalDateTime completedMemberJoinedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

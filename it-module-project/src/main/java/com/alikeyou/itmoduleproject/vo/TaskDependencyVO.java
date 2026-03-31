package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskDependencyVO {
    private Long id;
    private Long projectId;
    private Long predecessorTaskId;
    private String predecessorTaskTitle;
    private String predecessorTaskStatus;
    private Long successorTaskId;
    private String successorTaskTitle;
    private String successorTaskStatus;
    private String dependencyType;
    private LocalDateTime createdAt;
    private Boolean blocked;
}

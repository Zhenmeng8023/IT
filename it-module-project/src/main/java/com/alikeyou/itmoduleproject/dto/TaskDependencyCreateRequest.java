package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class TaskDependencyCreateRequest {
    private Long predecessorTaskId;
    private String dependencyType;
}

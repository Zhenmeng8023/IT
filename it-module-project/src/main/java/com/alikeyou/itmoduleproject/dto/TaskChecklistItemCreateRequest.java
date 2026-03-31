package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class TaskChecklistItemCreateRequest {
    private String content;
    private Integer sortOrder;
}

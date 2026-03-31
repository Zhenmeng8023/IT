package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class TaskChecklistItemUpdateRequest {
    private String content;
    private Integer sortOrder;
}

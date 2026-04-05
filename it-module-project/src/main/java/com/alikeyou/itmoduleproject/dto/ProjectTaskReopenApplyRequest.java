package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectTaskReopenApplyRequest {
    private String targetStatus;
    private String reason;
}

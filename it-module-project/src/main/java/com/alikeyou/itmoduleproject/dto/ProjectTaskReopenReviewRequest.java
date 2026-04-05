package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectTaskReopenReviewRequest {
    private String reviewRemark;
    private String approvedTargetStatus;
}

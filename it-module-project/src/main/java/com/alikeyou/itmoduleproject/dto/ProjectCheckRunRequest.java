package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectCheckRunRequest {
    private Long projectId;
    private Long commitId;
    private Long mergeRequestId;
    private String checkType;
    private String summary;
}

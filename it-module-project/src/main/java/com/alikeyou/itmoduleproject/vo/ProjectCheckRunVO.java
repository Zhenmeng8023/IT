package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectCheckRunVO {
    private Long id;
    private Long repositoryId;
    private Long commitId;
    private Long mergeRequestId;
    private String checkType;
    private String checkStatus;
    private String summary;
    private String logPath;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}

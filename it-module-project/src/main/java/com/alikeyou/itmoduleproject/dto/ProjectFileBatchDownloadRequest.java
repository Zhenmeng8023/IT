package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectFileBatchDownloadRequest {
    private Long projectId;
    private List<Long> fileIds;
}

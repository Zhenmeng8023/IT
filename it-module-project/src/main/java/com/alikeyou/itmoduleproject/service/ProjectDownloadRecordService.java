package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.entity.ProjectDownloadRecord;
import com.alikeyou.itmoduleproject.vo.PageResult;

import java.util.Map;

public interface ProjectDownloadRecordService {

    ProjectDownloadRecord recordDownload(Long projectId, Long fileId, Long fileVersionId, Long userId, String ipAddress, String userAgent);

    PageResult<ProjectDownloadRecord> pageRecords(Long projectId, int page, int size, Long currentUserId);

    Map<String, Object> getSummary(Long projectId, Long currentUserId);
}

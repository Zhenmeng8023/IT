package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.entity.ProjectRelease;
import com.alikeyou.itmoduleproject.entity.ProjectReleaseFile;
import com.alikeyou.itmoduleproject.vo.ProjectReleaseBindableFileVO;

import java.util.List;
import java.util.Map;

public interface ProjectReleaseService {

    List<ProjectRelease> listReleases(Long projectId, String status, Long currentUserId);

    Map<String, Object> getReleaseDetail(Long id, Long currentUserId);

    ProjectRelease createRelease(ProjectRelease request, Long currentUserId);

    ProjectRelease updateRelease(Long id, ProjectRelease request, Long currentUserId);

    ProjectRelease publishRelease(Long id, Long currentUserId);

    ProjectRelease archiveRelease(Long id, Long currentUserId);

    List<ProjectReleaseFile> bindProjectFiles(Long releaseId, List<Long> projectFileIds, Long currentUserId);

    void removeReleaseFile(Long releaseId, Long releaseFileId, Long currentUserId);

    Map<String, Object> getLatestReleaseSummary(Long projectId, Long currentUserId);

    List<ProjectReleaseBindableFileVO> listBindableFiles(Long projectId, Long commitId, Long currentUserId);
}

package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectDownloadRecord;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.repository.ProjectDownloadRecordRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectDownloadRecordService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectDownloadRecordServiceImpl implements ProjectDownloadRecordService {

    private final ProjectDownloadRecordRepository projectDownloadRecordRepository;
    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectActivityLogService projectActivityLogService;

    @Override
    @Transactional
    public ProjectDownloadRecord recordDownload(Long projectId, Long fileId, Long fileVersionId, Long userId, String ipAddress, String userAgent) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new BusinessException("项目不存在"));
        projectPermissionService.assertProjectReadable(projectId, userId);
        ProjectFile projectFile = null;
        if (fileId != null) {
            projectFile = projectFileRepository.findById(fileId).orElseThrow(() -> new BusinessException("项目文件不存在"));
            if (!projectId.equals(projectFile.getProjectId())) {
                throw new BusinessException("项目文件不属于当前项目");
            }
        }
        if (fileVersionId != null && projectFileVersionRepository.findById(fileVersionId).isEmpty()) {
            throw new BusinessException("项目文件版本不存在");
        }
        ProjectDownloadRecord saved = projectDownloadRecordRepository.save(ProjectDownloadRecord.builder()
                .projectId(projectId)
                .fileId(fileId)
                .fileVersionId(fileVersionId)
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build());
        project.setDownloads((project.getDownloads() == null ? 0 : project.getDownloads()) + 1);
        projectRepository.save(project);
        String content = projectFile == null ? "下载项目文件" : "下载文件：" + projectFile.getFileName();
        projectActivityLogService.record(projectId, userId, "download_project_file", fileId == null ? "project" : "file", fileId == null ? projectId : fileId, content);
        return saved;
    }

    @Override
    public PageResult<ProjectDownloadRecord> pageRecords(Long projectId, int page, int size, Long currentUserId) {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        Page<ProjectDownloadRecord> result = projectDownloadRecordRepository.findByProjectIdOrderByDownloadedAtDesc(projectId, PageRequest.of(page - 1, size));
        return new PageResult<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @Override
    public Map<String, Object> getSummary(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new BusinessException("项目不存在"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("projectId", projectId);
        map.put("totalDownloadCount", projectDownloadRecordRepository.countByProjectId(projectId));
        map.put("uniqueDownloadUserCount", projectDownloadRecordRepository.countDistinctUserIdByProjectId(projectId));
        map.put("projectDownloadsField", project.getDownloads() == null ? 0 : project.getDownloads());
        return map;
    }
}

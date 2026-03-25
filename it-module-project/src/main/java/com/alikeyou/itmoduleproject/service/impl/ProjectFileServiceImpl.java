package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.service.ProjectFileService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.FileStorageService;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.support.StoredFileInfo;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectPermissionService projectPermissionService;
    private final FileStorageService fileStorageService;
    private final com.alikeyou.itmoduleproject.service.impl.ProjectSizeSyncService projectSizeSyncService;

    @Override
    @Transactional
    public ProjectFileVO uploadFile(Long projectId, MultipartFile file, Boolean isMain, String version, String commitMessage, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        StoredFileInfo stored = fileStorageService.store(projectId, "main", file);
        String finalVersion = StringUtils.hasText(version) ? version : "1.0";
        if (Boolean.TRUE.equals(isMain)) {
            clearProjectMainFile(projectId);
        }
        ProjectFile saved = projectFileRepository.save(ProjectFile.builder()
            .projectId(projectId)
            .fileName(stored.getOriginalFilename())
            .filePath(stored.getStoredPath())
            .fileSizeBytes(stored.getSize())
            .fileType(stored.getExtension())
            .isMain(Boolean.TRUE.equals(isMain))
            .version(finalVersion)
            .isLatest(true)
            .build());
        projectFileVersionRepository.save(ProjectFileVersion.builder()
            .fileId(saved.getId())
            .version(finalVersion)
            .serverPath(saved.getFilePath())
            .fileSizeBytes(saved.getFileSizeBytes())
            .uploadedBy(currentUserId)
            .commitMessage(commitMessage)
            .build());
        projectSizeSyncService.syncProjectSize(projectId);
        return toFileVO(saved);
    }

    @Override
    @Transactional
    public ProjectFileVO uploadNewVersion(Long fileId, MultipartFile file, String version, String commitMessage, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectWritable(projectFile.getProjectId(), currentUserId);
        String finalVersion = StringUtils.hasText(version) ? version : incrementVersion(projectFile.getVersion());
        StoredFileInfo stored = fileStorageService.store(projectFile.getProjectId(), "version", file);
        projectFile.setFilePath(stored.getStoredPath());
        projectFile.setFileSizeBytes(stored.getSize());
        projectFile.setFileType(stored.getExtension());
        projectFile.setVersion(finalVersion);
        projectFile.setIsLatest(true);
        projectFileRepository.save(projectFile);
        projectFileVersionRepository.save(ProjectFileVersion.builder()
            .fileId(projectFile.getId())
            .version(finalVersion)
            .serverPath(stored.getStoredPath())
            .fileSizeBytes(stored.getSize())
            .uploadedBy(currentUserId)
            .commitMessage(commitMessage)
            .build());
        projectSizeSyncService.syncProjectSize(projectFile.getProjectId());
        return toFileVO(projectFile);
    }

    @Override
    public List<ProjectFileVO> listFiles(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        return projectFileRepository.findByProjectIdOrderByUploadTimeDesc(projectId)
            .stream()
            .map(this::toFileVO)
            .toList();
    }

    @Override
    public List<ProjectFileVersionVO> listVersions(Long fileId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectReadable(projectFile.getProjectId(), currentUserId);
        return projectFileVersionRepository.findByFileIdOrderByUploadedAtDesc(fileId)
            .stream()
            .map(ProjectVoMapper::toProjectFileVersionVO)
            .toList();
    }

    @Override
    public Resource downloadFile(Long fileId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectReadable(projectFile.getProjectId(), currentUserId);
        return fileStorageService.loadAsResource(projectFile.getFilePath());
    }

    private ProjectFile getProjectFile(Long fileId) {
        return projectFileRepository.findById(fileId)
            .orElseThrow(() -> new BusinessException("项目文件不存在"));
    }

    private void clearProjectMainFile(Long projectId) {
        List<ProjectFile> mainFiles = projectFileRepository.findByProjectIdAndIsMainTrue(projectId);
        for (ProjectFile mainFile : mainFiles) {
            mainFile.setIsMain(false);
            projectFileRepository.save(mainFile);
        }
    }

    private String incrementVersion(String oldVersion) {
        if (!StringUtils.hasText(oldVersion)) {
            return "1.0";
        }
        String[] parts = oldVersion.split("\\.");
        try {
            int last = Integer.parseInt(parts[parts.length - 1]);
            parts[parts.length - 1] = String.valueOf(last + 1);
            return String.join(".", parts);
        } catch (NumberFormatException e) {
            return oldVersion + ".1";
        }
    }

    private ProjectFileVO toFileVO(ProjectFile projectFile) {
        List<ProjectFileVersionVO> versions = projectFileVersionRepository.findByFileIdOrderByUploadedAtDesc(projectFile.getId())
            .stream()
            .map(ProjectVoMapper::toProjectFileVersionVO)
            .toList();
        return ProjectVoMapper.toProjectFileVO(projectFile, versions);
    }
}

package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectRepository projectRepository;
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
    @Transactional
    public Resource downloadFile(Long fileId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectReadable(projectFile.getProjectId(), currentUserId);
        incrementProjectDownloads(projectFile.getProjectId());
        return fileStorageService.loadAsResource(projectFile.getFilePath());
    }

    @Override
    @Transactional
    public ProjectFileVO setMainFile(Long fileId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectWritable(projectFile.getProjectId(), currentUserId);
        clearProjectMainFile(projectFile.getProjectId());
        projectFile.setIsMain(true);
        ProjectFile saved = projectFileRepository.save(projectFile);
        return toFileVO(saved);
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectWritable(projectFile.getProjectId(), currentUserId);

        List<ProjectFileVersion> versions = projectFileVersionRepository.findByFileIdOrderByUploadedAtDesc(fileId);
        Set<String> paths = new LinkedHashSet<>();
        if (StringUtils.hasText(projectFile.getFilePath())) {
            paths.add(projectFile.getFilePath());
        }
        for (ProjectFileVersion version : versions) {
            if (StringUtils.hasText(version.getServerPath())) {
                paths.add(version.getServerPath());
            }
        }

        projectFileVersionRepository.deleteAll(versions);
        boolean wasMain = Boolean.TRUE.equals(projectFile.getIsMain());
        Long projectId = projectFile.getProjectId();
        projectFileRepository.delete(projectFile);

        for (String path : paths) {
            fileStorageService.delete(path);
        }

        if (wasMain) {
            List<ProjectFile> remaining = projectFileRepository.findByProjectIdOrderByUploadTimeDesc(projectId);
            if (!remaining.isEmpty()) {
                ProjectFile replacement = remaining.get(0);
                replacement.setIsMain(true);
                projectFileRepository.save(replacement);
            }
        }
        projectSizeSyncService.syncProjectSize(projectId);
    }

    private void incrementProjectDownloads(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
        int downloads = project.getDownloads() == null ? 0 : project.getDownloads();
        project.setDownloads(downloads + 1);
        projectRepository.save(project);
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

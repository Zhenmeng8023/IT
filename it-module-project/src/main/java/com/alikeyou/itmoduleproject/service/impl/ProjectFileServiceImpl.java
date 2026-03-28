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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectPermissionService projectPermissionService;
    private final FileStorageService fileStorageService;
    private final ProjectSizeSyncService projectSizeSyncService;

    @Override
    @Transactional
    public ProjectFileVO uploadFile(Long projectId, MultipartFile file, Boolean isMain, String version, String commitMessage, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        ProjectFileVO result = uploadFileInternal(projectId, file, Boolean.TRUE.equals(isMain), version, commitMessage, currentUserId);
        projectSizeSyncService.syncProjectSize(projectId);
        return result;
    }

    @Override
    @Transactional
    public List<ProjectFileVO> uploadFiles(Long projectId, List<MultipartFile> files, Integer mainFileIndex, String version, String commitMessage, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        if (files == null || files.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        List<MultipartFile> validFiles = files.stream()
            .filter(Objects::nonNull)
            .filter(file -> !file.isEmpty())
            .toList();

        if (validFiles.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        Integer actualMainIndex = null;
        if (mainFileIndex != null && mainFileIndex >= 0 && mainFileIndex < validFiles.size()) {
            actualMainIndex = mainFileIndex;
            clearProjectMainFile(projectId);
        }

        List<ProjectFileVO> result = new ArrayList<>();
        for (int i = 0; i < validFiles.size(); i++) {
            boolean isMain = actualMainIndex != null && actualMainIndex == i;
            result.add(uploadFileInternal(projectId, validFiles.get(i), isMain, version, commitMessage, currentUserId));
        }

        projectSizeSyncService.syncProjectSize(projectId);
        return result;
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
    public Resource previewFile(Long fileId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectReadable(projectFile.getProjectId(), currentUserId);
        return fileStorageService.loadAsResource(projectFile.getFilePath());
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
    public Resource downloadFiles(Long projectId, List<Long> fileIds, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);

        List<ProjectFile> files;
        if (fileIds == null || fileIds.isEmpty()) {
            files = projectFileRepository.findByProjectIdOrderByUploadTimeDesc(projectId);
        } else {
            List<Long> distinctIds = fileIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

            files = projectFileRepository.findByProjectIdAndIdInOrderByUploadTimeDesc(projectId, distinctIds);

            if (files.size() != distinctIds.size()) {
                throw new BusinessException("部分文件不存在或不属于当前项目");
            }
        }

        if (files.isEmpty()) {
            throw new BusinessException("没有可下载的文件");
        }

        byte[] zipBytes = buildZipBytes(files);
        incrementProjectDownloads(projectId);
        return new NamedByteArrayResource(zipBytes, "project-" + projectId + "-files.zip");
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

    private ProjectFileVO uploadFileInternal(Long projectId, MultipartFile file, boolean isMain, String version, String commitMessage, Long currentUserId) {
        StoredFileInfo stored = fileStorageService.store(projectId, "main", file);
        String finalVersion = StringUtils.hasText(version) ? version : "1.0";

        if (isMain) {
            clearProjectMainFile(projectId);
        }

        ProjectFile saved = projectFileRepository.save(ProjectFile.builder()
            .projectId(projectId)
            .fileName(stored.getOriginalFilename())
            .filePath(stored.getStoredPath())
            .fileSizeBytes(stored.getSize())
            .fileType(stored.getExtension())
            .isMain(isMain)
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

        return toFileVO(saved);
    }

    private byte[] buildZipBytes(List<ProjectFile> files) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            Set<String> usedNames = new HashSet<>();
            int writtenCount = 0;

            for (ProjectFile file : files) {
                Resource resource = fileStorageService.loadAsResource(file.getFilePath());
                if (resource == null || !resource.exists()) {
                    continue;
                }

                String entryName = buildUniqueEntryName(usedNames, file.getFileName(), file.getVersion(), file.getId());
                zos.putNextEntry(new ZipEntry(entryName));

                try (InputStream inputStream = resource.getInputStream()) {
                    inputStream.transferTo(zos);
                }

                zos.closeEntry();
                writtenCount++;
            }

            if (writtenCount == 0) {
                throw new BusinessException("没有可打包的文件");
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("打包下载失败：" + e.getMessage());
        }
    }

    private String buildUniqueEntryName(Set<String> usedNames, String fileName, String version, Long fileId) {
        String safeName = StringUtils.hasText(fileName) ? fileName : "file-" + fileId;
        if (usedNames.add(safeName)) {
            return safeName;
        }

        String extension = StringUtils.getFilenameExtension(safeName);
        String ext = StringUtils.hasText(extension) ? "." + extension : "";
        String base = StringUtils.hasText(extension) ? safeName.substring(0, safeName.length() - ext.length()) : safeName;
        String suffix = StringUtils.hasText(version) ? "-v" + version : "";
        String candidate = base + suffix + ext;
        int index = 1;

        while (!usedNames.add(candidate)) {
            candidate = base + suffix + "-" + index + ext;
            index++;
        }

        return candidate;
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

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}

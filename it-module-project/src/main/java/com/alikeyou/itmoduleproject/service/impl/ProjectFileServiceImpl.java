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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private static final int MAX_ZIP_ENTRY_COUNT = 4500;
    private static final long MAX_SINGLE_ENTRY_BYTES = 20L * 1024 * 1024;
    private static final long MAX_TOTAL_UNZIP_BYTES = 200L * 1024 * 1024;
    private static final Set<String> IGNORED_PATH_SEGMENTS = Set.of(
            ".git", ".idea", "node_modules", "target", "dist", "build", "out", ".gradle", "__MACOSX"
    );

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
    public List<ProjectFileVO> uploadZip(Long projectId, MultipartFile file, String version, String commitMessage, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传 ZIP 不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = normalizeExtension(null, originalFilename);
        if (!"zip".equalsIgnoreCase(extension)) {
            throw new BusinessException("只能上传 zip 压缩包");
        }

        List<ZipEntryPayload> payloads = extractZipEntries(file);
        if (payloads.isEmpty()) {
            throw new BusinessException("ZIP 中没有可导入的文件");
        }

        String mainEntryPath = chooseMainEntryPath(payloads);
        clearProjectMainFile(projectId);

        String finalVersion = StringUtils.hasText(version) ? version.trim() : "1.0.0";
        String finalCommitMessage = StringUtils.hasText(commitMessage) ? commitMessage.trim() : "ZIP 导入项目文件";

        List<ProjectFileVO> result = new ArrayList<>();
        for (ZipEntryPayload payload : payloads) {
            MultipartFile multipartFile = new ByteArrayMultipartFile(
                    payload.relativePath(),
                    payload.relativePath(),
                    MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    payload.bytes()
            );
            boolean isMain = payload.relativePath().equals(mainEntryPath);
            result.add(uploadFileInternal(projectId, multipartFile, isMain, finalVersion, finalCommitMessage, currentUserId));
        }

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
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String finalVersion = resolveNextVersion(projectFile.getId(), projectFile.getVersion(), version);
        StoredFileInfo stored = fileStorageService.store(projectFile.getProjectId(), "version", file);
        projectFile.setFileName(resolveOriginalFilename(stored, file));
        projectFile.setFilePath(stored.getStoredPath());
        projectFile.setFileSizeBytes(stored.getSize());
        projectFile.setFileType(normalizeExtension(stored.getExtension(), file.getOriginalFilename()));
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
        String finalVersion = StringUtils.hasText(version) ? version.trim() : "1.0.0";
        if (isMain) {
            clearProjectMainFile(projectId);
        }
        ProjectFile saved = projectFileRepository.save(ProjectFile.builder()
                .projectId(projectId)
                .fileName(resolveOriginalFilename(stored, file))
                .filePath(stored.getStoredPath())
                .fileSizeBytes(stored.getSize())
                .fileType(normalizeExtension(stored.getExtension(), file.getOriginalFilename()))
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

    private List<ZipEntryPayload> extractZipEntries(MultipartFile zipFile) {
        List<ZipEntryPayload> result = new ArrayList<>();
        Set<String> seenPaths = new LinkedHashSet<>();
        long totalBytes = 0L;

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String relativePath = sanitizeZipEntryName(entry.getName());
                if (!StringUtils.hasText(relativePath) || shouldIgnoreZipEntry(relativePath)) {
                    continue;
                }
                if (!seenPaths.add(relativePath)) {
                    continue;
                }
                if (result.size() >= MAX_ZIP_ENTRY_COUNT) {
                    throw new BusinessException("ZIP 中文件过多，请精简后再上传");
                }

                byte[] bytes = readZipEntryBytes(zis, relativePath);
                totalBytes += bytes.length;
                if (totalBytes > MAX_TOTAL_UNZIP_BYTES) {
                    throw new BusinessException("ZIP 解压后的总大小过大，请拆分后再上传");
                }
                result.add(new ZipEntryPayload(relativePath, bytes));
            }
        } catch (IOException e) {
            throw new BusinessException("读取 ZIP 失败：" + e.getMessage());
        }

        return result;
    }

    private byte[] readZipEntryBytes(ZipInputStream zis, String relativePath) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            long currentSize = 0L;
            while ((len = zis.read(buffer)) != -1) {
                currentSize += len;
                if (currentSize > MAX_SINGLE_ENTRY_BYTES) {
                    throw new BusinessException("ZIP 内单个文件过大：" + relativePath);
                }
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    private boolean shouldIgnoreZipEntry(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.endsWith("/.ds_store") || lower.endsWith(".ds_store") || lower.endsWith("thumbs.db")) {
            return true;
        }
        String[] segments = normalized.split("/");
        for (String segment : segments) {
            if (IGNORED_PATH_SEGMENTS.contains(segment)) {
                return true;
            }
        }
        return false;
    }

    private String sanitizeZipEntryName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String normalized = name.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!StringUtils.hasText(normalized) || normalized.contains("../") || normalized.equals("..")) {
            return null;
        }
        return normalized;
    }

    private String chooseMainEntryPath(List<ZipEntryPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return null;
        }
        List<String> preferred = List.of(
                "readme.md", "readme.txt", "pom.xml", "package.json", "build.gradle", "settings.gradle"
        );
        for (String target : preferred) {
            for (ZipEntryPayload payload : payloads) {
                String path = payload.relativePath().replace('\\', '/');
                String lower = path.toLowerCase(Locale.ROOT);
                if (lower.equals(target) || lower.endsWith("/" + target)) {
                    return payload.relativePath();
                }
            }
        }
        return payloads.get(0).relativePath();
    }

    private String resolveNextVersion(Long fileId, String currentVersion, String requestedVersion) {
        if (StringUtils.hasText(requestedVersion)) {
            String trimmed = requestedVersion.trim();
            if (projectFileVersionRepository.existsByFileIdAndVersion(fileId, trimmed)) {
                throw new BusinessException("版本号已存在，请更换一个新的版本号");
            }
            return trimmed;
        }
        String candidate = incrementVersion(currentVersion);
        int guard = 0;
        while (projectFileVersionRepository.existsByFileIdAndVersion(fileId, candidate)) {
            candidate = incrementVersion(candidate);
            guard++;
            if (guard > 100) {
                throw new BusinessException("无法自动生成新的版本号，请手动填写版本号");
            }
        }
        return candidate;
    }

    private String resolveOriginalFilename(StoredFileInfo stored, MultipartFile file) {
        if (stored != null && StringUtils.hasText(stored.getOriginalFilename())) {
            return stored.getOriginalFilename();
        }
        if (file != null && StringUtils.hasText(file.getOriginalFilename())) {
            return file.getOriginalFilename();
        }
        return "未命名文件";
    }

    private String normalizeExtension(String extension, String fallbackFilename) {
        String value = extension;
        if (!StringUtils.hasText(value) && StringUtils.hasText(fallbackFilename)) {
            value = StringUtils.getFilenameExtension(fallbackFilename);
        }
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "bin";
    }

    private byte[] buildZipBytes(List<ProjectFile> files) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos)) {
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
            return "1.0.0";
        }
        String[] parts = oldVersion.trim().split("\\.");
        try {
            int last = Integer.parseInt(parts[parts.length - 1]);
            parts[parts.length - 1] = String.valueOf(last + 1);
            return String.join(".", parts);
        } catch (NumberFormatException e) {
            return oldVersion.trim() + ".1";
        }
    }

    private ProjectFileVO toFileVO(ProjectFile projectFile) {
        List<ProjectFileVersionVO> versions = projectFileVersionRepository.findByFileIdOrderByUploadedAtDesc(projectFile.getId())
                .stream()
                .map(ProjectVoMapper::toProjectFileVersionVO)
                .toList();
        return ProjectVoMapper.toProjectFileVO(projectFile, versions);
    }

    private record ZipEntryPayload(String relativePath, byte[] bytes) {
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

    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        private ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes == null ? new byte[0] : bytes;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
    }
}

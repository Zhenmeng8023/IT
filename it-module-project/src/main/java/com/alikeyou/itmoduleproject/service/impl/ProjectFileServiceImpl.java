package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.service.ProjectCodeRepositoryService;
import com.alikeyou.itmoduleproject.service.ProjectFileService;
import com.alikeyou.itmoduleproject.service.ProjectWorkspaceService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.FileStorageService;
import com.alikeyou.itmoduleproject.support.ProjectFileTypeSupport;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectPathUtils;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.support.StoredFileInfo;
import com.alikeyou.itmoduleproject.vo.ProjectCodeRepositoryVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
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
            ".git", ".svn", ".hg",
            ".idea", ".vscode",
            "node_modules", "target", "dist", "build", "out",
            ".gradle", ".next", ".nuxt", ".cache", "coverage",
            "__MACOSX"
    );

    private static final Set<String> IGNORED_FILE_NAMES = Set.of(
            ".ds_store", "thumbs.db", "desktop.ini",
            "npm-debug.log", "yarn-error.log", "pnpm-debug.log"
    );

    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            "class", "pyc", "pyo", "o", "obj", "tmp", "swp", "log", "iml"
    );

    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectPermissionService projectPermissionService;
    private final FileStorageService fileStorageService;
    private final ProjectWorkspaceService projectWorkspaceService;
    private final ProjectCodeRepositoryService projectCodeRepositoryService;
    private final ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport;

    @Override
    @Transactional
    public ProjectWorkspaceItemVO uploadFile(Long projectId, Long branchId, String canonicalPath, MultipartFile file, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        Long resolvedBranchId = resolveBranchId(projectId, branchId, currentUserId);
        return projectWorkspaceService.stageFile(
                projectId,
                resolvedBranchId,
                currentUserId,
                resolveUploadPath(canonicalPath, file),
                file
        );
    }

    @Override
    @Transactional
    public List<ProjectWorkspaceItemVO> uploadZip(Long projectId, Long branchId, MultipartFile file, Long currentUserId) {
        throw new BusinessException("ZIP 上传已在当前阶段临时关闭，请改用单文件或批量上传");
    }

    @Override
    @Transactional
    public List<ProjectWorkspaceItemVO> uploadFiles(Long projectId, Long branchId, String targetDir, List<MultipartFile> files, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        Long resolvedBranchId = resolveBranchId(projectId, branchId, currentUserId);
        return projectWorkspaceService.stageFiles(projectId, resolvedBranchId, currentUserId, targetDir, files);
    }

    @Override
    @Transactional
    public ProjectWorkspaceItemVO uploadNewVersion(Long fileId, Long branchId, MultipartFile file, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectWritable(projectFile.getProjectId(), currentUserId);
        Long resolvedBranchId = resolveBranchId(projectFile.getProjectId(), branchId, currentUserId);
        return projectWorkspaceService.stageFile(
                projectFile.getProjectId(),
                resolvedBranchId,
                currentUserId,
                resolveExistingCanonicalPath(projectFile),
                file
        );
    }

    @Override
    public List<ProjectFileVO> listFiles(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        projectCodeRepositoryRepository.findByProjectId(projectId)
                .ifPresent(repository -> projectRepositoryBootstrapSupport.ensureRepositorySnapshotInitialized(repository, currentUserId));
        return projectFileRepository.findByProjectIdAndDeletedFlagFalseOrderByUploadTimeDesc(projectId)
                .stream()
                .map(this::toFileVO)
                .toList();
    }

    @Override
    public List<ProjectFileVersionVO> listVersions(Long fileId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectReadable(projectFile.getProjectId(), currentUserId);
        if (isFolderRecord(projectFile)) {
            return List.of();
        }
        return projectFileVersionRepository.findByFileIdOrderByUploadedAtDesc(fileId)
                .stream()
                .map(ProjectVoMapper::toProjectFileVersionVO)
                .toList();
    }

    @Override
    public Resource previewFile(Long fileId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectReadable(projectFile.getProjectId(), currentUserId);
        if (isFolderRecord(projectFile)) {
            throw new BusinessException("文件夹不支持在线预览");
        }
        if (!StringUtils.hasText(projectFile.getFilePath())) {
            throw new BusinessException("文件路径不存在");
        }
        Resource resource = fileStorageService.loadAsResource(projectFile.getFilePath());
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            throw new BusinessException("文件不存在或不可读");
        }
        return resource;
    }

    @Override
    @Transactional
    public Resource downloadFile(Long fileId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectReadable(projectFile.getProjectId(), currentUserId);
        if (isFolderRecord(projectFile)) {
            throw new BusinessException("文件夹不支持下载，请下载具体文件");
        }
        if (!StringUtils.hasText(projectFile.getFilePath())) {
            throw new BusinessException("文件路径不存在");
        }
        Resource resource = fileStorageService.loadAsResource(projectFile.getFilePath());
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            throw new BusinessException("文件不存在或不可读");
        }
        return resource;
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
    public ProjectWorkspaceItemVO deleteFile(Long fileId, Long branchId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectWritable(projectFile.getProjectId(), currentUserId);
        Long resolvedBranchId = resolveBranchId(projectFile.getProjectId(), branchId, currentUserId);
        return projectWorkspaceService.stageDelete(
                projectFile.getProjectId(),
                resolvedBranchId,
                currentUserId,
                resolveExistingCanonicalPath(projectFile)
        );
    }

    private Long resolveBranchId(Long projectId, Long branchId, Long currentUserId) {
        ProjectCodeRepositoryVO repository = projectCodeRepositoryService.getByProjectId(projectId, currentUserId);
        if (repository == null) {
            repository = projectCodeRepositoryService.initRepository(projectId, currentUserId);
        }
        Long resolvedBranchId = branchId != null ? branchId : repository.getDefaultBranchId();
        if (resolvedBranchId == null) {
            throw new BusinessException("项目默认分支不存在，请先初始化仓库");
        }
        return resolvedBranchId;
    }

    private String resolveUploadPath(String canonicalPath, MultipartFile file) {
        if (StringUtils.hasText(canonicalPath)) {
            return ProjectPathUtils.normalize(canonicalPath);
        }
        String originalFilename = file == null ? null : file.getOriginalFilename();
        String fileName = StringUtils.hasText(originalFilename)
                ? originalFilename.replace('\\', '/').trim()
                : "unnamed-file";
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        }
        return ProjectPathUtils.normalize("/" + fileName);
    }

    private String resolveExistingCanonicalPath(ProjectFile projectFile) {
        if (projectFile != null && StringUtils.hasText(projectFile.getCanonicalPath())) {
            return ProjectPathUtils.normalize(projectFile.getCanonicalPath());
        }
        if (projectFile != null && StringUtils.hasText(projectFile.getFileName())) {
            return ProjectPathUtils.normalize("/" + projectFile.getFileName());
        }
        throw new BusinessException("项目文件缺少规范路径，无法加入工作区");
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

    private ProjectFile upsertImportedZipFile(Long projectId,
                                              String relativePath,
                                              TempExtractedFile tempFile,
                                              String requestedVersion,
                                              String commitMessage,
                                              Long currentUserId,
                                              List<String> createdPaths) {
        Optional<ProjectFile> existingOpt = projectFileRepository.findByProjectIdAndFileName(projectId, relativePath);

        String actualFilename = StringUtils.getFilename(relativePath);
        if (!StringUtils.hasText(actualFilename)) {
            actualFilename = relativePath.replace('/', '_');
        }

        MultipartFile multipartFile = new TempFileMultipartFile(
                actualFilename,
                actualFilename,
                "application/octet-stream",
                tempFile.path()
        );

        String folder = existingOpt.isPresent() ? "version" : "main";
        StoredFileInfo stored = fileStorageService.store(projectId, folder, multipartFile);
        if (StringUtils.hasText(stored.getStoredPath())) {
            createdPaths.add(stored.getStoredPath());
        }

        if (existingOpt.isPresent()) {
            ProjectFile existing = existingOpt.get();
            String finalVersion = resolveZipVersion(existing.getId(), existing.getVersion(), requestedVersion);

            existing.setFileName(relativePath);
            existing.setFilePath(stored.getStoredPath());
            existing.setFileSizeBytes(stored.getSize());
            existing.setFileType(normalizeExtension(stored.getExtension(), relativePath));
            existing.setVersion(finalVersion);
            existing.setIsLatest(true);

            ProjectFile saved = projectFileRepository.save(existing);

            projectFileVersionRepository.save(ProjectFileVersion.builder()
                    .fileId(saved.getId())
                    .version(finalVersion)
                    .serverPath(stored.getStoredPath())
                    .fileSizeBytes(stored.getSize())
                    .uploadedBy(currentUserId)
                    .commitMessage(commitMessage)
                    .build());

            return saved;
        }

        String finalVersion = StringUtils.hasText(requestedVersion) ? requestedVersion.trim() : "1.0.0";

        ProjectFile saved = projectFileRepository.save(ProjectFile.builder()
                .projectId(projectId)
                .fileName(relativePath)
                .filePath(stored.getStoredPath())
                .fileSizeBytes(stored.getSize())
                .fileType(normalizeExtension(stored.getExtension(), relativePath))
                .isMain(false)
                .version(finalVersion)
                .isLatest(true)
                .build());

        projectFileVersionRepository.save(ProjectFileVersion.builder()
                .fileId(saved.getId())
                .version(finalVersion)
                .serverPath(stored.getStoredPath())
                .fileSizeBytes(stored.getSize())
                .uploadedBy(currentUserId)
                .commitMessage(commitMessage)
                .build());

        return saved;
    }

    private String resolveZipVersion(Long fileId, String currentVersion, String requestedVersion) {
        if (StringUtils.hasText(requestedVersion)) {
            String v = requestedVersion.trim();
            if (!projectFileVersionRepository.existsByFileIdAndVersion(fileId, v)) {
                return v;
            }
        }
        return resolveNextVersion(fileId, currentVersion, null);
    }

    private TempExtractedFile streamZipEntryToTempFile(ZipInputStream zis, String relativePath) throws IOException {
        String ext = StringUtils.getFilenameExtension(relativePath);
        String suffix = StringUtils.hasText(ext) ? "." + ext : ".tmp";
        Path tempPath = Files.createTempFile("project-zip-", suffix);

        long size = 0L;
        try (OutputStream os = Files.newOutputStream(tempPath)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = zis.read(buffer)) != -1) {
                size += len;
                if (size > MAX_SINGLE_ENTRY_BYTES) {
                    throw new BusinessException("ZIP 内单个文件过大：" + relativePath);
                }
                os.write(buffer, 0, len);
            }
        } catch (Exception e) {
            Files.deleteIfExists(tempPath);
            throw e;
        }

        return new TempExtractedFile(tempPath, size);
    }

    private ZipLayout detectZipLayout(MultipartFile zipFile, String originalFilename) {
        Set<String> firstSegments = new LinkedHashSet<>();
        boolean hasRootFile = false;

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String normalized = sanitizeZipEntryName(entry.getName());
                if (!StringUtils.hasText(normalized)) {
                    zis.closeEntry();
                    continue;
                }

                int idx = normalized.indexOf('/');
                if (idx < 0) {
                    hasRootFile = true;
                    zis.closeEntry();
                    break;
                }

                firstSegments.add(normalized.substring(0, idx));
                zis.closeEntry();

                if (firstSegments.size() > 1) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new BusinessException("读取 ZIP 失败：" + e.getMessage());
        }

        if (hasRootFile || firstSegments.size() != 1) {
            return new ZipLayout(null);
        }

        String candidate = firstSegments.iterator().next();
        String zipBase = StringUtils.stripFilenameExtension(
                StringUtils.hasText(originalFilename) ? originalFilename.trim() : ""
        );
        String a = candidate.toLowerCase(Locale.ROOT);
        String b = zipBase == null ? "" : zipBase.toLowerCase(Locale.ROOT);

        boolean strip = StringUtils.hasText(b) && (
                a.equals(b)
                        || a.startsWith(b + "-")
                        || a.equals(b + "-main")
                        || a.equals(b + "-master")
                        || a.endsWith("-main")
                        || a.endsWith("-master")
        );

        return new ZipLayout(strip ? candidate : null);
    }

    private String normalizeZipEntryPath(String name, String rootPrefix) {
        String normalized = sanitizeZipEntryName(name);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        if (StringUtils.hasText(rootPrefix) && normalized.startsWith(rootPrefix + "/")) {
            normalized = normalized.substring(rootPrefix.length() + 1);
        }

        normalized = normalized.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private boolean shouldIgnoreZipEntry(String relativePath) {
        String normalized = relativePath.replace('\\', '/').trim();
        if (!StringUtils.hasText(normalized)) {
            return true;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        String fileName = lower.substring(lower.lastIndexOf('/') + 1);

        if (IGNORED_FILE_NAMES.contains(fileName)) {
            return true;
        }

        String[] segments = lower.split("/");
        for (String segment : segments) {
            if (IGNORED_PATH_SEGMENTS.contains(segment)) {
                return true;
            }
        }

        String ext = StringUtils.getFilenameExtension(fileName);
        return StringUtils.hasText(ext) && IGNORED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    private String sanitizeZipEntryName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String normalized = name.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.equals("..")
                || normalized.startsWith("../")
                || normalized.contains("/../")
                || normalized.contains(":/")
                || normalized.startsWith("~")) {
            return null;
        }
        return normalized;
    }

    private int scoreMainCandidate(String relativePath) {
        String path = relativePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        int score = 0;

        if (!path.contains("/")) {
            score += 20;
        }

        if (path.equals("readme.md") || path.endsWith("/readme.md")) score += 1000;
        else if (path.equals("pom.xml") || path.endsWith("/pom.xml")) score += 950;
        else if (path.equals("package.json") || path.endsWith("/package.json")) score += 900;
        else if (path.equals("build.gradle") || path.endsWith("/build.gradle")) score += 850;
        else if (path.equals("settings.gradle") || path.endsWith("/settings.gradle")) score += 840;
        else if (path.equals("index.html") || path.endsWith("/index.html")) score += 800;
        else if (path.equals("app.vue") || path.endsWith("/app.vue")) score += 780;
        else if (path.equals("main.ts") || path.endsWith("/main.ts")) score += 760;
        else if (path.equals("main.js") || path.endsWith("/main.js")) score += 750;
        else if (path.endsWith("application.java")) score += 740;

        if (path.contains("/src/")) score += 40;
        if (path.contains("/docs/")) score -= 100;

        return score;
    }

    private void rollbackCreatedFiles(List<String> createdPaths) {
        for (String path : createdPaths) {
            try {
                fileStorageService.delete(path);
            } catch (Exception ignored) {
            }
        }
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
        String normalized = ProjectFileTypeSupport.resolve(null, fallbackFilename);
        if (!"bin".equals(normalized)) {
            return normalized;
        }
        return StringUtils.hasText(extension) ? extension.trim().toLowerCase(Locale.ROOT) : normalized;
    }

    private byte[] buildZipBytes(List<ProjectFile> files) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos)) {
            Set<String> usedNames = new HashSet<>();
            int writtenCount = 0;
            for (ProjectFile file : files) {
                if (isFolderRecord(file)) {
                    continue;
                }
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

    private boolean isFolderRecord(ProjectFile projectFile) {
        return projectFile != null
                && "folder".equalsIgnoreCase(String.valueOf(projectFile.getFileType()));
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
        List<ProjectFileVersionVO> versions = isFolderRecord(projectFile)
                ? List.of()
                : projectFileVersionRepository.findByFileIdOrderByUploadedAtDesc(projectFile.getId())
                .stream()
                .map(ProjectVoMapper::toProjectFileVersionVO)
                .toList();
        return ProjectVoMapper.toProjectFileVO(projectFile, versions);
    }

    private record ZipLayout(String rootPrefix) {
    }

    private record TempExtractedFile(Path path, long size) {
        void deleteQuietly() {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
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

    private static class TempFileMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final Path path;

        private TempFileMultipartFile(String name, String originalFilename, String contentType, Path path) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.path = path;
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
            try {
                return Files.size(path) <= 0;
            } catch (IOException e) {
                return true;
            }
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException e) {
                return 0L;
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

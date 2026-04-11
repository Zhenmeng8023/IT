package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.*;
import com.alikeyou.itmoduleproject.repository.*;
import com.alikeyou.itmoduleproject.service.ProjectWorkspaceService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPathUtils;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepoStorageSupport;
import com.alikeyou.itmoduleproject.vo.ProjectCommitVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ProjectWorkspaceServiceImpl implements ProjectWorkspaceService {

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

    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectWorkspaceRepository projectWorkspaceRepository;
    private final ProjectWorkspaceItemRepository projectWorkspaceItemRepository;
    private final ProjectBlobRepository projectBlobRepository;
    private final ProjectRepoStorageSupport projectRepoStorageSupport;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectCommitParentRepository projectCommitParentRepository;
    private final ProjectSnapshotRepository projectSnapshotRepository;
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository;
    private final ProjectCommitChangeRepository projectCommitChangeRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectPermissionService projectPermissionService;

    public ProjectWorkspaceServiceImpl(ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                       ProjectBranchRepository projectBranchRepository,
                                       ProjectWorkspaceRepository projectWorkspaceRepository,
                                       ProjectWorkspaceItemRepository projectWorkspaceItemRepository,
                                       ProjectBlobRepository projectBlobRepository,
                                       ProjectRepoStorageSupport projectRepoStorageSupport,
                                       ProjectCommitRepository projectCommitRepository,
                                       ProjectCommitParentRepository projectCommitParentRepository,
                                       ProjectSnapshotRepository projectSnapshotRepository,
                                       ProjectSnapshotItemRepository projectSnapshotItemRepository,
                                       ProjectCommitChangeRepository projectCommitChangeRepository,
                                       ProjectFileRepository projectFileRepository,
                                       ProjectFileVersionRepository projectFileVersionRepository,
                                       ProjectPermissionService projectPermissionService) {
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectBranchRepository = projectBranchRepository;
        this.projectWorkspaceRepository = projectWorkspaceRepository;
        this.projectWorkspaceItemRepository = projectWorkspaceItemRepository;
        this.projectBlobRepository = projectBlobRepository;
        this.projectRepoStorageSupport = projectRepoStorageSupport;
        this.projectCommitRepository = projectCommitRepository;
        this.projectCommitParentRepository = projectCommitParentRepository;
        this.projectSnapshotRepository = projectSnapshotRepository;
        this.projectSnapshotItemRepository = projectSnapshotItemRepository;
        this.projectCommitChangeRepository = projectCommitChangeRepository;
        this.projectFileRepository = projectFileRepository;
        this.projectFileVersionRepository = projectFileVersionRepository;
        this.projectPermissionService = projectPermissionService;
    }

    @Override
    public ProjectWorkspaceVO getCurrentWorkspace(Long projectId, Long branchId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        ProjectCodeRepository repo = requireRepo(projectId);
        ProjectBranch branch = requireBranch(repo.getId(), branchId);
        ProjectWorkspace workspace = getOrCreateWorkspace(repo, branch, currentUserId);
        return toVO(workspace);
    }

    @Override
    @Transactional
    public ProjectWorkspaceItemVO stageFile(Long projectId, Long branchId, Long currentUserId, String canonicalPath, MultipartFile file) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        ProjectCodeRepository repo = requireRepo(projectId);
        ProjectBranch branch = requireBranch(repo.getId(), branchId);
        ProjectWorkspace workspace = getOrCreateWorkspace(repo, branch, currentUserId);
        return stageFileInternal(workspace, branch, ProjectPathUtils.normalize(canonicalPath), file, null);
    }

    @Override
    @Transactional
    public List<ProjectWorkspaceItemVO> stageFiles(Long projectId, Long branchId, Long currentUserId, String targetDir, List<MultipartFile> files) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        if (files == null || files.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        ProjectCodeRepository repo = requireRepo(projectId);
        ProjectBranch branch = requireBranch(repo.getId(), branchId);
        ProjectWorkspace workspace = getOrCreateWorkspace(repo, branch, currentUserId);

        String normalizedTargetDir = normalizeTargetDir(targetDir);
        List<ProjectWorkspaceItemVO> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String canonicalPath = buildCanonicalPath(normalizedTargetDir, file.getOriginalFilename());
            results.add(stageFileInternal(workspace, branch, canonicalPath, file, null));
        }
        if (results.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        return results;
    }

    @Override
    @Transactional
    public List<ProjectWorkspaceItemVO> stageZip(Long projectId, Long branchId, Long currentUserId, MultipartFile file) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传 ZIP 不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(extension) || !"zip".equalsIgnoreCase(extension.trim())) {
            throw new BusinessException("只能上传 zip 压缩包");
        }

        ProjectCodeRepository repo = requireRepo(projectId);
        ProjectBranch branch = requireBranch(repo.getId(), branchId);
        ProjectWorkspace workspace = getOrCreateWorkspace(repo, branch, currentUserId);
        ZipLayout layout = detectZipLayout(file, originalFilename);

        List<ProjectWorkspaceItemVO> results = new ArrayList<>();
        Set<String> seenPaths = new LinkedHashSet<>();
        long totalBytes = 0L;
        int validCount = 0;

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String relativePath = normalizeZipEntryPath(entry.getName(), layout.rootPrefix());
                if (!StringUtils.hasText(relativePath)) {
                    zis.closeEntry();
                    continue;
                }
                if (relativePath.length() > 255) {
                    throw new BusinessException("ZIP 内文件路径过长：" + relativePath);
                }
                if (shouldIgnoreZipEntry(relativePath) || !seenPaths.add(relativePath)) {
                    zis.closeEntry();
                    continue;
                }

                validCount++;
                if (validCount > MAX_ZIP_ENTRY_COUNT) {
                    throw new BusinessException("ZIP 中文件过多，请精简后再上传");
                }

                TempExtractedFile tempFile = streamZipEntryToTempFile(zis, relativePath);
                try {
                    totalBytes += tempFile.size();
                    if (totalBytes > MAX_TOTAL_UNZIP_BYTES) {
                        throw new BusinessException("ZIP 解压后的总大小过大，请拆分后再上传");
                    }
                    String fileName = StringUtils.getFilename(relativePath);
                    if (!StringUtils.hasText(fileName)) {
                        fileName = relativePath.replace('/', '_');
                    }
                    MultipartFile entryFile = new TempFileMultipartFile(
                            fileName,
                            fileName,
                            "application/octet-stream",
                            tempFile.path()
                    );
                    results.add(stageFileInternal(
                            workspace,
                            branch,
                            ProjectPathUtils.normalize("/" + relativePath),
                            entryFile,
                            "ZIP 导入：" + relativePath
                    ));
                } finally {
                    tempFile.deleteQuietly();
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new BusinessException("读取 ZIP 失败：" + e.getMessage());
        }

        if (results.isEmpty()) {
            throw new BusinessException("ZIP 中没有可暂存的项目文件");
        }
        return results;
    }

    @Override
    @Transactional
    public ProjectWorkspaceItemVO stageDelete(Long projectId, Long branchId, Long currentUserId, String canonicalPath) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        ProjectCodeRepository repo = requireRepo(projectId);
        ProjectBranch branch = requireBranch(repo.getId(), branchId);
        if (!existsInHead(branch, ProjectPathUtils.normalize(canonicalPath))) {
            throw new BusinessException("当前主线上不存在该路径，无法删除");
        }
        ProjectWorkspace workspace = getOrCreateWorkspace(repo, branch, currentUserId);
        ProjectWorkspaceItem item = projectWorkspaceItemRepository.save(ProjectWorkspaceItem.builder()
                .workspaceId(workspace.getId())
                .canonicalPath(ProjectPathUtils.normalize(canonicalPath))
                .changeType("DELETE")
                .stagedFlag(true)
                .conflictFlag(false)
                .detectedMessage("删除文件")
                .build());
        return toItemVO(item);
    }

    @Override
    public List<ProjectWorkspaceItemVO> listItems(Long projectId, Long branchId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        ProjectWorkspace workspace = getCurrentWorkspaceEntity(projectId, branchId, currentUserId);
        return projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(workspace.getId())
                .stream().map(this::toItemVO).toList();
    }

    @Override
    @Transactional
    public ProjectCommitVO commit(Long projectId, Long branchId, Long currentUserId, String message) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        if (message == null || message.isBlank()) {
            throw new BusinessException("提交说明不能为空");
        }
        ProjectCodeRepository repo = requireRepo(projectId);
        ProjectBranch branch = requireBranch(repo.getId(), branchId);
        if (Boolean.TRUE.equals(branch.getProtectedFlag()) && !Boolean.TRUE.equals(branch.getAllowDirectCommitFlag())) {
            throw new BusinessException("受保护分支不允许直接提交，请切换到可提交分支后提交并通过合并请求合入");
        }
        ProjectWorkspace workspace = getCurrentWorkspaceEntity(projectId, branchId, currentUserId);
        List<ProjectWorkspaceItem> items = projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(workspace.getId());
        if (items.isEmpty()) {
            throw new BusinessException("工作区没有可提交内容");
        }

        ProjectCommit lastCommit = branch.getHeadCommitId() == null ? null :
                projectCommitRepository.findById(branch.getHeadCommitId()).orElse(null);
        Long nextNo = projectCommitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(repo.getId(), branch.getId())
                .map(ProjectCommit::getCommitNo).orElse(0L) + 1L;
        ProjectCommit commit = projectCommitRepository.save(ProjectCommit.builder()
                .repositoryId(repo.getId())
                .branchId(branch.getId())
                .commitNo(nextNo)
                .displaySha(UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .message(message)
                .commitType("normal")
                .operatorId(currentUserId)
                .baseCommitId(branch.getHeadCommitId())
                .build());

        if (branch.getHeadCommitId() != null) {
            projectCommitParentRepository.save(ProjectCommitParent.builder()
                    .commitId(commit.getId())
                    .parentCommitId(branch.getHeadCommitId())
                    .parentOrder(1)
                    .build());
        }

        Map<String, ProjectSnapshotItem> headMap = loadHeadSnapshotMap(branch);
        Map<String, ProjectSnapshotItem> working = new LinkedHashMap<>(headMap);

        for (ProjectWorkspaceItem item : items) {
            String path = ProjectPathUtils.normalize(item.getCanonicalPath());
            if ("DELETE".equalsIgnoreCase(item.getChangeType())) {
                ProjectSnapshotItem oldItem = working.remove(path);
                if (oldItem != null) {
                    ProjectFile file = projectFileRepository.findById(oldItem.getProjectFileId()).orElse(null);
                    if (file != null) {
                        file.setDeletedFlag(true);
                        file.setLatestCommitId(commit.getId());
                        file.setLastModifiedAt(LocalDateTime.now());
                        projectFileRepository.save(file);
                        ProjectFileVersion parentVersion = projectFileVersionRepository.findById(oldItem.getProjectFileVersionId()).orElse(null);
                        int versionSeq = (int) projectFileVersionRepository.countByFileId(file.getId()) + 1;
                        projectFileVersionRepository.save(ProjectFileVersion.builder()
                                .fileId(file.getId())
                                .repositoryId(repo.getId())
                                .commitId(commit.getId())
                                .blobId(oldItem.getBlobId())
                                .version("v" + versionSeq)
                                .versionSeq(versionSeq)
                                .contentHash(oldItem.getContentHash())
                                .pathAtVersion(path)
                                .changeType("DELETE")
                                .parentVersionId(parentVersion == null ? null : parentVersion.getId())
                                .serverPath(file.getFilePath())
                                .fileSizeBytes(file.getFileSizeBytes())
                                .uploadedBy(currentUserId)
                                .commitMessage(message)
                                .build());
                    }
                    projectCommitChangeRepository.save(ProjectCommitChange.builder()
                            .commitId(commit.getId())
                            .projectFileId(oldItem.getProjectFileId())
                            .oldBlobId(oldItem.getBlobId())
                            .newBlobId(null)
                            .oldPath(path)
                            .newPath(null)
                            .changeType("DELETE")
                            .diffSummaryJson("{\"path\":\"" + path + "\"}")
                            .build());
                }
                continue;
            }

            ProjectBlob blob = projectBlobRepository.findById(item.getBlobId())
                    .orElseThrow(() -> new BusinessException("工作区内容对象不存在"));
            ProjectFile projectFile = projectFileRepository.findByProjectIdAndCanonicalPathAndDeletedFlagFalse(projectId, path)
                    .orElseGet(() -> ProjectFile.builder()
                            .projectId(projectId)
                            .repositoryId(repo.getId())
                            .fileName(ProjectPathUtils.extractFileName(path))
                            .canonicalPath(path)
                            .fileKey(path)
                            .filePath(blob.getStoragePath())
                            .fileSizeBytes(blob.getSizeBytes())
                            .fileType(blob.getMimeType())
                            .isMain(false)
                            .isLatest(true)
                            .deletedFlag(false)
                            .build());

            ProjectSnapshotItem oldItem = headMap.get(path);
            Long oldBlobId = oldItem == null ? null : oldItem.getBlobId();

            if (projectFile.getId() == null) {
                projectFile = projectFileRepository.save(projectFile);
            }

            long versionCount = projectFileVersionRepository.countByFileId(projectFile.getId());
            ProjectFileVersion previous = projectFileVersionRepository.findTopByFileIdOrderByUploadedAtDesc(projectFile.getId()).orElse(null);
            int nextVersionSeq = (int) versionCount + 1;
            ProjectFileVersion fileVersion = projectFileVersionRepository.save(ProjectFileVersion.builder()
                    .fileId(projectFile.getId())
                    .repositoryId(repo.getId())
                    .commitId(commit.getId())
                    .blobId(blob.getId())
                    .version("v" + nextVersionSeq)
                    .versionSeq(nextVersionSeq)
                    .contentHash(blob.getSha256())
                    .pathAtVersion(path)
                    .changeType(item.getChangeType())
                    .parentVersionId(previous == null ? null : previous.getId())
                    .serverPath(blob.getStoragePath())
                    .fileSizeBytes(blob.getSizeBytes())
                    .uploadedBy(currentUserId)
                    .commitMessage(message)
                    .build());

            projectFile.setRepositoryId(repo.getId());
            projectFile.setFileName(ProjectPathUtils.extractFileName(path));
            projectFile.setCanonicalPath(path);
            projectFile.setFileKey(path);
            projectFile.setFilePath(blob.getStoragePath());
            projectFile.setFileSizeBytes(blob.getSizeBytes());
            projectFile.setFileType(blob.getMimeType());
            projectFile.setVersion(fileVersion.getVersion());
            projectFile.setLatestBlobId(blob.getId());
            projectFile.setLatestVersionId(fileVersion.getId());
            projectFile.setLatestCommitId(commit.getId());
            projectFile.setIsLatest(true);
            projectFile.setDeletedFlag(false);
            projectFile.setContentHash(blob.getSha256());
            projectFile.setLastModifiedAt(LocalDateTime.now());
            projectFileRepository.save(projectFile);

            ProjectSnapshotItem newSnapshotItem = ProjectSnapshotItem.builder()
                    .projectFileId(projectFile.getId())
                    .projectFileVersionId(fileVersion.getId())
                    .blobId(blob.getId())
                    .canonicalPath(path)
                    .contentHash(blob.getSha256())
                    .build();
            working.put(path, newSnapshotItem);

            projectCommitChangeRepository.save(ProjectCommitChange.builder()
                    .commitId(commit.getId())
                    .projectFileId(projectFile.getId())
                    .oldBlobId(oldBlobId)
                    .newBlobId(blob.getId())
                    .oldPath(path)
                    .newPath(path)
                    .changeType(item.getChangeType())
                    .diffSummaryJson("{\"path\":\"" + path + "\",\"changeType\":\"" + item.getChangeType() + "\"}")
                    .build());
        }

        ProjectSnapshot snapshot = projectSnapshotRepository.save(ProjectSnapshot.builder()
                .repositoryId(repo.getId())
                .commitId(commit.getId())
                .manifestHash(UUID.randomUUID().toString().replace("-", ""))
                .fileCount(working.size())
                .build());

        for (ProjectSnapshotItem item : working.values()) {
            item.setSnapshotId(snapshot.getId());
            projectSnapshotItemRepository.save(item);
        }

        commit.setSnapshotId(snapshot.getId());
        projectCommitRepository.save(commit);

        branch.setHeadCommitId(commit.getId());
        projectBranchRepository.save(branch);

        if (repo.getDefaultBranchId() != null && repo.getDefaultBranchId().equals(branch.getId())) {
            repo.setHeadCommitId(commit.getId());
            projectCodeRepositoryRepository.save(repo);
        }

        workspace.setStatus("committed");
        projectWorkspaceRepository.save(workspace);

        return ProjectCommitVO.builder()
                .id(commit.getId())
                .repositoryId(commit.getRepositoryId())
                .branchId(commit.getBranchId())
                .commitNo(commit.getCommitNo())
                .displaySha(commit.getDisplaySha())
                .message(commit.getMessage())
                .commitType(commit.getCommitType())
                .snapshotId(commit.getSnapshotId())
                .operatorId(commit.getOperatorId())
                .baseCommitId(commit.getBaseCommitId())
                .isMergeCommit(commit.getIsMergeCommit())
                .isRevertCommit(commit.getIsRevertCommit())
                .changedFileCount(items.size())
                .createdAt(commit.getCreatedAt())
                .build();
    }

    private ProjectWorkspaceItemVO stageFileInternal(ProjectWorkspace workspace,
                                                     ProjectBranch branch,
                                                     String canonicalPath,
                                                     MultipartFile file,
                                                     String detectedMessage) {
        ProjectBlob blob = projectRepoStorageSupport.saveMultipart(file);
        String changeType = existsInHead(branch, canonicalPath) ? "MODIFY" : "ADD";
        ProjectWorkspaceItem item = projectWorkspaceItemRepository.save(ProjectWorkspaceItem.builder()
                .workspaceId(workspace.getId())
                .canonicalPath(canonicalPath)
                .tempStoragePath(blob.getStoragePath())
                .blobId(blob.getId())
                .changeType(changeType)
                .stagedFlag(true)
                .conflictFlag(false)
                .detectedMessage(StringUtils.hasText(detectedMessage)
                        ? detectedMessage
                        : (changeType.equals("ADD") ? "新增文件" : "覆盖更新文件"))
                .build());
        return toItemVO(item);
    }

    private ProjectCodeRepository requireRepo(Long projectId) {
        return projectCodeRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("项目仓库不存在，请先初始化仓库"));
    }

    private ProjectBranch requireBranch(Long repoId, Long branchId) {
        ProjectBranch branch = projectBranchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("分支不存在"));
        if (!repoId.equals(branch.getRepositoryId())) {
            throw new BusinessException("分支不属于当前项目仓库");
        }
        return branch;
    }

    private boolean existsInHead(ProjectBranch branch, String canonicalPath) {
        if (branch.getHeadCommitId() == null) return false;
        ProjectCommit head = projectCommitRepository.findById(branch.getHeadCommitId()).orElse(null);
        if (head == null || head.getSnapshotId() == null) return false;
        return projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(head.getSnapshotId())
                .stream().anyMatch(item -> canonicalPath.equals(item.getCanonicalPath()));
    }

    private Map<String, ProjectSnapshotItem> loadHeadSnapshotMap(ProjectBranch branch) {
        if (branch.getHeadCommitId() == null) return new LinkedHashMap<>();
        ProjectCommit head = projectCommitRepository.findById(branch.getHeadCommitId()).orElse(null);
        if (head == null || head.getSnapshotId() == null) return new LinkedHashMap<>();
        Map<String, ProjectSnapshotItem> map = new LinkedHashMap<>();
        for (ProjectSnapshotItem item : projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(head.getSnapshotId())) {
            map.put(item.getCanonicalPath(), ProjectSnapshotItem.builder()
                    .projectFileId(item.getProjectFileId())
                    .projectFileVersionId(item.getProjectFileVersionId())
                    .blobId(item.getBlobId())
                    .canonicalPath(item.getCanonicalPath())
                    .contentHash(item.getContentHash())
                    .build());
        }
        return map;
    }

    private ProjectWorkspace getCurrentWorkspaceEntity(Long projectId, Long branchId, Long currentUserId) {
        ProjectCodeRepository repo = requireRepo(projectId);
        ProjectBranch branch = requireBranch(repo.getId(), branchId);
        return getOrCreateWorkspace(repo, branch, currentUserId);
    }

    private ProjectWorkspace getOrCreateWorkspace(ProjectCodeRepository repo, ProjectBranch branch, Long currentUserId) {
        return projectWorkspaceRepository.findFirstByRepositoryIdAndBranchIdAndOwnerIdAndStatusOrderByUpdatedAtDesc(
                repo.getId(), branch.getId(), currentUserId, "active"
        ).orElseGet(() -> projectWorkspaceRepository.save(ProjectWorkspace.builder()
                .repositoryId(repo.getId())
                .branchId(branch.getId())
                .ownerId(currentUserId)
                .baseCommitId(branch.getHeadCommitId())
                .status("active")
                .build()));
    }

    private ProjectWorkspaceVO toVO(ProjectWorkspace workspace) {
        return ProjectWorkspaceVO.builder()
                .id(workspace.getId())
                .repositoryId(workspace.getRepositoryId())
                .branchId(workspace.getBranchId())
                .ownerId(workspace.getOwnerId())
                .baseCommitId(workspace.getBaseCommitId())
                .status(workspace.getStatus())
                .items(projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(workspace.getId()).stream().map(this::toItemVO).toList())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }

    private ProjectWorkspaceItemVO toItemVO(ProjectWorkspaceItem item) {
        return ProjectWorkspaceItemVO.builder()
                .id(item.getId())
                .workspaceId(item.getWorkspaceId())
                .canonicalPath(item.getCanonicalPath())
                .blobId(item.getBlobId())
                .changeType(item.getChangeType())
                .stagedFlag(item.getStagedFlag())
                .conflictFlag(item.getConflictFlag())
                .detectedMessage(item.getDetectedMessage())
                .build();
    }

    private String normalizeTargetDir(String targetDir) {
        if (!StringUtils.hasText(targetDir)) {
            return "";
        }
        String normalized = ProjectPathUtils.normalize(targetDir);
        return "/".equals(normalized) ? "" : normalized;
    }

    private String buildCanonicalPath(String targetDir, String originalFilename) {
        String fileName = StringUtils.hasText(originalFilename)
                ? originalFilename.replace('\\', '/').trim()
                : "unnamed-file";
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        }
        return ProjectPathUtils.normalize((StringUtils.hasText(targetDir) ? targetDir : "") + "/" + fileName);
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

    private TempExtractedFile streamZipEntryToTempFile(ZipInputStream zis, String relativePath) throws IOException {
        String ext = StringUtils.getFilenameExtension(relativePath);
        String suffix = StringUtils.hasText(ext) ? "." + ext : ".tmp";
        Path tempPath = Files.createTempFile("project-workspace-zip-", suffix);

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

package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectCommitChange;
import com.alikeyou.itmoduleproject.entity.ProjectCommitParent;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshot;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.repository.ProjectBlobRepository;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitChangeRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitParentRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotRepository;
import com.alikeyou.itmoduleproject.service.ProjectCommitService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectFileTypeSupport;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import com.alikeyou.itmoduleproject.support.ProjectSnapshotDiffSupport;
import com.alikeyou.itmoduleproject.support.diff.ChangeEntry;
import com.alikeyou.itmoduleproject.vo.ProjectCommitVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class ProjectCommitServiceImpl implements ProjectCommitService {

    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectCommitParentRepository projectCommitParentRepository;
    private final ProjectCommitChangeRepository projectCommitChangeRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectSnapshotRepository projectSnapshotRepository;
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectCommitChangeRepository commitChangeRepository;
    private final ProjectBlobRepository projectBlobRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport;

    public ProjectCommitServiceImpl(ProjectCommitRepository projectCommitRepository,
                                    ProjectCommitParentRepository projectCommitParentRepository,
                                    ProjectCommitChangeRepository projectCommitChangeRepository,
                                    ProjectBranchRepository projectBranchRepository,
                                    ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                    ProjectSnapshotRepository projectSnapshotRepository,
                                    ProjectSnapshotItemRepository projectSnapshotItemRepository,
                                    ProjectFileRepository projectFileRepository,
                                    ProjectFileVersionRepository projectFileVersionRepository,
                                    ProjectCommitChangeRepository commitChangeRepository,
                                    ProjectBlobRepository projectBlobRepository,
                                    ProjectPermissionService projectPermissionService,
                                    ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport) {
        this.projectCommitRepository = projectCommitRepository;
        this.projectCommitParentRepository = projectCommitParentRepository;
        this.projectCommitChangeRepository = projectCommitChangeRepository;
        this.projectBranchRepository = projectBranchRepository;
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectSnapshotRepository = projectSnapshotRepository;
        this.projectSnapshotItemRepository = projectSnapshotItemRepository;
        this.projectFileRepository = projectFileRepository;
        this.projectFileVersionRepository = projectFileVersionRepository;
        this.commitChangeRepository = commitChangeRepository;
        this.projectBlobRepository = projectBlobRepository;
        this.projectPermissionService = projectPermissionService;
        this.projectRepositoryBootstrapSupport = projectRepositoryBootstrapSupport;
    }

    @Override
    public List<ProjectCommitVO> listByBranch(Long projectId, Long branchId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        projectRepositoryBootstrapSupport.ensureRepositorySnapshotInitialized(repo, currentUserId);
        ProjectBranch branch = projectBranchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("分支不存在"));
        if (!repo.getId().equals(branch.getRepositoryId())) {
            throw new BusinessException("分支不属于当前项目仓库");
        }
        return projectCommitRepository.findByBranchIdOrderByCreatedAtDesc(branchId)
                .stream().map(this::toVO).toList();
    }

    @Override
    public Map<String, Object> detail(Long commitId, Long currentUserId) {
        ProjectCommit commit = requireCommit(commitId);
        ProjectCodeRepository repo = requireRepository(commit.getRepositoryId());
        projectRepositoryBootstrapSupport.ensureRepositorySnapshotInitialized(repo, currentUserId);
        projectPermissionService.assertProjectReadable(repo.getProjectId(), currentUserId);
        assertCommitBelongsToRepository(commit, repo);
        List<Long> parents = projectCommitParentRepository.findByCommitIdOrderByParentOrderAsc(commitId)
                .stream().map(ProjectCommitParent::getParentCommitId).toList();
        Long directParentCommitId = parents.isEmpty() ? null : parents.get(0);
        Map<String, ProjectSnapshotItem> fromMap = loadSnapshotMapByCommitId(directParentCommitId);
        Map<String, ProjectSnapshotItem> toMap = loadSnapshotMap(commit.getSnapshotId());
        Map<Long, Boolean> binaryCache = new HashMap<>();
        List<ChangeEntry> changes = ProjectSnapshotDiffSupport.buildChangeEntries(
                fromMap,
                toMap,
                directParentCommitId,
                commit.getId(),
                null,
                blobId -> resolveBinaryFlagByBlobId(binaryCache, blobId)
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("commit", toVO(commit));
        result.put("parents", parents);
        result.put("changes", changes);
        result.put("legacyChanges", projectCommitChangeRepository.findByCommitIdOrderByIdAsc(commitId));
        return result;
    }

    @Override
    public Map<String, Object> compare(Long fromCommitId, Long toCommitId, Long currentUserId) {
        ProjectCommit from = requireCommit(fromCommitId);
        ProjectCommit to = requireCommit(toCommitId);
        ProjectCodeRepository fromRepo = requireRepository(from.getRepositoryId());
        ProjectCodeRepository toRepo = requireRepository(to.getRepositoryId());
        projectRepositoryBootstrapSupport.ensureRepositorySnapshotInitialized(fromRepo, currentUserId);
        projectRepositoryBootstrapSupport.ensureRepositorySnapshotInitialized(toRepo, currentUserId);
        projectPermissionService.assertProjectReadable(fromRepo.getProjectId(), currentUserId);
        if (!Objects.equals(fromRepo.getProjectId(), toRepo.getProjectId())) {
            throw new BusinessException("禁止跨项目 compare");
        }
        if (!Objects.equals(from.getRepositoryId(), to.getRepositoryId())) {
            throw new BusinessException("禁止跨仓库 compare");
        }
        assertCommitBelongsToRepository(from, fromRepo);
        assertCommitBelongsToRepository(to, toRepo);
        Map<String, ProjectSnapshotItem> fromMap = loadSnapshotMap(from.getSnapshotId());
        Map<String, ProjectSnapshotItem> toMap = loadSnapshotMap(to.getSnapshotId());
        Map<Long, Boolean> binaryCache = new HashMap<>();
        Map<String, Object> result = new LinkedHashMap<>(ProjectSnapshotDiffSupport.buildComparePayload(
                fromMap,
                toMap,
                blobId -> resolveBinaryFlagByBlobId(binaryCache, blobId),
                fromCommitId,
                toCommitId,
                null
        ));
        result.put("fromCommitId", fromCommitId);
        result.put("toCommitId", toCommitId);
        return result;
    }

    @Override
    @Transactional
    public ProjectCommitVO rollbackToCommit(Long commitId, Long currentUserId) {
        ProjectCommit target = requireCommit(commitId);
        ProjectBranch branch = projectBranchRepository.findById(target.getBranchId())
                .orElseThrow(() -> new BusinessException("分支不存在"));
        ProjectCodeRepository repo = requireRepository(target.getRepositoryId());
        projectRepositoryBootstrapSupport.ensureRepositorySnapshotInitialized(repo, currentUserId);
        projectPermissionService.assertProjectManageMembers(repo.getProjectId(), currentUserId);
        assertCommitBelongsToRepository(target, repo);
        if (Boolean.TRUE.equals(branch.getProtectedFlag()) && !Boolean.TRUE.equals(branch.getAllowDirectCommitFlag())) {
            throw new BusinessException("受保护分支不允许直接回退，请通过受控分支处理");
        }

        Long previousHeadCommitId = branch.getHeadCommitId();
        Map<String, ProjectSnapshotItem> targetMap = loadSnapshotMap(target.getSnapshotId());
        Map<String, ProjectSnapshotItem> currentHeadMap = loadCurrentHeadSnapshotMap(branch);
        if (sameSnapshotMap(targetMap, currentHeadMap)) {
            throw new BusinessException("目标提交已与当前分支状态一致，无需回退");
        }

        Long nextNo = projectCommitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(repo.getId(), branch.getId())
                .map(ProjectCommit::getCommitNo).orElse(0L) + 1L;
        ProjectCommit rollback = projectCommitRepository.save(ProjectCommit.builder()
                .repositoryId(repo.getId())
                .branchId(branch.getId())
                .commitNo(nextNo)
                .displaySha(UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .message("rollback to commit " + target.getDisplaySha())
                .commitType("revert")
                .operatorId(currentUserId)
                .baseCommitId(target.getId())
                .isRevertCommit(true)
                .createdAt(LocalDateTime.now())
                .build());

        if (previousHeadCommitId != null) {
            projectCommitParentRepository.save(ProjectCommitParent.builder()
                    .commitId(rollback.getId())
                    .parentCommitId(previousHeadCommitId)
                    .parentOrder(1)
                    .build());
        }

        Map<String, ProjectSnapshotItem> rollbackSnapshot = cloneSnapshotMap(currentHeadMap);
        Set<String> paths = new TreeSet<>();
        paths.addAll(currentHeadMap.keySet());
        paths.addAll(targetMap.keySet());
        for (String path : paths) {
            ProjectSnapshotItem targetItem = targetMap.get(path);
            ProjectSnapshotItem headItem = currentHeadMap.get(path);
            if (sameSnapshotItem(targetItem, headItem)) {
                continue;
            }
            if (targetItem == null) {
                applyRollbackDeleteChange(rollback, currentUserId, rollbackSnapshot, path, headItem, target.getDisplaySha());
                continue;
            }
            applyRollbackRestoreChange(repo.getProjectId(), repo, rollback, currentUserId, rollbackSnapshot, path, headItem, targetItem, target.getDisplaySha());
        }

        ProjectSnapshot snapshot = projectSnapshotRepository.save(ProjectSnapshot.builder()
                .repositoryId(repo.getId())
                .commitId(rollback.getId())
                .manifestHash(UUID.randomUUID().toString().replace("-", ""))
                .fileCount(rollbackSnapshot.size())
                .build());

        List<String> snapshotPaths = new ArrayList<>(rollbackSnapshot.keySet());
        Collections.sort(snapshotPaths);
        for (String path : snapshotPaths) {
            ProjectSnapshotItem sourceItem = rollbackSnapshot.get(path);
            projectSnapshotItemRepository.save(ProjectSnapshotItem.builder()
                    .snapshotId(snapshot.getId())
                    .projectFileId(sourceItem.getProjectFileId())
                    .projectFileVersionId(sourceItem.getProjectFileVersionId())
                    .blobId(sourceItem.getBlobId())
                    .canonicalPath(sourceItem.getCanonicalPath())
                    .contentHash(sourceItem.getContentHash())
                    .build());
        }

        rollback.setSnapshotId(snapshot.getId());
        projectCommitRepository.save(rollback);
        branch.setHeadCommitId(rollback.getId());
        projectBranchRepository.save(branch);
        if (repo.getDefaultBranchId() != null && repo.getDefaultBranchId().equals(branch.getId())) {
            repo.setHeadCommitId(rollback.getId());
            projectCodeRepositoryRepository.save(repo);
        }
        return toVO(rollback);
    }

    private ProjectCommit requireCommit(Long commitId) {
        return projectCommitRepository.findById(commitId)
                .orElseThrow(() -> new BusinessException("提交不存在"));
    }

    private ProjectCodeRepository requireRepository(Long repositoryId) {
        return projectCodeRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
    }

    private void assertCommitBelongsToRepository(ProjectCommit commit, ProjectCodeRepository repo) {
        if (!Objects.equals(commit.getRepositoryId(), repo.getId())) {
            throw new BusinessException("提交不属于当前项目仓库");
        }
        ProjectBranch branch = projectBranchRepository.findById(commit.getBranchId())
                .orElseThrow(() -> new BusinessException("分支不存在"));
        if (!Objects.equals(branch.getRepositoryId(), repo.getId())) {
            throw new BusinessException("提交关联分支不属于当前项目仓库");
        }
    }

    private Map<String, ProjectSnapshotItem> loadSnapshotMap(Long snapshotId) {
        if (snapshotId == null) {
            return new LinkedHashMap<>();
        }
        Map<String, ProjectSnapshotItem> map = new LinkedHashMap<>();
        for (ProjectSnapshotItem item : projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(snapshotId)) {
            map.put(item.getCanonicalPath(), item);
        }
        return map;
    }

    private Map<String, ProjectSnapshotItem> loadSnapshotMapByCommitId(Long commitId) {
        if (commitId == null) {
            return new LinkedHashMap<>();
        }
        ProjectCommit commit = projectCommitRepository.findById(commitId).orElse(null);
        if (commit == null || commit.getSnapshotId() == null) {
            return new LinkedHashMap<>();
        }
        return loadSnapshotMap(commit.getSnapshotId());
    }

    private Map<String, ProjectSnapshotItem> loadCurrentHeadSnapshotMap(ProjectBranch branch) {
        if (branch.getHeadCommitId() == null) {
            return new LinkedHashMap<>();
        }
        ProjectCommit head = projectCommitRepository.findById(branch.getHeadCommitId()).orElse(null);
        if (head == null) {
            return new LinkedHashMap<>();
        }
        return loadSnapshotMap(head.getSnapshotId());
    }

    private void applyRollbackDeleteChange(ProjectCommit rollbackCommit,
                                           Long currentUserId,
                                           Map<String, ProjectSnapshotItem> rollbackSnapshot,
                                           String path,
                                           ProjectSnapshotItem headItem,
                                           String targetDisplaySha) {
        if (headItem == null) {
            rollbackSnapshot.remove(path);
            return;
        }
        rollbackSnapshot.remove(path);
        ProjectFile file = projectFileRepository.findById(headItem.getProjectFileId()).orElse(null);
        if (file != null) {
            Long parentVersionId = headItem.getProjectFileVersionId() != null
                    ? headItem.getProjectFileVersionId()
                    : file.getLatestVersionId();
            int nextVersionSeq = (int) projectFileVersionRepository.countByFileId(file.getId()) + 1;
            ProjectFileVersion deleteVersion = projectFileVersionRepository.save(ProjectFileVersion.builder()
                    .fileId(file.getId())
                    .repositoryId(file.getRepositoryId())
                    .commitId(rollbackCommit.getId())
                    .blobId(headItem.getBlobId())
                    .version("v" + nextVersionSeq)
                    .versionSeq(nextVersionSeq)
                    .contentHash(headItem.getContentHash())
                    .pathAtVersion(path)
                    .changeType("DELETE")
                    .parentVersionId(parentVersionId)
                    .serverPath(file.getFilePath())
                    .fileSizeBytes(file.getFileSizeBytes())
                    .uploadedBy(currentUserId)
                    .commitMessage(rollbackCommit.getMessage())
                    .build());
            file.setDeletedFlag(true);
            file.setLatestCommitId(rollbackCommit.getId());
            file.setLatestVersionId(deleteVersion.getId());
            file.setLatestBlobId(null);
            file.setContentHash(headItem.getContentHash());
            file.setLastModifiedAt(LocalDateTime.now());
            projectFileRepository.save(file);
        }
        commitChangeRepository.save(ProjectCommitChange.builder()
                .commitId(rollbackCommit.getId())
                .projectFileId(headItem.getProjectFileId())
                .oldBlobId(headItem.getBlobId())
                .newBlobId(null)
                .oldPath(path)
                .newPath(null)
                .changeType("DELETE")
                .diffSummaryJson("{\"path\":\"" + path + "\",\"rollback\":true,\"targetCommit\":\"" + targetDisplaySha + "\"}")
                .build());
    }

    private void applyRollbackRestoreChange(Long projectId,
                                            ProjectCodeRepository repo,
                                            ProjectCommit rollbackCommit,
                                            Long currentUserId,
                                            Map<String, ProjectSnapshotItem> rollbackSnapshot,
                                            String path,
                                            ProjectSnapshotItem headItem,
                                            ProjectSnapshotItem targetItem,
                                            String targetDisplaySha) {
        ProjectBlob blob = requireBlob(targetItem.getBlobId());
        ProjectFile file = resolveRollbackProjectFile(projectId, headItem, targetItem, path, repo.getId(), blob);
        Long parentVersionId = headItem != null && headItem.getProjectFileVersionId() != null
                ? headItem.getProjectFileVersionId()
                : file.getLatestVersionId();
        String changeType = headItem == null ? "ADD" : "MODIFY";
        int nextVersionSeq = (int) projectFileVersionRepository.countByFileId(file.getId()) + 1;
        ProjectFileVersion fileVersion = projectFileVersionRepository.save(ProjectFileVersion.builder()
                .fileId(file.getId())
                .repositoryId(repo.getId())
                .commitId(rollbackCommit.getId())
                .blobId(blob.getId())
                .version("v" + nextVersionSeq)
                .versionSeq(nextVersionSeq)
                .contentHash(blob.getSha256())
                .pathAtVersion(path)
                .changeType(changeType)
                .parentVersionId(parentVersionId)
                .serverPath(blob.getStoragePath())
                .fileSizeBytes(blob.getSizeBytes())
                .uploadedBy(currentUserId)
                .commitMessage(rollbackCommit.getMessage())
                .build());

        file.setRepositoryId(repo.getId());
        file.setFileName(extractFileName(path));
        file.setCanonicalPath(path);
        file.setFileKey(path);
        file.setFilePath(blob.getStoragePath());
        file.setFileSizeBytes(blob.getSizeBytes());
        file.setFileType(ProjectFileTypeSupport.resolve(path, file.getFileName()));
        file.setVersion(fileVersion.getVersion());
        file.setLatestBlobId(blob.getId());
        file.setLatestVersionId(fileVersion.getId());
        file.setLatestCommitId(rollbackCommit.getId());
        file.setIsLatest(true);
        file.setDeletedFlag(false);
        file.setContentHash(blob.getSha256());
        file.setLastModifiedAt(LocalDateTime.now());
        projectFileRepository.save(file);

        rollbackSnapshot.put(path, ProjectSnapshotItem.builder()
                .projectFileId(file.getId())
                .projectFileVersionId(fileVersion.getId())
                .blobId(blob.getId())
                .canonicalPath(path)
                .contentHash(blob.getSha256())
                .build());

        commitChangeRepository.save(ProjectCommitChange.builder()
                .commitId(rollbackCommit.getId())
                .projectFileId(file.getId())
                .oldBlobId(headItem == null ? null : headItem.getBlobId())
                .newBlobId(blob.getId())
                .oldPath(path)
                .newPath(path)
                .changeType(changeType)
                .diffSummaryJson("{\"path\":\"" + path + "\",\"changeType\":\"" + changeType + "\",\"rollback\":true,\"targetCommit\":\"" + targetDisplaySha + "\"}")
                .build());
    }

    private ProjectFile resolveRollbackProjectFile(Long projectId,
                                                   ProjectSnapshotItem headItem,
                                                   ProjectSnapshotItem targetItem,
                                                   String path,
                                                   Long repositoryId,
                                                   ProjectBlob blob) {
        ProjectFile file = null;
        if (headItem != null) {
            file = projectFileRepository.findById(headItem.getProjectFileId()).orElse(null);
        }
        if (file == null && targetItem != null) {
            file = projectFileRepository.findById(targetItem.getProjectFileId()).orElse(null);
        }
        if (file == null) {
            file = projectFileRepository.findByProjectIdAndCanonicalPathAndDeletedFlagFalse(projectId, path).orElse(null);
        }
        if (file == null) {
            file = ProjectFile.builder()
                    .projectId(projectId)
                    .repositoryId(repositoryId)
                    .fileName(extractFileName(path))
                    .canonicalPath(path)
                    .fileKey(path)
                    .filePath(blob.getStoragePath())
                    .fileSizeBytes(blob.getSizeBytes())
                    .fileType(ProjectFileTypeSupport.resolve(path, extractFileName(path)))
                    .isMain(false)
                    .isLatest(true)
                    .deletedFlag(false)
                    .build();
        }
        if (file.getId() == null) {
            file = projectFileRepository.save(file);
        }
        return file;
    }

    private ProjectBlob requireBlob(Long blobId) {
        if (blobId == null) {
            throw new BusinessException("回退目标内容对象不存在");
        }
        return projectBlobRepository.findById(blobId)
                .orElseThrow(() -> new BusinessException("回退目标内容对象不存在"));
    }

    private boolean sameSnapshotMap(Map<String, ProjectSnapshotItem> left, Map<String, ProjectSnapshotItem> right) {
        Set<String> paths = new TreeSet<>();
        if (left != null) {
            paths.addAll(left.keySet());
        }
        if (right != null) {
            paths.addAll(right.keySet());
        }
        for (String path : paths) {
            if (!sameSnapshotItem(left == null ? null : left.get(path), right == null ? null : right.get(path))) {
                return false;
            }
        }
        return true;
    }

    private Map<String, ProjectSnapshotItem> cloneSnapshotMap(Map<String, ProjectSnapshotItem> source) {
        Map<String, ProjectSnapshotItem> cloned = new LinkedHashMap<>();
        if (source == null) {
            return cloned;
        }
        for (Map.Entry<String, ProjectSnapshotItem> entry : source.entrySet()) {
            cloned.put(entry.getKey(), cloneSnapshotItem(entry.getValue()));
        }
        return cloned;
    }

    private ProjectSnapshotItem cloneSnapshotItem(ProjectSnapshotItem item) {
        if (item == null) {
            return null;
        }
        return ProjectSnapshotItem.builder()
                .projectFileId(item.getProjectFileId())
                .projectFileVersionId(item.getProjectFileVersionId())
                .blobId(item.getBlobId())
                .canonicalPath(item.getCanonicalPath())
                .contentHash(item.getContentHash())
                .build();
    }

    private boolean sameSnapshotItem(ProjectSnapshotItem left, ProjectSnapshotItem right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getBlobId(), right.getBlobId())
                && Objects.equals(left.getCanonicalPath(), right.getCanonicalPath());
    }

    private String extractFileName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private Boolean resolveBinaryFlagByBlobId(Map<Long, Boolean> cache, Long blobId) {
        if (blobId == null) {
            return null;
        }
        return cache.computeIfAbsent(blobId, id -> projectBlobRepository.findById(id)
                .map(blob -> isBinaryMimeType(blob.getMimeType()))
                .orElse(Boolean.FALSE));
    }

    private boolean isBinaryMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return false;
        }
        String normalized = mimeType.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("text/")) {
            return false;
        }
        return !(normalized.contains("json")
                || normalized.contains("xml")
                || normalized.contains("javascript")
                || normalized.contains("ecmascript")
                || normalized.contains("yaml")
                || normalized.contains("yml")
                || normalized.contains("x-sh")
                || normalized.contains("sql"));
    }

    private ProjectCommitVO toVO(ProjectCommit commit) {
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
                .changedFileCount(projectCommitChangeRepository.findByCommitIdOrderByIdAsc(commit.getId()).size())
                .createdAt(commit.getCreatedAt())
                .build();
    }
}

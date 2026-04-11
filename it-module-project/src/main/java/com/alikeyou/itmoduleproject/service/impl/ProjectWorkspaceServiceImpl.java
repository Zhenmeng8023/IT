package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.*;
import com.alikeyou.itmoduleproject.repository.*;
import com.alikeyou.itmoduleproject.service.ProjectWorkspaceService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPathUtils;
import com.alikeyou.itmoduleproject.support.ProjectRepoStorageSupport;
import com.alikeyou.itmoduleproject.vo.ProjectCommitVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProjectWorkspaceServiceImpl implements ProjectWorkspaceService {

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
                                       ProjectFileVersionRepository projectFileVersionRepository) {
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
    }

    @Override
    public ProjectWorkspaceVO getCurrentWorkspace(Long projectId, Long branchId, Long currentUserId) {
        ProjectCodeRepository repo = requireRepo(projectId);
        ProjectBranch branch = requireBranch(repo.getId(), branchId);
        ProjectWorkspace workspace = getOrCreateWorkspace(repo, branch, currentUserId);
        return toVO(workspace);
    }

    @Override
    @Transactional
    public ProjectWorkspaceItemVO stageFile(Long projectId, Long branchId, Long currentUserId, String canonicalPath, MultipartFile file) {
        ProjectCodeRepository repo = requireRepo(projectId);
        ProjectBranch branch = requireBranch(repo.getId(), branchId);
        ProjectWorkspace workspace = getOrCreateWorkspace(repo, branch, currentUserId);
        String normalized = ProjectPathUtils.normalize(canonicalPath);
        ProjectBlob blob = projectRepoStorageSupport.saveMultipart(file);
        String changeType = existsInHead(branch, normalized) ? "MODIFY" : "ADD";
        ProjectWorkspaceItem item = projectWorkspaceItemRepository.save(ProjectWorkspaceItem.builder()
                .workspaceId(workspace.getId())
                .canonicalPath(normalized)
                .tempStoragePath(blob.getStoragePath())
                .blobId(blob.getId())
                .changeType(changeType)
                .stagedFlag(true)
                .conflictFlag(false)
                .detectedMessage(changeType.equals("ADD") ? "新增文件" : "覆盖更新文件")
                .build());
        return toItemVO(item);
    }

    @Override
    @Transactional
    public ProjectWorkspaceItemVO stageDelete(Long projectId, Long branchId, Long currentUserId, String canonicalPath) {
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
        ProjectWorkspace workspace = getCurrentWorkspaceEntity(projectId, branchId, currentUserId);
        return projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(workspace.getId())
                .stream().map(this::toItemVO).toList();
    }

    @Override
    @Transactional
    public ProjectCommitVO commit(Long projectId, Long branchId, Long currentUserId, String message) {
        if (message == null || message.isBlank()) {
            throw new BusinessException("提交说明不能为空");
        }
        ProjectCodeRepository repo = requireRepo(projectId);
        ProjectBranch branch = requireBranch(repo.getId(), branchId);
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
}

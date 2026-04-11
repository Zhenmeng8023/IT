package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.*;
import com.alikeyou.itmoduleproject.repository.*;
import com.alikeyou.itmoduleproject.service.ProjectCommitService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.vo.ProjectCommitVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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
    private final ProjectCommitChangeRepository commitChangeRepository;

    public ProjectCommitServiceImpl(ProjectCommitRepository projectCommitRepository,
                                    ProjectCommitParentRepository projectCommitParentRepository,
                                    ProjectCommitChangeRepository projectCommitChangeRepository,
                                    ProjectBranchRepository projectBranchRepository,
                                    ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                    ProjectSnapshotRepository projectSnapshotRepository,
                                    ProjectSnapshotItemRepository projectSnapshotItemRepository,
                                    ProjectFileRepository projectFileRepository,
                                    ProjectCommitChangeRepository commitChangeRepository) {
        this.projectCommitRepository = projectCommitRepository;
        this.projectCommitParentRepository = projectCommitParentRepository;
        this.projectCommitChangeRepository = projectCommitChangeRepository;
        this.projectBranchRepository = projectBranchRepository;
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectSnapshotRepository = projectSnapshotRepository;
        this.projectSnapshotItemRepository = projectSnapshotItemRepository;
        this.projectFileRepository = projectFileRepository;
        this.commitChangeRepository = commitChangeRepository;
    }

    @Override
    public List<ProjectCommitVO> listByBranch(Long projectId, Long branchId) {
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        ProjectBranch branch = projectBranchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("分支不存在"));
        if (!repo.getId().equals(branch.getRepositoryId())) {
            throw new BusinessException("分支不属于当前项目仓库");
        }
        return projectCommitRepository.findByBranchIdOrderByCreatedAtDesc(branchId)
                .stream().map(this::toVO).toList();
    }

    @Override
    public Map<String, Object> detail(Long commitId) {
        ProjectCommit commit = projectCommitRepository.findById(commitId)
                .orElseThrow(() -> new BusinessException("提交不存在"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("commit", toVO(commit));
        result.put("parents", projectCommitParentRepository.findByCommitIdOrderByParentOrderAsc(commitId)
                .stream().map(ProjectCommitParent::getParentCommitId).toList());
        result.put("changes", projectCommitChangeRepository.findByCommitIdOrderByIdAsc(commitId));
        return result;
    }

    @Override
    public Map<String, Object> compare(Long fromCommitId, Long toCommitId) {
        ProjectCommit from = projectCommitRepository.findById(fromCommitId)
                .orElseThrow(() -> new BusinessException("起始提交不存在"));
        ProjectCommit to = projectCommitRepository.findById(toCommitId)
                .orElseThrow(() -> new BusinessException("目标提交不存在"));
        Map<String, ProjectSnapshotItem> fromMap = loadSnapshotMap(from.getSnapshotId());
        Map<String, ProjectSnapshotItem> toMap = loadSnapshotMap(to.getSnapshotId());
        List<Map<String, Object>> files = new ArrayList<>();
        int add = 0, modify = 0, delete = 0;
        Set<String> paths = new TreeSet<>();
        paths.addAll(fromMap.keySet());
        paths.addAll(toMap.keySet());
        for (String path : paths) {
            ProjectSnapshotItem f = fromMap.get(path);
            ProjectSnapshotItem t = toMap.get(path);
            String type;
            if (f == null) {
                type = "ADD";
                add++;
            } else if (t == null) {
                type = "DELETE";
                delete++;
            } else if (Objects.equals(f.getBlobId(), t.getBlobId())) {
                continue;
            } else {
                type = "MODIFY";
                modify++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", path);
            row.put("changeType", type);
            row.put("fromBlobId", f == null ? null : f.getBlobId());
            row.put("toBlobId", t == null ? null : t.getBlobId());
            files.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fromCommitId", fromCommitId);
        result.put("toCommitId", toCommitId);
        result.put("addCount", add);
        result.put("modifyCount", modify);
        result.put("deleteCount", delete);
        result.put("files", files);
        return result;
    }

    @Override
    @Transactional
    public ProjectCommitVO rollbackToCommit(Long commitId, Long currentUserId) {
        ProjectCommit target = projectCommitRepository.findById(commitId)
                .orElseThrow(() -> new BusinessException("目标提交不存在"));
        ProjectBranch branch = projectBranchRepository.findById(target.getBranchId())
                .orElseThrow(() -> new BusinessException("分支不存在"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(target.getRepositoryId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));

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
                .baseCommitId(branch.getHeadCommitId())
                .isRevertCommit(true)
                .createdAt(LocalDateTime.now())
                .build());

        if (branch.getHeadCommitId() != null) {
            projectCommitParentRepository.save(ProjectCommitParent.builder()
                    .commitId(rollback.getId())
                    .parentCommitId(branch.getHeadCommitId())
                    .parentOrder(1)
                    .build());
        }

        Map<String, ProjectSnapshotItem> targetMap = loadSnapshotMap(target.getSnapshotId());
        ProjectSnapshot snapshot = projectSnapshotRepository.save(ProjectSnapshot.builder()
                .repositoryId(repo.getId())
                .commitId(rollback.getId())
                .manifestHash(UUID.randomUUID().toString().replace("-", ""))
                .fileCount(targetMap.size())
                .build());

        for (ProjectSnapshotItem sourceItem : targetMap.values()) {
            projectSnapshotItemRepository.save(ProjectSnapshotItem.builder()
                    .snapshotId(snapshot.getId())
                    .projectFileId(sourceItem.getProjectFileId())
                    .projectFileVersionId(sourceItem.getProjectFileVersionId())
                    .blobId(sourceItem.getBlobId())
                    .canonicalPath(sourceItem.getCanonicalPath())
                    .contentHash(sourceItem.getContentHash())
                    .build());
            ProjectFile file = projectFileRepository.findById(sourceItem.getProjectFileId()).orElse(null);
            if (file != null) {
                file.setDeletedFlag(false);
                file.setLatestCommitId(rollback.getId());
                file.setLatestBlobId(sourceItem.getBlobId());
                file.setLatestVersionId(sourceItem.getProjectFileVersionId());
                file.setContentHash(sourceItem.getContentHash());
                projectFileRepository.save(file);
            }
            commitChangeRepository.save(ProjectCommitChange.builder()
                    .commitId(rollback.getId())
                    .projectFileId(sourceItem.getProjectFileId())
                    .oldBlobId(null)
                    .newBlobId(sourceItem.getBlobId())
                    .oldPath(sourceItem.getCanonicalPath())
                    .newPath(sourceItem.getCanonicalPath())
                    .changeType("REVERT")
                    .diffSummaryJson("{\"path\":\"" + sourceItem.getCanonicalPath() + "\"}")
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

    private Map<String, ProjectSnapshotItem> loadSnapshotMap(Long snapshotId) {
        if (snapshotId == null) return new LinkedHashMap<>();
        Map<String, ProjectSnapshotItem> map = new LinkedHashMap<>();
        for (ProjectSnapshotItem item : projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(snapshotId)) {
            map.put(item.getCanonicalPath(), item);
        }
        return map;
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

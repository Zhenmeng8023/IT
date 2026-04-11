package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectMergeRequestCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectReviewSubmitRequest;
import com.alikeyou.itmoduleproject.entity.*;
import com.alikeyou.itmoduleproject.repository.*;
import com.alikeyou.itmoduleproject.service.ProjectMergeRequestService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.vo.ProjectMergeRequestVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProjectMergeRequestServiceImpl implements ProjectMergeRequestService {

    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectMergeRequestRepository projectMergeRequestRepository;
    private final ProjectReviewRepository projectReviewRepository;
    private final ProjectCheckRunRepository projectCheckRunRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectCommitParentRepository projectCommitParentRepository;
    private final ProjectSnapshotRepository projectSnapshotRepository;
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository;

    public ProjectMergeRequestServiceImpl(ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                          ProjectBranchRepository projectBranchRepository,
                                          ProjectMergeRequestRepository projectMergeRequestRepository,
                                          ProjectReviewRepository projectReviewRepository,
                                          ProjectCheckRunRepository projectCheckRunRepository,
                                          ProjectCommitRepository projectCommitRepository,
                                          ProjectCommitParentRepository projectCommitParentRepository,
                                          ProjectSnapshotRepository projectSnapshotRepository,
                                          ProjectSnapshotItemRepository projectSnapshotItemRepository) {
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectBranchRepository = projectBranchRepository;
        this.projectMergeRequestRepository = projectMergeRequestRepository;
        this.projectReviewRepository = projectReviewRepository;
        this.projectCheckRunRepository = projectCheckRunRepository;
        this.projectCommitRepository = projectCommitRepository;
        this.projectCommitParentRepository = projectCommitParentRepository;
        this.projectSnapshotRepository = projectSnapshotRepository;
        this.projectSnapshotItemRepository = projectSnapshotItemRepository;
    }

    @Override
    @Transactional
    public ProjectMergeRequestVO create(ProjectMergeRequestCreateRequest request, Long currentUserId) {
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        ProjectBranch source = projectBranchRepository.findById(request.getSourceBranchId())
                .orElseThrow(() -> new BusinessException("源分支不存在"));
        ProjectBranch target = projectBranchRepository.findById(request.getTargetBranchId())
                .orElseThrow(() -> new BusinessException("目标分支不存在"));
        if (!Objects.equals(source.getRepositoryId(), repo.getId()) || !Objects.equals(target.getRepositoryId(), repo.getId())) {
            throw new BusinessException("分支不属于当前项目仓库");
        }
        if (Objects.equals(source.getId(), target.getId())) {
            throw new BusinessException("源分支和目标分支不能相同");
        }
        ProjectMergeRequest mr = projectMergeRequestRepository.save(ProjectMergeRequest.builder()
                .repositoryId(repo.getId())
                .sourceBranchId(source.getId())
                .targetBranchId(target.getId())
                .sourceHeadCommitId(source.getHeadCommitId())
                .targetHeadCommitId(target.getHeadCommitId())
                .title(request.getTitle() == null ? "Merge " + source.getName() + " -> " + target.getName() : request.getTitle())
                .description(request.getDescription())
                .status("open")
                .createdBy(currentUserId)
                .build());
        return toVO(mr);
    }

    @Override
    public List<ProjectMergeRequestVO> list(Long projectId, String status) {
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        List<ProjectMergeRequest> list = (status == null || status.isBlank())
                ? projectMergeRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(repo.getId())
                : projectMergeRequestRepository.findByRepositoryIdAndStatusOrderByCreatedAtDesc(repo.getId(), status);
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public ProjectMergeRequestVO review(Long mergeRequestId, ProjectReviewSubmitRequest request, Long currentUserId) {
        ProjectMergeRequest mr = projectMergeRequestRepository.findById(mergeRequestId)
                .orElseThrow(() -> new BusinessException("合并请求不存在"));
        if (!"open".equalsIgnoreCase(mr.getStatus())) {
            throw new BusinessException("当前合并请求不是打开状态");
        }
        projectReviewRepository.save(ProjectReview.builder()
                .mergeRequestId(mr.getId())
                .reviewerId(currentUserId)
                .reviewResult(request.getReviewResult() == null ? "comment" : request.getReviewResult())
                .reviewComment(request.getReviewComment())
                .build());
        return toVO(mr);
    }

    @Override
    @Transactional
    public ProjectMergeRequestVO merge(Long mergeRequestId, Long currentUserId) {
        ProjectMergeRequest mr = projectMergeRequestRepository.findById(mergeRequestId)
                .orElseThrow(() -> new BusinessException("合并请求不存在"));
        if (!"open".equalsIgnoreCase(mr.getStatus())) {
            throw new BusinessException("合并请求不是可合并状态");
        }
        ProjectBranch source = projectBranchRepository.findById(mr.getSourceBranchId())
                .orElseThrow(() -> new BusinessException("源分支不存在"));
        ProjectBranch target = projectBranchRepository.findById(mr.getTargetBranchId())
                .orElseThrow(() -> new BusinessException("目标分支不存在"));

        if (Boolean.TRUE.equals(target.getProtectedFlag())) {
            boolean approved = projectReviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(mr.getId())
                    .stream().anyMatch(item -> "approve".equalsIgnoreCase(item.getReviewResult()));
            if (!approved) {
                throw new BusinessException("受保护分支合并前至少需要一个通过评审");
            }
            boolean hasFailedCheck = projectCheckRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(mr.getId())
                    .stream().anyMatch(item -> "failed".equalsIgnoreCase(item.getCheckStatus()));
            if (hasFailedCheck) {
                throw new BusinessException("当前合并请求存在失败检查，不能合并");
            }
        }

        ProjectCommit sourceHead = source.getHeadCommitId() == null ? null :
                projectCommitRepository.findById(source.getHeadCommitId()).orElse(null);
        if (sourceHead == null) {
            throw new BusinessException("源分支没有可合并提交");
        }

        Long nextNo = projectCommitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(target.getRepositoryId(), target.getId())
                .map(ProjectCommit::getCommitNo).orElse(0L) + 1L;
        ProjectCommit mergeCommit = projectCommitRepository.save(ProjectCommit.builder()
                .repositoryId(target.getRepositoryId())
                .branchId(target.getId())
                .commitNo(nextNo)
                .displaySha(UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .message("merge branch " + source.getName() + " into " + target.getName())
                .commitType("merge")
                .operatorId(currentUserId)
                .baseCommitId(target.getHeadCommitId())
                .isMergeCommit(true)
                .build());

        if (target.getHeadCommitId() != null) {
            projectCommitParentRepository.save(ProjectCommitParent.builder()
                    .commitId(mergeCommit.getId())
                    .parentCommitId(target.getHeadCommitId())
                    .parentOrder(1)
                    .build());
        }
        projectCommitParentRepository.save(ProjectCommitParent.builder()
                .commitId(mergeCommit.getId())
                .parentCommitId(sourceHead.getId())
                .parentOrder(2)
                .build());

        ProjectSnapshot snapshot = projectSnapshotRepository.save(ProjectSnapshot.builder()
                .repositoryId(target.getRepositoryId())
                .commitId(mergeCommit.getId())
                .manifestHash(UUID.randomUUID().toString().replace("-", ""))
                .fileCount(0)
                .build());

        if (sourceHead.getSnapshotId() != null) {
            List<ProjectSnapshotItem> items = projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(sourceHead.getSnapshotId());
            for (ProjectSnapshotItem item : items) {
                projectSnapshotItemRepository.save(ProjectSnapshotItem.builder()
                        .snapshotId(snapshot.getId())
                        .projectFileId(item.getProjectFileId())
                        .projectFileVersionId(item.getProjectFileVersionId())
                        .blobId(item.getBlobId())
                        .canonicalPath(item.getCanonicalPath())
                        .contentHash(item.getContentHash())
                        .build());
            }
            snapshot.setFileCount(items.size());
            projectSnapshotRepository.save(snapshot);
        }

        mergeCommit.setSnapshotId(snapshot.getId());
        projectCommitRepository.save(mergeCommit);

        target.setHeadCommitId(mergeCommit.getId());
        projectBranchRepository.save(target);

        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(target.getRepositoryId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        if (Objects.equals(repo.getDefaultBranchId(), target.getId())) {
            repo.setHeadCommitId(mergeCommit.getId());
            projectCodeRepositoryRepository.save(repo);
        }

        mr.setStatus("merged");
        mr.setMergedBy(currentUserId);
        mr.setMergedAt(java.time.LocalDateTime.now());
        projectMergeRequestRepository.save(mr);

        return toVO(mr);
    }

    private ProjectMergeRequestVO toVO(ProjectMergeRequest mr) {
        return ProjectMergeRequestVO.builder()
                .id(mr.getId())
                .repositoryId(mr.getRepositoryId())
                .sourceBranchId(mr.getSourceBranchId())
                .targetBranchId(mr.getTargetBranchId())
                .sourceHeadCommitId(mr.getSourceHeadCommitId())
                .targetHeadCommitId(mr.getTargetHeadCommitId())
                .title(mr.getTitle())
                .description(mr.getDescription())
                .status(mr.getStatus())
                .createdBy(mr.getCreatedBy())
                .mergedBy(mr.getMergedBy())
                .mergedAt(mr.getMergedAt())
                .createdAt(mr.getCreatedAt())
                .reviews(new ArrayList<>(projectReviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(mr.getId())))
                .checks(new ArrayList<>(projectCheckRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(mr.getId())))
                .build();
    }
}

package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectMergeRequestCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectReviewSubmitRequest;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectMergeRequest;
import com.alikeyou.itmoduleproject.entity.ProjectReview;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMergeRequestRepository;
import com.alikeyou.itmoduleproject.repository.ProjectReviewRepository;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.vo.ProjectMergeRequestVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * MR create/list/review and MR head synchronization lifecycle concerns.
 */
public class ProjectMergeRequestLifecycleSupport {

    private static final String MERGE_REQUEST_CREATE_ACTION = "mr_create";
    private static final String MERGE_REQUEST_REVIEW_ACTION = "mr_review";

    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectMergeRequestRepository projectMergeRequestRepository;
    private final ProjectReviewRepository projectReviewRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectMergeRequestActivitySupport activitySupport;

    public ProjectMergeRequestLifecycleSupport(ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                               ProjectBranchRepository projectBranchRepository,
                                               ProjectMergeRequestRepository projectMergeRequestRepository,
                                               ProjectReviewRepository projectReviewRepository,
                                               ProjectPermissionService projectPermissionService,
                                               ProjectMergeRequestActivitySupport activitySupport) {
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectBranchRepository = projectBranchRepository;
        this.projectMergeRequestRepository = projectMergeRequestRepository;
        this.projectReviewRepository = projectReviewRepository;
        this.projectPermissionService = projectPermissionService;
        this.activitySupport = activitySupport;
    }

    public ProjectMergeRequestVO create(ProjectMergeRequestCreateRequest request,
                                        Long currentUserId,
                                        Function<ProjectMergeRequest, ProjectMergeRequestVO> voMapper) {
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
                .title(resolveTitle(request.getTitle(), source, target))
                .description(request.getDescription())
                .status("open")
                .createdBy(currentUserId)
                .build());
        activitySupport.recordMergeRequestActivity(
                repo.getProjectId(),
                currentUserId,
                MERGE_REQUEST_CREATE_ACTION,
                mr,
                source,
                source.getHeadCommitId(),
                activitySupport.buildMergeRequestActivitySummary("Created merge request", source, target),
                activitySupport.buildMergeRequestActivityDetails(
                        mr.getTitle(),
                        mr.getStatus(),
                        source.getId(),
                        target.getId(),
                        source.getHeadCommitId(),
                        target.getHeadCommitId(),
                        null,
                        null,
                        null
                )
        );
        return voMapper.apply(mr);
    }

    public List<ProjectMergeRequestVO> list(Long projectId,
                                            String status,
                                            Long currentUserId,
                                            Function<ProjectMergeRequest, ProjectMergeRequestVO> voMapper) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        List<ProjectMergeRequest> list = (status == null || status.isBlank())
                ? projectMergeRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(repo.getId())
                : projectMergeRequestRepository.findByRepositoryIdAndStatusOrderByCreatedAtDesc(repo.getId(), status);
        return list.stream().map(this::refreshHeadsIfNeeded).map(voMapper).toList();
    }

    public ProjectMergeRequestVO review(Long mergeRequestId,
                                        ProjectReviewSubmitRequest request,
                                        Long currentUserId,
                                        Function<ProjectMergeRequest, ProjectMergeRequestVO> voMapper) {
        ProjectMergeRequest mr = projectMergeRequestRepository.findById(mergeRequestId)
                .orElseThrow(() -> new BusinessException("合并请求不存在"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(mr.getRepositoryId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        projectPermissionService.assertProjectManageMembers(repo.getProjectId(), currentUserId);
        if (!"open".equalsIgnoreCase(mr.getStatus())) {
            throw new BusinessException("当前合并请求不是打开状态");
        }
        mr = refreshHeadsIfNeeded(mr);
        ProjectReview review = projectReviewRepository.save(ProjectReview.builder()
                .mergeRequestId(mr.getId())
                .reviewerId(currentUserId)
                .reviewResult(request.getReviewResult() == null ? "comment" : request.getReviewResult())
                .reviewComment(request.getReviewComment())
                .build());
        ProjectBranch source = projectBranchRepository.findById(mr.getSourceBranchId()).orElse(null);
        ProjectBranch target = projectBranchRepository.findById(mr.getTargetBranchId()).orElse(null);
        activitySupport.recordMergeRequestActivity(
                repo.getProjectId(),
                currentUserId,
                MERGE_REQUEST_REVIEW_ACTION,
                mr,
                source,
                mr.getSourceHeadCommitId(),
                activitySupport.buildMergeRequestReviewSummary(review.getReviewResult(), source, target),
                activitySupport.buildMergeRequestActivityDetails(
                        mr.getTitle(),
                        mr.getStatus(),
                        mr.getSourceBranchId(),
                        mr.getTargetBranchId(),
                        mr.getSourceHeadCommitId(),
                        mr.getTargetHeadCommitId(),
                        review.getId(),
                        review.getReviewResult(),
                        review.getReviewComment()
                )
        );
        return voMapper.apply(mr);
    }

    public ProjectMergeRequest refreshHeadsIfNeeded(ProjectMergeRequest mr) {
        if (mr == null || !"open".equalsIgnoreCase(mr.getStatus())) {
            return mr;
        }
        ProjectBranch source = projectBranchRepository.findById(mr.getSourceBranchId()).orElse(null);
        ProjectBranch target = projectBranchRepository.findById(mr.getTargetBranchId()).orElse(null);
        return syncMergeRequestHeads(mr, source, target);
    }

    public ProjectMergeRequest syncMergeRequestHeads(ProjectMergeRequest mr, ProjectBranch source, ProjectBranch target) {
        boolean changed = false;
        if (source != null && source.getHeadCommitId() != null && !Objects.equals(mr.getSourceHeadCommitId(), source.getHeadCommitId())) {
            mr.setSourceHeadCommitId(source.getHeadCommitId());
            changed = true;
        }
        if (target != null && target.getHeadCommitId() != null && !Objects.equals(mr.getTargetHeadCommitId(), target.getHeadCommitId())) {
            mr.setTargetHeadCommitId(target.getHeadCommitId());
            changed = true;
        }
        return changed ? projectMergeRequestRepository.save(mr) : mr;
    }

    public ProjectMergeRequestVO toVO(ProjectMergeRequest mr) {
        if (mr == null) {
            return null;
        }
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
                .createdAt(mr.getCreatedAt())
                .mergedBy(mr.getMergedBy())
                .mergedAt(mr.getMergedAt())
                .reviews(new ArrayList<>(projectReviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(mr.getId())))
                .build();
    }

    public String resolveTitle(String rawTitle, ProjectBranch source, ProjectBranch target) {
        if (rawTitle != null && !rawTitle.isBlank()) {
            return rawTitle.trim();
        }
        return "Merge " + source.getName() + " -> " + target.getName();
    }

    public void assertBranchBelongsToRepository(ProjectBranch branch, Long repositoryId, String label) {
        if (branch == null || !Objects.equals(branch.getRepositoryId(), repositoryId)) {
            throw new BusinessException(label + "不属于当前项目仓库");
        }
    }
}

package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectCheckRunRequest;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCheckRun;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectMergeRequest;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCheckRunRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMergeRequestRepository;
import com.alikeyou.itmoduleproject.service.ProjectCheckRunService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.vo.ProjectCheckRunVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ProjectCheckRunServiceImpl implements ProjectCheckRunService {

    private static final String MERGE_CHECK_TYPE = "merge_conflict";
    private static final String MERGE_CONFLICT_RESOLUTION_TYPE = "merge_conflict_resolution";
    private static final Set<String> SYSTEM_INTERNAL_CHECK_TYPES = Set.of(
            MERGE_CHECK_TYPE,
            MERGE_CONFLICT_RESOLUTION_TYPE
    );

    private final ProjectCheckRunRepository projectCheckRunRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectMergeRequestRepository projectMergeRequestRepository;

    public ProjectCheckRunServiceImpl(ProjectCheckRunRepository projectCheckRunRepository,
                                      ProjectBranchRepository projectBranchRepository,
                                      ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                      ProjectCommitRepository projectCommitRepository,
                                      ProjectMergeRequestRepository projectMergeRequestRepository) {
        this.projectCheckRunRepository = projectCheckRunRepository;
        this.projectBranchRepository = projectBranchRepository;
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectCommitRepository = projectCommitRepository;
        this.projectMergeRequestRepository = projectMergeRequestRepository;
    }

    @Override
    @Transactional
    public ProjectCheckRunVO run(ProjectCheckRunRequest request) {
        if (request == null || request.getProjectId() == null) {
            throw new BusinessException("project id is required");
        }
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new BusinessException("project repository not found"));
        ProjectMergeRequest mergeRequest = null;
        Long resolvedCommitId = request.getCommitId();
        if (request.getMergeRequestId() == null && resolvedCommitId == null) {
            throw new BusinessException("either commitId or mergeRequestId is required");
        }
        if (request.getMergeRequestId() != null) {
            mergeRequest = projectMergeRequestRepository.findById(request.getMergeRequestId())
                    .orElseThrow(() -> new BusinessException("merge request not found"));
            if (!repo.getId().equals(mergeRequest.getRepositoryId())) {
                throw new BusinessException("merge request does not belong to repository");
            }
            ProjectBranch sourceBranch = projectBranchRepository.findById(mergeRequest.getSourceBranchId())
                    .orElseThrow(() -> new BusinessException("source branch not found"));
            if (!repo.getId().equals(sourceBranch.getRepositoryId())) {
                throw new BusinessException("source branch does not belong to repository");
            }
            if (sourceBranch.getHeadCommitId() != null && !sourceBranch.getHeadCommitId().equals(mergeRequest.getSourceHeadCommitId())) {
                mergeRequest.setSourceHeadCommitId(sourceBranch.getHeadCommitId());
                projectMergeRequestRepository.save(mergeRequest);
            }
            resolvedCommitId = sourceBranch.getHeadCommitId() != null ? sourceBranch.getHeadCommitId() : mergeRequest.getSourceHeadCommitId();
            if (resolvedCommitId == null) {
                throw new BusinessException("merge request has no source commit");
            }
        }
        if (resolvedCommitId != null) {
            ProjectCommit commit = projectCommitRepository.findById(resolvedCommitId)
                    .orElseThrow(() -> new BusinessException("commit not found"));
            if (!repo.getId().equals(commit.getRepositoryId())) {
                throw new BusinessException("commit does not belong to repository");
            }
        }
        String status = normalizeStatus(request.getCheckStatus());
        ProjectCheckRun checkRun = projectCheckRunRepository.save(ProjectCheckRun.builder()
                .repositoryId(repo.getId())
                .commitId(resolvedCommitId)
                .mergeRequestId(request.getMergeRequestId())
                .checkType(request.getCheckType() == null ? "custom" : request.getCheckType())
                .checkStatus(status)
                .summary(resolveSummary(request.getSummary(), status))
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .build());
        boolean protectedTargetBranch = mergeRequest != null && isProtectedBranch(mergeRequest.getTargetBranchId());
        return toVO(checkRun, protectedTargetBranch);
    }

    @Override
    public List<ProjectCheckRunVO> listByCommit(Long commitId) {
        return projectCheckRunRepository.findByCommitIdOrderByCreatedAtDesc(commitId).stream()
                .map(item -> toVO(item, false))
                .toList();
    }

    @Override
    public List<ProjectCheckRunVO> listByMergeRequest(Long mergeRequestId) {
        if (mergeRequestId == null) {
            return List.of();
        }
        ProjectMergeRequest mergeRequest = projectMergeRequestRepository.findById(mergeRequestId)
                .orElseThrow(() -> new BusinessException("merge request not found"));
        Long currentCommitId = resolveCurrentSourceCommitId(mergeRequest);
        if (currentCommitId == null) {
            return List.of();
        }
        boolean protectedTargetBranch = isProtectedBranch(mergeRequest.getTargetBranchId());
        return resolveEffectiveChecks(mergeRequestId, currentCommitId).stream()
                .map(item -> toVO(item, protectedTargetBranch))
                .toList();
    }

    private List<ProjectCheckRun> resolveEffectiveChecks(Long mergeRequestId, Long currentCommitId) {
        if (mergeRequestId == null || currentCommitId == null) {
            return List.of();
        }
        Map<String, ProjectCheckRun> latestByType = new LinkedHashMap<>();
        for (ProjectCheckRun item : projectCheckRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(mergeRequestId)) {
            if (!Objects.equals(item.getCommitId(), currentCommitId)) {
                continue;
            }
            latestByType.putIfAbsent(normalizeCheckType(item.getCheckType()), item);
        }
        return new ArrayList<>(latestByType.values());
    }

    private Long resolveCurrentSourceCommitId(ProjectMergeRequest mergeRequest) {
        if (mergeRequest == null) {
            return null;
        }
        ProjectBranch sourceBranch = projectBranchRepository.findById(mergeRequest.getSourceBranchId()).orElse(null);
        if (sourceBranch != null && sourceBranch.getHeadCommitId() != null) {
            if (!Objects.equals(sourceBranch.getHeadCommitId(), mergeRequest.getSourceHeadCommitId())) {
                mergeRequest.setSourceHeadCommitId(sourceBranch.getHeadCommitId());
                projectMergeRequestRepository.save(mergeRequest);
            }
            return sourceBranch.getHeadCommitId();
        }
        return mergeRequest.getSourceHeadCommitId();
    }

    private boolean isProtectedBranch(Long branchId) {
        if (branchId == null) {
            return false;
        }
        return projectBranchRepository.findById(branchId)
                .map(ProjectBranch::getProtectedFlag)
                .map(Boolean::booleanValue)
                .orElse(false);
    }

    private boolean isSystemInternalCheck(String checkType) {
        return SYSTEM_INTERNAL_CHECK_TYPES.contains(normalizeCheckType(checkType));
    }

    private ProjectCheckRunVO toVO(ProjectCheckRun checkRun, boolean protectedTargetBranch) {
        boolean systemInternal = isSystemInternalCheck(checkRun == null ? null : checkRun.getCheckType());
        boolean blockingMerge = checkRun != null
                && protectedTargetBranch
                && !systemInternal
                && "failed".equalsIgnoreCase(checkRun.getCheckStatus());
        return ProjectCheckRunVO.builder()
                .id(checkRun.getId())
                .repositoryId(checkRun.getRepositoryId())
                .commitId(checkRun.getCommitId())
                .mergeRequestId(checkRun.getMergeRequestId())
                .checkType(checkRun.getCheckType())
                .checkStatus(checkRun.getCheckStatus())
                .summary(checkRun.getSummary())
                .logPath(checkRun.getLogPath())
                .startedAt(checkRun.getStartedAt())
                .finishedAt(checkRun.getFinishedAt())
                .createdAt(checkRun.getCreatedAt())
                .blockingMerge(blockingMerge)
                .systemInternal(systemInternal)
                .build();
    }

    private String normalizeCheckType(String checkType) {
        if (checkType == null || checkType.isBlank()) {
            return "custom";
        }
        return checkType.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "queued", "running", "failed", "success" -> value;
            default -> "success";
        };
    }

    private String resolveSummary(String summary, String status) {
        if (summary != null && !summary.isBlank()) {
            return summary.trim();
        }
        return switch (status) {
            case "failed" -> "manual check failed";
            case "running" -> "manual check running";
            case "queued" -> "manual check queued";
            default -> "manual check passed";
        };
    }
}

package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectCheckRunRequest;
import com.alikeyou.itmoduleproject.entity.ProjectCheckRun;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectMergeRequest;
import com.alikeyou.itmoduleproject.repository.ProjectCheckRunRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMergeRequestRepository;
import com.alikeyou.itmoduleproject.service.ProjectCheckRunService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.vo.ProjectCheckRunVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectCheckRunServiceImpl implements ProjectCheckRunService {

    private final ProjectCheckRunRepository projectCheckRunRepository;
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectMergeRequestRepository projectMergeRequestRepository;

    public ProjectCheckRunServiceImpl(ProjectCheckRunRepository projectCheckRunRepository,
                                      ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                      ProjectCommitRepository projectCommitRepository,
                                      ProjectMergeRequestRepository projectMergeRequestRepository) {
        this.projectCheckRunRepository = projectCheckRunRepository;
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectCommitRepository = projectCommitRepository;
        this.projectMergeRequestRepository = projectMergeRequestRepository;
    }

    @Override
    @Transactional
    public ProjectCheckRunVO run(ProjectCheckRunRequest request) {
        if (request == null || request.getProjectId() == null) {
            throw new BusinessException("项目ID不能为空");
        }
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        Long resolvedCommitId = request.getCommitId();
        if (request.getMergeRequestId() == null && resolvedCommitId == null) {
            throw new BusinessException("请至少指定提交或合并请求");
        }
        if (request.getMergeRequestId() != null) {
            ProjectMergeRequest mergeRequest = projectMergeRequestRepository.findById(request.getMergeRequestId())
                    .orElseThrow(() -> new BusinessException("合并请求不存在"));
            if (!repo.getId().equals(mergeRequest.getRepositoryId())) {
                throw new BusinessException("合并请求不属于当前项目仓库");
            }
            if (resolvedCommitId == null) {
                resolvedCommitId = mergeRequest.getSourceHeadCommitId();
            }
        }
        if (resolvedCommitId != null) {
            ProjectCommit commit = projectCommitRepository.findById(resolvedCommitId)
                    .orElseThrow(() -> new BusinessException("提交不存在"));
            if (!repo.getId().equals(commit.getRepositoryId())) {
                throw new BusinessException("提交不属于当前项目仓库");
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
        return toVO(checkRun);
    }

    @Override
    public List<ProjectCheckRunVO> listByCommit(Long commitId) {
        return projectCheckRunRepository.findByCommitIdOrderByCreatedAtDesc(commitId).stream().map(this::toVO).toList();
    }

    @Override
    public List<ProjectCheckRunVO> listByMergeRequest(Long mergeRequestId) {
        return projectCheckRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(mergeRequestId).stream().map(this::toVO).toList();
    }

    private ProjectCheckRunVO toVO(ProjectCheckRun checkRun) {
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
                .build();
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toLowerCase();
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

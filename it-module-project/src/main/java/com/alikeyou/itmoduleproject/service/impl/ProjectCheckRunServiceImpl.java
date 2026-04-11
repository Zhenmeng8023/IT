package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectCheckRunRequest;
import com.alikeyou.itmoduleproject.entity.ProjectCheckRun;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCheckRunRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
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

    public ProjectCheckRunServiceImpl(ProjectCheckRunRepository projectCheckRunRepository,
                                      ProjectCodeRepositoryRepository projectCodeRepositoryRepository) {
        this.projectCheckRunRepository = projectCheckRunRepository;
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
    }

    @Override
    @Transactional
    public ProjectCheckRunVO run(ProjectCheckRunRequest request) {
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        ProjectCheckRun checkRun = projectCheckRunRepository.save(ProjectCheckRun.builder()
                .repositoryId(repo.getId())
                .commitId(request.getCommitId())
                .mergeRequestId(request.getMergeRequestId())
                .checkType(request.getCheckType() == null ? "custom" : request.getCheckType())
                .checkStatus("success")
                .summary(request.getSummary() == null ? "manual check passed" : request.getSummary())
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
}

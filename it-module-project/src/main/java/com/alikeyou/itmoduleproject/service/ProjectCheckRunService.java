package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectCheckRunRequest;
import com.alikeyou.itmoduleproject.vo.ProjectCheckRunVO;

import java.util.List;

public interface ProjectCheckRunService {
    ProjectCheckRunVO run(ProjectCheckRunRequest request);
    List<ProjectCheckRunVO> listByCommit(Long commitId);
    List<ProjectCheckRunVO> listByMergeRequest(Long mergeRequestId);
}

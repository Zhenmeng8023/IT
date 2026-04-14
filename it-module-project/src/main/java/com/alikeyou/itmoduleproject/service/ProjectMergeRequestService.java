package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectMergeRequestCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectReviewSubmitRequest;
import com.alikeyou.itmoduleproject.support.diff.MergeCheckResult;
import com.alikeyou.itmoduleproject.vo.ProjectMergeRequestVO;

import java.util.List;

public interface ProjectMergeRequestService {
    ProjectMergeRequestVO create(ProjectMergeRequestCreateRequest request, Long currentUserId);
    List<ProjectMergeRequestVO> list(Long projectId, String status, Long currentUserId);
    ProjectMergeRequestVO review(Long mergeRequestId, ProjectReviewSubmitRequest request, Long currentUserId);
    MergeCheckResult checkMerge(Long mergeRequestId, Long currentUserId);
    MergeCheckResult latestMergeCheck(Long mergeRequestId, Long currentUserId);
    MergeCheckResult recheckMerge(Long mergeRequestId, Long currentUserId);
    MergeCheckResult preMergeCheck(Long mergeRequestId, Long currentUserId);
    ProjectMergeRequestVO merge(Long mergeRequestId, Long currentUserId);
}

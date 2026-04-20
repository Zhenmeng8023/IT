package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.support.diff.MergeCheckResult;
import com.alikeyou.itmoduleproject.support.diff.ProjectMergeDiffSupport;

import java.util.Map;
import java.util.function.Function;

/**
 * Default conflict detector backed by snapshot three-way diff.
 */
public class DefaultProjectMergeConflictDetector implements ProjectMergeConflictDetector {

    @Override
    public MergeCheckResult detect(Long mergeRequestId,
                                   Long repositoryId,
                                   Long sourceBranchId,
                                   Long targetBranchId,
                                   Long baseCommitId,
                                   Long sourceCommitId,
                                   Long targetCommitId,
                                   Map<String, ProjectSnapshotItem> baseSnapshot,
                                   Map<String, ProjectSnapshotItem> sourceSnapshot,
                                   Map<String, ProjectSnapshotItem> targetSnapshot,
                                   Function<Long, Boolean> binaryFlagResolver) {
        return ProjectMergeDiffSupport.buildMergeCheck(
                mergeRequestId,
                repositoryId,
                sourceBranchId,
                targetBranchId,
                baseCommitId,
                sourceCommitId,
                targetCommitId,
                baseSnapshot,
                sourceSnapshot,
                targetSnapshot,
                binaryFlagResolver
        );
    }
}

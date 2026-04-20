package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.support.diff.MergeCheckResult;

import java.util.Map;
import java.util.function.Function;

/**
 * Extension point for merge conflict detection strategy.
 */
@FunctionalInterface
public interface ProjectMergeConflictDetector {

    MergeCheckResult detect(Long mergeRequestId,
                            Long repositoryId,
                            Long sourceBranchId,
                            Long targetBranchId,
                            Long baseCommitId,
                            Long sourceCommitId,
                            Long targetCommitId,
                            Map<String, ProjectSnapshotItem> baseSnapshot,
                            Map<String, ProjectSnapshotItem> sourceSnapshot,
                            Map<String, ProjectSnapshotItem> targetSnapshot,
                            Function<Long, Boolean> binaryFlagResolver);
}

package com.alikeyou.itmoduleproject.support.diff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCheckResult {
    private Long mergeRequestId;
    private Long repositoryId;
    private Long sourceBranchId;
    private Long targetBranchId;
    private Long baseCommitId;
    private Long sourceCommitId;
    private Long targetCommitId;
    private Boolean mergeable;
    private Integer totalConflicts;
    private Boolean requiresBranchUpdate;
    private Boolean requiresRecheck;
    private List<ChangeEntry> changes;
    private List<ConflictDetail> conflicts;
    private List<String> blockingReasons;
    private String summary;
    private String suggestedAction;
    private String severity;
    private Map<String, Object> metadata;
}

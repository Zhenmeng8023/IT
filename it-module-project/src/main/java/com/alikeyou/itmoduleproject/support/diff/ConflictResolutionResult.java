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
public class ConflictResolutionResult {
    private Long mergeRequestId;
    private Long sourceCommitId;
    private Long targetCommitId;
    private Boolean resolved;
    private Integer requestedConflictCount;
    private Integer appliedConflictCount;
    private Long supplementalCommitId;
    private Boolean equivalentResult;
    private Long resolutionCheckRunId;
    private Long resolutionActivityLogId;
    private List<String> unresolvedConflictIds;
    private String summary;
    private String suggestedAction;
    private MergeCheckResult latestMergeCheck;
    private Map<String, Object> metadata;
}

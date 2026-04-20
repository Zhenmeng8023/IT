package com.alikeyou.itmoduleproject.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alikeyou.itmoduleproject.entity.ProjectActivityLog;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectMergeRequest;
import com.alikeyou.itmoduleproject.repository.ProjectActivityLogRepository;
import com.alikeyou.itmoduleproject.support.diff.ConflictDetail;
import com.alikeyou.itmoduleproject.support.diff.ConflictResolutionOption;
import com.alikeyou.itmoduleproject.support.diff.MergeCheckResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Activity/log serialization and trace composition for merge-request workflow.
 */
public class ProjectMergeRequestActivitySupport {

    private final ProjectActivityLogRepository projectActivityLogRepository;
    private final ObjectMapper objectMapper;

    public ProjectMergeRequestActivitySupport(ProjectActivityLogRepository projectActivityLogRepository,
                                              ObjectMapper objectMapper) {
        this.projectActivityLogRepository = projectActivityLogRepository;
        this.objectMapper = objectMapper;
    }

    public String writeMergeCheckJson(MergeCheckResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    public void recordMergeRequestActivity(Long projectId,
                                           Long operatorId,
                                           String action,
                                           ProjectMergeRequest mr,
                                           ProjectBranch branch,
                                           Long commitId,
                                           String content,
                                           Map<String, Object> details) {
        if (projectId == null || mr == null || action == null || action.isBlank()) {
            return;
        }
        projectActivityLogRepository.save(ProjectActivityLog.builder()
                .projectId(projectId)
                .operatorId(operatorId)
                .action(action)
                .targetType("merge_request")
                .targetId(mr.getId())
                .branchId(branch == null ? null : branch.getId())
                .commitId(commitId)
                .mergeRequestId(mr.getId())
                .content(truncate(content, 255))
                .details(writeJson(details))
                .build());
    }

    public String writeJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    public void recordConflictResolutionActivity(Long projectId,
                                                 Long operatorId,
                                                 String action,
                                                 Long mergeRequestId,
                                                 Long sourceHeadCommitId,
                                                 Long sourceBranchId,
                                                 String content,
                                                 Map<String, Object> details) {
        projectActivityLogRepository.save(ProjectActivityLog.builder()
                .projectId(projectId)
                .operatorId(operatorId)
                .action(action)
                .targetType("merge_request")
                .targetId(mergeRequestId)
                .commitId(sourceHeadCommitId)
                .branchId(sourceBranchId)
                .mergeRequestId(mergeRequestId)
                .content(truncate(content, 255))
                .details(writeJson(details))
                .build());
    }

    public List<Map<String, Object>> buildConflictResolutionOptionTrace(List<ConflictResolutionOption> options,
                                                                        Map<String, ConflictDetail> conflictsById,
                                                                        Function<ConflictDetail, String> conflictPathResolver) {
        List<Map<String, Object>> trace = new ArrayList<>();
        if (options == null) {
            return trace;
        }
        for (ConflictResolutionOption option : options) {
            if (option == null) {
                continue;
            }
            ConflictDetail conflict = option.getConflictId() == null ? null : conflictsById.get(option.getConflictId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("conflictId", option.getConflictId());
            item.put("conflictType", conflict == null ? null : Objects.toString(conflict.getConflictType(), null));
            item.put("path", conflict == null ? null : conflictPathResolver.apply(conflict));
            item.put("fileName", conflict == null ? null : conflict.getFileName());
            item.put("basePath", conflict == null ? null : conflict.getBasePath());
            item.put("sourcePath", conflict == null ? null : conflict.getSourcePath());
            item.put("conflictTargetPath", conflict == null ? null : conflict.getTargetPath());
            item.put("binaryFile", conflict == null ? null : conflict.getBinaryFile());
            item.put("strategy", option.normalizedStrategy());
            item.put("targetPath", option.getTargetPath());
            item.put("requestedTargetPath", option.getTargetPath());
            item.put("note", option.getNote());
            item.put("summary", conflict == null ? null : conflict.getSummary());
            item.put("suggestedAction", conflict == null ? null : conflict.getSuggestedAction());
            item.put("severity", conflict == null ? null : conflict.getSeverity());
            item.put("metadata", option.getMetadata());
            trace.add(item);
        }
        return trace;
    }

    public Map<String, Object> buildConflictResolutionTraceDetails(Long mergeRequestId,
                                                                   List<Map<String, Object>> options,
                                                                   Long operatorId,
                                                                   Long createdCommitId,
                                                                   MergeCheckResult recheckResult) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mergeRequestId", mergeRequestId);
        details.put("operator", operatorId);
        details.put("createdCommitId", createdCommitId);
        details.put("conflicts", options == null ? List.of() : options);
        details.put("recheckResult", buildConflictResolutionRecheckResult(recheckResult));
        details.put("latestMergeCheck", recheckResult);
        return details;
    }

    public Map<String, Object> buildConflictResolutionRecheckResult(MergeCheckResult recheckResult) {
        if (recheckResult == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mergeable", recheckResult.getMergeable());
        result.put("requiresRecheck", recheckResult.getRequiresRecheck());
        result.put("requiresBranchUpdate", recheckResult.getRequiresBranchUpdate());
        result.put("sourceCommitId", recheckResult.getSourceCommitId());
        result.put("targetCommitId", recheckResult.getTargetCommitId());
        result.put("baseCommitId", recheckResult.getBaseCommitId());
        result.put("summary", recheckResult.getSummary());
        result.put("blockingReasons", recheckResult.getBlockingReasons());
        List<String> conflictIds = recheckResult.getConflicts() == null ? List.of() : recheckResult.getConflicts().stream()
                .filter(Objects::nonNull)
                .map(ConflictDetail::getConflictId)
                .filter(Objects::nonNull)
                .toList();
        int conflictCount = conflictIds.size();
        result.put("conflictCount", conflictCount);
        result.put("remainingConflictCount", conflictCount);
        result.put("unresolvedConflictIds", conflictIds);
        return result;
    }

    public String buildMergeRequestActivitySummary(String prefix, ProjectBranch source, ProjectBranch target) {
        StringBuilder builder = new StringBuilder(prefix);
        if (source != null || target != null) {
            builder.append(": ");
            builder.append(source == null ? "-" : source.getName());
            builder.append(" -> ");
            builder.append(target == null ? "-" : target.getName());
        }
        return builder.toString();
    }

    public String buildMergeRequestReviewSummary(String reviewResult, ProjectBranch source, ProjectBranch target) {
        String normalizedResult = reviewResult == null || reviewResult.isBlank() ? "comment" : reviewResult.trim().toLowerCase(Locale.ROOT);
        String prefix = switch (normalizedResult) {
            case "approve" -> "Reviewed merge request: approved";
            case "reject" -> "Reviewed merge request: rejected";
            default -> "Reviewed merge request: commented";
        };
        return buildMergeRequestActivitySummary(prefix, source, target);
    }

    public Map<String, Object> buildMergeRequestActivityDetails(String title,
                                                                String status,
                                                                Long sourceBranchId,
                                                                Long targetBranchId,
                                                                Long sourceHeadCommitId,
                                                                Long targetHeadCommitId,
                                                                Long reviewId,
                                                                String reviewResult,
                                                                String reviewComment) {
        return buildMergeRequestActivityDetails(
                title,
                status,
                sourceBranchId,
                targetBranchId,
                sourceHeadCommitId,
                targetHeadCommitId,
                reviewId,
                reviewResult,
                reviewComment,
                null,
                null
        );
    }

    public Map<String, Object> buildMergeRequestActivityDetails(String title,
                                                                String status,
                                                                Long sourceBranchId,
                                                                Long targetBranchId,
                                                                Long sourceHeadCommitId,
                                                                Long targetHeadCommitId,
                                                                Long reviewId,
                                                                String reviewResult,
                                                                String reviewComment,
                                                                Long mergeCommitId,
                                                                Long mergeBaseCommitId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("title", title);
        details.put("status", status);
        details.put("sourceBranchId", sourceBranchId);
        details.put("targetBranchId", targetBranchId);
        details.put("sourceHeadCommitId", sourceHeadCommitId);
        details.put("targetHeadCommitId", targetHeadCommitId);
        if (reviewId != null) {
            details.put("reviewId", reviewId);
        }
        if (reviewResult != null) {
            details.put("reviewResult", reviewResult);
        }
        if (reviewComment != null) {
            details.put("reviewComment", reviewComment);
        }
        if (mergeCommitId != null) {
            details.put("mergeCommitId", mergeCommitId);
        }
        if (mergeBaseCommitId != null) {
            details.put("mergeBaseCommitId", mergeBaseCommitId);
        }
        return details;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}

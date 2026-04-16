package com.alikeyou.itmoduleproject.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.alikeyou.itmoduleproject.dto.ConflictResolutionRequest;
import com.alikeyou.itmoduleproject.dto.ContentConflictResolveRequest;
import com.alikeyou.itmoduleproject.dto.ProjectMergeRequestCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectReviewSubmitRequest;
import com.alikeyou.itmoduleproject.entity.*;
import com.alikeyou.itmoduleproject.repository.*;
import com.alikeyou.itmoduleproject.service.ProjectMergeRequestService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectFileTypeSupport;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepoStorageSupport;
import com.alikeyou.itmoduleproject.support.diff.ContentConflictBlock;
import com.alikeyou.itmoduleproject.support.diff.ContentConflictDetail;
import com.alikeyou.itmoduleproject.support.diff.ConflictDetail;
import com.alikeyou.itmoduleproject.support.diff.ConflictResolutionOption;
import com.alikeyou.itmoduleproject.support.diff.ConflictResolutionResult;
import com.alikeyou.itmoduleproject.support.diff.ConflictType;
import com.alikeyou.itmoduleproject.support.diff.MergeCheckResult;
import com.alikeyou.itmoduleproject.support.diff.ProjectMergeDiffSupport;
import com.alikeyou.itmoduleproject.vo.ProjectMergeRequestVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class ProjectMergeRequestServiceImpl implements ProjectMergeRequestService {

    private static final String MERGE_CHECK_TYPE = "merge_conflict";
    private static final String MERGE_CONFLICT_RESOLVE_TYPE = "merge_conflict_resolution";
    private static final String MERGE_CHECK_ACTION = "mr_merge_check";
    private static final String MERGE_CONFLICT_RESOLVE_ACTION = "mr_conflict_resolve";
    private static final String MERGE_CONFLICT_RESOLVE_START_ACTION = "mr_conflict_resolve_start";
    private static final String MERGE_CONFLICT_RESOLVE_APPLY_ACTION = "mr_conflict_resolve_apply";
    private static final String MERGE_CONFLICT_RESOLVE_RECHECK_ACTION = "mr_conflict_resolve_recheck";
    private static final String MERGE_CONFLICT_RESOLVE_FAIL_ACTION = "mr_conflict_resolve_fail";
    private static final String MERGE_REQUEST_CREATE_ACTION = "mr_create";
    private static final String MERGE_REQUEST_REVIEW_ACTION = "mr_review";
    private static final String MERGE_REQUEST_MERGE_ACTION = "mr_merge";
    private static final String CONTENT_CONFLICT_MANUAL_STRATEGY = "MANUAL_CONTENT";
    private static final Set<ConflictType> STRUCTURED_RESOLVABLE_CONFLICT_TYPES = EnumSet.of(
            ConflictType.STALE_BRANCH,
            ConflictType.DELETE_MODIFY_CONFLICT,
            ConflictType.RENAME_CONFLICT,
            ConflictType.MOVE_CONFLICT,
            ConflictType.TARGET_PATH_OCCUPIED
    );

    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectMergeRequestRepository projectMergeRequestRepository;
    private final ProjectReviewRepository projectReviewRepository;
    private final ProjectCheckRunRepository projectCheckRunRepository;
    private final ProjectActivityLogRepository projectActivityLogRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectCommitParentRepository projectCommitParentRepository;
    private final ProjectSnapshotRepository projectSnapshotRepository;
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository;
    private final ProjectCommitChangeRepository projectCommitChangeRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectBlobRepository projectBlobRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectRepoStorageSupport projectRepoStorageSupport;
    private final ObjectMapper objectMapper;

    public ProjectMergeRequestServiceImpl(ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                          ProjectBranchRepository projectBranchRepository,
                                          ProjectMergeRequestRepository projectMergeRequestRepository,
                                          ProjectReviewRepository projectReviewRepository,
                                          ProjectCheckRunRepository projectCheckRunRepository,
                                          ProjectActivityLogRepository projectActivityLogRepository,
                                          ProjectCommitRepository projectCommitRepository,
                                          ProjectCommitParentRepository projectCommitParentRepository,
                                          ProjectSnapshotRepository projectSnapshotRepository,
                                          ProjectSnapshotItemRepository projectSnapshotItemRepository,
                                          ProjectCommitChangeRepository projectCommitChangeRepository,
                                          ProjectFileRepository projectFileRepository,
                                          ProjectFileVersionRepository projectFileVersionRepository,
                                          ProjectBlobRepository projectBlobRepository,
                                          ProjectPermissionService projectPermissionService,
                                          ProjectRepoStorageSupport projectRepoStorageSupport,
                                          ObjectMapper objectMapper) {
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectBranchRepository = projectBranchRepository;
        this.projectMergeRequestRepository = projectMergeRequestRepository;
        this.projectReviewRepository = projectReviewRepository;
        this.projectCheckRunRepository = projectCheckRunRepository;
        this.projectActivityLogRepository = projectActivityLogRepository;
        this.projectCommitRepository = projectCommitRepository;
        this.projectCommitParentRepository = projectCommitParentRepository;
        this.projectSnapshotRepository = projectSnapshotRepository;
        this.projectSnapshotItemRepository = projectSnapshotItemRepository;
        this.projectCommitChangeRepository = projectCommitChangeRepository;
        this.projectFileRepository = projectFileRepository;
        this.projectFileVersionRepository = projectFileVersionRepository;
        this.projectBlobRepository = projectBlobRepository;
        this.projectPermissionService = projectPermissionService;
        this.projectRepoStorageSupport = projectRepoStorageSupport;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ProjectMergeRequestVO create(ProjectMergeRequestCreateRequest request, Long currentUserId) {
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        ProjectBranch source = projectBranchRepository.findById(request.getSourceBranchId())
                .orElseThrow(() -> new BusinessException("源分支不存在"));
        ProjectBranch target = projectBranchRepository.findById(request.getTargetBranchId())
                .orElseThrow(() -> new BusinessException("目标分支不存在"));
        if (!Objects.equals(source.getRepositoryId(), repo.getId()) || !Objects.equals(target.getRepositoryId(), repo.getId())) {
            throw new BusinessException("分支不属于当前项目仓库");
        }
        if (Objects.equals(source.getId(), target.getId())) {
            throw new BusinessException("源分支和目标分支不能相同");
        }
        ProjectMergeRequest mr = projectMergeRequestRepository.save(ProjectMergeRequest.builder()
                .repositoryId(repo.getId())
                .sourceBranchId(source.getId())
                .targetBranchId(target.getId())
                .sourceHeadCommitId(source.getHeadCommitId())
                .targetHeadCommitId(target.getHeadCommitId())
                .title(resolveTitle(request.getTitle(), source, target))
                .description(request.getDescription())
                .status("open")
                .createdBy(currentUserId)
                .build());
        recordMergeRequestActivity(
                repo.getProjectId(),
                currentUserId,
                MERGE_REQUEST_CREATE_ACTION,
                mr,
                source,
                source.getHeadCommitId(),
                buildMergeRequestActivitySummary("Created merge request", source, target),
                buildMergeRequestActivityDetails(
                        mr.getTitle(),
                        mr.getStatus(),
                        source.getId(),
                        target.getId(),
                        source.getHeadCommitId(),
                        target.getHeadCommitId(),
                        null,
                        null,
                        null
                )
        );
        return toVO(mr);
    }

    @Override
    public List<ProjectMergeRequestVO> list(Long projectId, String status, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        List<ProjectMergeRequest> list = (status == null || status.isBlank())
                ? projectMergeRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(repo.getId())
                : projectMergeRequestRepository.findByRepositoryIdAndStatusOrderByCreatedAtDesc(repo.getId(), status);
        return list.stream().map(this::refreshHeadsIfNeeded).map(this::toVO).toList();
    }

    @Override
    @Transactional
    public ProjectMergeRequestVO review(Long mergeRequestId, ProjectReviewSubmitRequest request, Long currentUserId) {
        ProjectMergeRequest mr = projectMergeRequestRepository.findById(mergeRequestId)
                .orElseThrow(() -> new BusinessException("合并请求不存在"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(mr.getRepositoryId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        projectPermissionService.assertProjectManageMembers(repo.getProjectId(), currentUserId);
        if (!"open".equalsIgnoreCase(mr.getStatus())) {
            throw new BusinessException("当前合并请求不是打开状态");
        }
        mr = refreshHeadsIfNeeded(mr);
        ProjectReview review = projectReviewRepository.save(ProjectReview.builder()
                .mergeRequestId(mr.getId())
                .reviewerId(currentUserId)
                .reviewResult(request.getReviewResult() == null ? "comment" : request.getReviewResult())
                .reviewComment(request.getReviewComment())
                .build());
        ProjectBranch source = projectBranchRepository.findById(mr.getSourceBranchId()).orElse(null);
        ProjectBranch target = projectBranchRepository.findById(mr.getTargetBranchId()).orElse(null);
        recordMergeRequestActivity(
                repo.getProjectId(),
                currentUserId,
                MERGE_REQUEST_REVIEW_ACTION,
                mr,
                source,
                mr.getSourceHeadCommitId(),
                buildMergeRequestReviewSummary(review.getReviewResult(), source, target),
                buildMergeRequestActivityDetails(
                        mr.getTitle(),
                        mr.getStatus(),
                        mr.getSourceBranchId(),
                        mr.getTargetBranchId(),
                        mr.getSourceHeadCommitId(),
                        mr.getTargetHeadCommitId(),
                        review.getId(),
                        review.getReviewResult(),
                        review.getReviewComment()
                )
        );
        return toVO(mr);
    }

    @Override
    public MergeCheckResult checkMerge(Long mergeRequestId, Long currentUserId) {
        MergeCheckContext context = resolveMergeCheckContext(mergeRequestId, currentUserId, false);
        MergeCheckResult result = runMergeCheck(context, currentUserId, true);
        return result;
    }

    @Override
    public MergeCheckResult latestMergeCheck(Long mergeRequestId, Long currentUserId) {
        MergeCheckContext context = resolveMergeCheckContext(mergeRequestId, currentUserId, false);
        MergeExecutionState executionState = loadMergeExecutionState(context);
        MergeCheckResult result = buildMergeCheckResult(context, executionState);
        result = applyStructuredResolutionIfPossible(context, executionState, result);
        return applyHeadDriftState(result, context.source().getHeadCommitId(), context.target().getHeadCommitId(), false);
    }

    @Override
    public MergeCheckResult recheckMerge(Long mergeRequestId, Long currentUserId) {
        MergeCheckContext context = resolveMergeCheckContext(mergeRequestId, currentUserId, false);
        MergeExecutionState executionState = loadMergeExecutionState(context);
        MergeCheckResult result = buildMergeCheckResult(context, executionState);
        result = applyStructuredResolutionIfPossible(context, executionState, result);
        persistMergeCheckResult(context, result, currentUserId);
        return result;
    }

    @Override
    public ContentConflictDetail getContentConflictDetail(Long mergeRequestId, String conflictId, Long currentUserId) {
        MergeCheckContext context = resolveMergeCheckContext(mergeRequestId, currentUserId, false);
        MergeCheckResult latestResult = latestMergeCheck(mergeRequestId, currentUserId);
        latestResult = applyHeadDriftState(
                latestResult,
                context.source().getHeadCommitId(),
                context.target().getHeadCommitId(),
                true
        );
        assertLatestMergeCheckUsable(latestResult);

        ConflictDetail conflict = requireContentConflict(conflictId, indexLatestConflictsById(latestResult));
        ConflictSnapshotView snapshotView = resolveConflictSnapshotView(conflict);

        String path = resolveConflictPath(conflict);
        List<ContentConflictBlock> blocks = Boolean.TRUE.equals(snapshotView.binaryFile()) ? List.of() : buildContentConflictBlocks(
                snapshotView.baseContent(),
                snapshotView.sourceContent(),
                snapshotView.targetContent()
        );
        return ContentConflictDetail.builder()
                .mergeRequestId(mergeRequestId)
                .conflictId(conflict.getConflictId())
                .conflictType(conflict.getConflictType())
                .path(path)
                .fileName(extractFileName(path))
                .basePath(conflict.getBasePath())
                .sourcePath(conflict.getSourcePath())
                .targetPath(conflict.getTargetPath())
                .baseCommitId(conflict.getBaseCommitId())
                .sourceCommitId(conflict.getSourceCommitId())
                .targetCommitId(conflict.getTargetCommitId())
                .baseContent(snapshotView.baseContent())
                .sourceContent(snapshotView.sourceContent())
                .targetContent(snapshotView.targetContent())
                .baseLineCount(snapshotView.baseLines().size())
                .sourceLineCount(snapshotView.sourceLines().size())
                .targetLineCount(snapshotView.targetLines().size())
                .binaryFile(snapshotView.binaryFile())
                .summary(conflict.getSummary())
                .suggestedAction(conflict.getSuggestedAction())
                .severity(conflict.getSeverity())
                .requiresRecheck(latestResult.getRequiresRecheck())
                .requiresBranchUpdate(latestResult.getRequiresBranchUpdate())
                .blockingReasons(latestResult.getBlockingReasons())
                .blocks(blocks)
                .metadata(buildContentConflictMetadata(conflict, latestResult, snapshotView))
                .build();
    }

    @Override
    @Transactional
    public ConflictResolutionResult resolveStructuredConflicts(Long mergeRequestId,
                                                               ConflictResolutionRequest request,
                                                               Long currentUserId) {
        MergeCheckContext context = resolveMergeCheckContext(mergeRequestId, currentUserId, true);
        MergeExecutionState executionState = loadMergeExecutionState(context);
        MergeCheckResult latestResult = buildMergeCheckResult(context, executionState);

        List<ConflictResolutionOption> options = resolveConflictOptions(request);
        Map<String, ConflictDetail> conflictsById = indexLatestConflictsById(latestResult);
        validateConflictResolutionOptions(options, conflictsById);
        validateStructuredResolutionPaths(options, conflictsById, executionState);
        List<Map<String, Object>> optionTrace = buildConflictResolutionOptionTrace(options, conflictsById);
        recordConflictResolutionActivity(
                context,
                currentUserId,
                MERGE_CONFLICT_RESOLVE_START_ACTION,
                "Conflict resolution started",
                buildConflictResolutionTraceDetails(
                        context.mr().getId(),
                        optionTrace,
                        currentUserId,
                        null,
                        latestResult
                )
        );
        AppliedStructuredResolution appliedResolution = applyStructuredResolutionPlan(
                context,
                executionState,
                latestResult,
                options,
                conflictsById
        );
        MergeCheckResult resolvedEquivalentResult = appliedResolution.mergeCheckResult();
        List<String> resolvedConflictIds = appliedResolution.resolvedConflictIds();
        List<String> unresolvedConflictIds = resolvedEquivalentResult.getConflicts().stream()
                .filter(Objects::nonNull)
                .map(ConflictDetail::getConflictId)
                .filter(Objects::nonNull)
                .toList();
        int remainingConflictCount = unresolvedConflictIds.size();

        ProjectCheckRun resolutionCheckRun = projectCheckRunRepository.save(ProjectCheckRun.builder()
                .repositoryId(context.repo().getId())
                .commitId(context.source().getHeadCommitId())
                .mergeRequestId(context.mr().getId())
                .checkType(MERGE_CONFLICT_RESOLVE_TYPE)
                .checkStatus(Boolean.TRUE.equals(resolvedEquivalentResult.getMergeable()) ? "success" : "failed")
                .summary(truncate(resolvedEquivalentResult.getSummary(), 500))
                .startedAt(java.time.LocalDateTime.now())
                .finishedAt(java.time.LocalDateTime.now())
                .build());

        Long createdCommitId = null;
        Map<String, Object> resolutionLogDetails = buildConflictResolutionTraceDetails(
                context.mr().getId(),
                optionTrace,
                currentUserId,
                createdCommitId,
                resolvedEquivalentResult
        );
        resolutionLogDetails.put("resolutionMode", "EQUIVALENT_RESULT");
        resolutionLogDetails.put("latestMergeCheck", resolvedEquivalentResult);
        ProjectActivityLog resolutionLog = projectActivityLogRepository.save(ProjectActivityLog.builder()
                .projectId(context.repo().getProjectId())
                .operatorId(currentUserId)
                .action(MERGE_CONFLICT_RESOLVE_ACTION)
                .targetType("merge_request")
                .targetId(context.mr().getId())
                .commitId(context.source().getHeadCommitId())
                .branchId(context.source().getId())
                .mergeRequestId(context.mr().getId())
                .checkRunId(resolutionCheckRun.getId())
                .content(truncate(resolvedEquivalentResult.getSummary(), 255))
                .details(writeJson(resolutionLogDetails))
                .build());

        recordConflictResolutionActivity(
                context,
                currentUserId,
                MERGE_CONFLICT_RESOLVE_APPLY_ACTION,
                "Conflict resolution strategy applied",
                buildConflictResolutionTraceDetails(
                        context.mr().getId(),
                        optionTrace,
                        currentUserId,
                        createdCommitId,
                        resolvedEquivalentResult
                )
        );
        recordConflictResolutionActivity(
                context,
                currentUserId,
                MERGE_CONFLICT_RESOLVE_RECHECK_ACTION,
                truncate(resolvedEquivalentResult.getSummary(), 255),
                buildConflictResolutionTraceDetails(
                        context.mr().getId(),
                        optionTrace,
                        currentUserId,
                        createdCommitId,
                        resolvedEquivalentResult
                )
        );
        if (!Boolean.TRUE.equals(resolvedEquivalentResult.getMergeable())) {
            recordConflictResolutionActivity(
                    context,
                    currentUserId,
                    MERGE_CONFLICT_RESOLVE_FAIL_ACTION,
                    truncate(resolvedEquivalentResult.getSummary(), 255),
                    buildConflictResolutionTraceDetails(
                            context.mr().getId(),
                            optionTrace,
                            currentUserId,
                            createdCommitId,
                            resolvedEquivalentResult
                    )
            );
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("resolutionMode", "EQUIVALENT_RESULT");
        metadata.put("resolvedConflictIds", resolvedConflictIds);
        metadata.put("remainingConflictCount", remainingConflictCount);
        metadata.put("requiresRecheck", resolvedEquivalentResult.getRequiresRecheck());
        metadata.put("requiresBranchUpdate", resolvedEquivalentResult.getRequiresBranchUpdate());
        metadata.put("blockingReasons", resolvedEquivalentResult.getBlockingReasons());
        metadata.put("unresolvedConflictIds", unresolvedConflictIds);
        metadata.put("recheckRecommended", shouldRecommendRecheck(request, resolvedEquivalentResult));
        return ConflictResolutionResult.builder()
                .mergeRequestId(context.mr().getId())
                .sourceCommitId(context.source().getHeadCommitId())
                .targetCommitId(context.target().getHeadCommitId())
                .resolved(unresolvedConflictIds.isEmpty())
                .requestedConflictCount(options.size())
                .appliedConflictCount(resolvedConflictIds.size())
                .resolvedConflictIds(resolvedConflictIds)
                .remainingConflictCount(remainingConflictCount)
                .supplementalCommitId(null)
                .equivalentResult(true)
                .resolutionCheckRunId(resolutionCheckRun.getId())
                .resolutionActivityLogId(resolutionLog.getId())
                .requiresRecheck(resolvedEquivalentResult.getRequiresRecheck())
                .requiresBranchUpdate(resolvedEquivalentResult.getRequiresBranchUpdate())
                .recheckRecommended(shouldRecommendRecheck(request, resolvedEquivalentResult))
                .blockingReasons(resolvedEquivalentResult.getBlockingReasons())
                .unresolvedConflictIds(unresolvedConflictIds)
                .summary(resolvedEquivalentResult.getSummary())
                .suggestedAction(resolvedEquivalentResult.getSuggestedAction())
                .latestMergeCheck(resolvedEquivalentResult)
                .metadata(metadata)
                .build();
    }

    @Override
    @Transactional
    public ConflictResolutionResult resolveContentConflict(Long mergeRequestId,
                                                           ContentConflictResolveRequest request,
                                                           Long currentUserId) {
        MergeCheckContext context = resolveMergeCheckContext(mergeRequestId, currentUserId, true);
        MergeCheckResult latestResult = latestMergeCheck(mergeRequestId, currentUserId);
        latestResult = applyHeadDriftState(
                latestResult,
                context.source().getHeadCommitId(),
                context.target().getHeadCommitId(),
                true
        );
        assertLatestMergeCheckUsable(latestResult);

        ConflictDetail conflict = requireContentConflict(request == null ? null : request.getConflictId(), indexLatestConflictsById(latestResult));
        String resolvedContent = normalizeResolvedContent(request);
        validateResolvedContent(conflict, resolvedContent);

        List<Map<String, Object>> optionTrace = List.of(buildManualContentConflictTrace(conflict));
        recordConflictResolutionActivity(
                context,
                currentUserId,
                MERGE_CONFLICT_RESOLVE_START_ACTION,
                "Manual content conflict resolution started",
                buildConflictResolutionTraceDetails(
                        context.mr().getId(),
                        optionTrace,
                        currentUserId,
                        null,
                        latestResult
                )
        );

        Long supplementalCommitId = createManualContentResolutionCommit(context, conflict, resolvedContent, currentUserId);
        context.source().setHeadCommitId(supplementalCommitId);
        context.mr().setSourceHeadCommitId(supplementalCommitId);
        ProjectCheckRun resolutionCheckRun = projectCheckRunRepository.save(ProjectCheckRun.builder()
                .repositoryId(context.repo().getId())
                .commitId(supplementalCommitId)
                .mergeRequestId(context.mr().getId())
                .checkType(MERGE_CONFLICT_RESOLVE_TYPE)
                .checkStatus("success")
                .summary(truncate("Manual content conflict resolution committed for " + resolveConflictPath(conflict), 500))
                .startedAt(java.time.LocalDateTime.now())
                .finishedAt(java.time.LocalDateTime.now())
                .build());

        MergeCheckResult recheckedResult = recheckMerge(mergeRequestId, currentUserId);
        List<String> unresolvedConflictIds = recheckedResult.getConflicts() == null ? List.of()
                : recheckedResult.getConflicts().stream()
                .filter(Objects::nonNull)
                .map(ConflictDetail::getConflictId)
                .filter(Objects::nonNull)
                .toList();
        List<String> resolvedConflictIds = List.of(conflict.getConflictId());
        int remainingConflictCount = unresolvedConflictIds.size();

        Map<String, Object> resolutionLogDetails = buildConflictResolutionTraceDetails(
                context.mr().getId(),
                optionTrace,
                currentUserId,
                supplementalCommitId,
                recheckedResult
        );
        resolutionLogDetails.put("resolutionMode", "SUPPLEMENTAL_COMMIT");
        ProjectActivityLog resolutionLog = projectActivityLogRepository.save(ProjectActivityLog.builder()
                .projectId(context.repo().getProjectId())
                .operatorId(currentUserId)
                .action(MERGE_CONFLICT_RESOLVE_ACTION)
                .targetType("merge_request")
                .targetId(context.mr().getId())
                .commitId(supplementalCommitId)
                .branchId(context.source().getId())
                .mergeRequestId(context.mr().getId())
                .checkRunId(resolutionCheckRun.getId())
                .content(truncate(recheckedResult.getSummary(), 255))
                .details(writeJson(resolutionLogDetails))
                .build());

        recordConflictResolutionActivity(
                context,
                currentUserId,
                MERGE_CONFLICT_RESOLVE_APPLY_ACTION,
                "Manual content conflict resolution committed",
                buildConflictResolutionTraceDetails(
                        context.mr().getId(),
                        optionTrace,
                        currentUserId,
                        supplementalCommitId,
                        recheckedResult
                )
        );
        recordConflictResolutionActivity(
                context,
                currentUserId,
                MERGE_CONFLICT_RESOLVE_RECHECK_ACTION,
                truncate(recheckedResult.getSummary(), 255),
                buildConflictResolutionTraceDetails(
                        context.mr().getId(),
                        optionTrace,
                        currentUserId,
                        supplementalCommitId,
                        recheckedResult
                )
        );
        if (!Boolean.TRUE.equals(recheckedResult.getMergeable())) {
            recordConflictResolutionActivity(
                    context,
                    currentUserId,
                    MERGE_CONFLICT_RESOLVE_FAIL_ACTION,
                    truncate(recheckedResult.getSummary(), 255),
                    buildConflictResolutionTraceDetails(
                            context.mr().getId(),
                            optionTrace,
                            currentUserId,
                            supplementalCommitId,
                            recheckedResult
                    )
            );
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("resolutionMode", "SUPPLEMENTAL_COMMIT");
        metadata.put("resolvedConflictId", conflict.getConflictId());
        metadata.put("resolvedConflictIds", resolvedConflictIds);
        metadata.put("resolvedPath", resolveConflictPath(conflict));
        metadata.put("remainingConflictCount", remainingConflictCount);
        metadata.put("requiresRecheck", recheckedResult.getRequiresRecheck());
        metadata.put("requiresBranchUpdate", recheckedResult.getRequiresBranchUpdate());
        metadata.put("blockingReasons", recheckedResult.getBlockingReasons());
        metadata.put("unresolvedConflictIds", unresolvedConflictIds);
        metadata.put("recheckRecommended", shouldRecommendRecheck(null, recheckedResult));
        return ConflictResolutionResult.builder()
                .mergeRequestId(context.mr().getId())
                .sourceCommitId(recheckedResult.getSourceCommitId())
                .targetCommitId(recheckedResult.getTargetCommitId())
                .resolved(Boolean.TRUE.equals(recheckedResult.getMergeable()))
                .requestedConflictCount(1)
                .appliedConflictCount(1)
                .resolvedConflictIds(resolvedConflictIds)
                .remainingConflictCount(remainingConflictCount)
                .supplementalCommitId(supplementalCommitId)
                .equivalentResult(false)
                .resolutionCheckRunId(resolutionCheckRun.getId())
                .resolutionActivityLogId(resolutionLog.getId())
                .requiresRecheck(recheckedResult.getRequiresRecheck())
                .requiresBranchUpdate(recheckedResult.getRequiresBranchUpdate())
                .recheckRecommended(shouldRecommendRecheck(null, recheckedResult))
                .blockingReasons(recheckedResult.getBlockingReasons())
                .unresolvedConflictIds(unresolvedConflictIds)
                .summary(recheckedResult.getSummary())
                .suggestedAction(recheckedResult.getSuggestedAction())
                .latestMergeCheck(recheckedResult)
                .metadata(metadata)
                .build();
    }

    @Override
    public MergeCheckResult preMergeCheck(Long mergeRequestId, Long currentUserId) {
        MergeCheckContext context = resolveMergeCheckContext(mergeRequestId, currentUserId, false);
        MergeExecutionState executionState = loadMergeExecutionState(context);
        MergeCheckResult result = buildMergeCheckResult(context, executionState);
        StructuredResolutionState structuredResolutionState = loadLatestStructuredResolutionState(context.mr().getId());
        if (isStructuredResolutionApplicable(structuredResolutionState, result)) {
            Map<String, ConflictDetail> conflictsById = indexLatestConflictsById(result);
            result = applyStructuredResolutionPlan(
                    context,
                    executionState,
                    result,
                    structuredResolutionState.options(),
                    conflictsById
            ).mergeCheckResult();
        }
        return applyPreMergeGates(result, context.mr(), context.source(), context.target());
    }

    @Override
    @Transactional
    public ProjectMergeRequestVO merge(Long mergeRequestId, Long currentUserId) {
        ProjectMergeRequest mr = projectMergeRequestRepository.findById(mergeRequestId)
                .orElseThrow(() -> new BusinessException("合并请求不存在"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(mr.getRepositoryId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        projectPermissionService.assertProjectManageMembers(repo.getProjectId(), currentUserId);
        if (!"open".equalsIgnoreCase(mr.getStatus())) {
            throw new BusinessException("合并请求不是可合并状态");
        }
        ProjectBranch source = projectBranchRepository.findById(mr.getSourceBranchId())
                .orElseThrow(() -> new BusinessException("源分支不存在"));
        ProjectBranch target = projectBranchRepository.findById(mr.getTargetBranchId())
                .orElseThrow(() -> new BusinessException("目标分支不存在"));
        assertBranchBelongsToRepository(source, repo.getId(), "源分支");
        assertBranchBelongsToRepository(target, repo.getId(), "目标分支");
        mr = syncMergeRequestHeads(mr, source, target);

        ProjectCommit sourceHead = source.getHeadCommitId() == null ? null :
                projectCommitRepository.findById(source.getHeadCommitId()).orElse(null);
        if (sourceHead == null) {
            throw new BusinessException("源分支没有可合并提交");
        }
        ProjectCommit targetHead = target.getHeadCommitId() == null ? null :
                projectCommitRepository.findById(target.getHeadCommitId()).orElse(null);
        if (targetHead == null) {
            throw new BusinessException("目标分支没有可合并提交");
        }

        ProjectCommit mergeBase = resolveMergeBase(sourceHead, targetHead);
        Map<String, ProjectSnapshotItem> baseSnapshot = loadSnapshotMapByCommitId(mergeBase == null ? null : mergeBase.getId());
        Map<String, ProjectSnapshotItem> sourceSnapshot = loadSnapshotMapByCommitId(sourceHead.getId());
        Map<String, ProjectSnapshotItem> targetSnapshot = loadSnapshotMapByCommitId(targetHead.getId());
        MergeExecutionState executionState = new MergeExecutionState(
                sourceHead,
                targetHead,
                mergeBase,
                baseSnapshot,
                sourceSnapshot,
                targetSnapshot
        );
        MergeCheckResult currentMergeCheck = buildMergeCheckResult(new MergeCheckContext(mr, repo, source, target), executionState);
        StructuredResolutionState structuredResolutionState = loadLatestStructuredResolutionState(mr.getId());
        Map<String, ProjectSnapshotItem> effectiveSourceSnapshot = sourceSnapshot;
        Map<String, ProjectSnapshotItem> effectiveTargetSnapshot = targetSnapshot;
        MergeCheckResult effectivePreMergeResult = currentMergeCheck;
        if (isStructuredResolutionApplicable(structuredResolutionState, currentMergeCheck)) {
            Map<String, ConflictDetail> conflictsById = indexLatestConflictsById(currentMergeCheck);
            AppliedStructuredResolution appliedResolution = applyStructuredResolutionPlan(
                    new MergeCheckContext(mr, repo, source, target),
                    executionState,
                    currentMergeCheck,
                    structuredResolutionState.options(),
                    conflictsById
            );
            effectivePreMergeResult = appliedResolution.mergeCheckResult();
            effectiveSourceSnapshot = appliedResolution.effectiveSourceSnapshot();
            effectiveTargetSnapshot = appliedResolution.effectiveTargetSnapshot();
        }
        effectivePreMergeResult = applyPreMergeGates(effectivePreMergeResult, mr, source, target);
        if (!Boolean.TRUE.equals(effectivePreMergeResult.getMergeable())) {
            throw new BusinessException(resolvePreMergeBlockMessage(effectivePreMergeResult));
        }
        ThreeWayMergePlan mergePlan = buildThreeWayMergePlan(baseSnapshot, effectiveSourceSnapshot, effectiveTargetSnapshot);
        if (!mergePlan.conflictPaths().isEmpty()) {
            throw new BusinessException(buildMergeConflictMessage(mergePlan.conflictPaths()));
        }

        Long nextNo = projectCommitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(target.getRepositoryId(), target.getId())
                .map(ProjectCommit::getCommitNo).orElse(0L) + 1L;
        ProjectCommit mergeCommit = projectCommitRepository.save(ProjectCommit.builder()
                .repositoryId(target.getRepositoryId())
                .branchId(target.getId())
                .commitNo(nextNo)
                .displaySha(UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .message("merge branch " + source.getName() + " into " + target.getName())
                .commitType("merge")
                .operatorId(currentUserId)
                .baseCommitId(mergeBase == null ? null : mergeBase.getId())
                .isMergeCommit(true)
                .build());

        if (target.getHeadCommitId() != null) {
            projectCommitParentRepository.save(ProjectCommitParent.builder()
                    .commitId(mergeCommit.getId())
                    .parentCommitId(target.getHeadCommitId())
                    .parentOrder(1)
                    .build());
        }
        projectCommitParentRepository.save(ProjectCommitParent.builder()
                .commitId(mergeCommit.getId())
                .parentCommitId(sourceHead.getId())
                .parentOrder(2)
                .build());

        ProjectSnapshot snapshot = projectSnapshotRepository.save(ProjectSnapshot.builder()
                .repositoryId(target.getRepositoryId())
                .commitId(mergeCommit.getId())
                .manifestHash(UUID.randomUUID().toString().replace("-", ""))
                .fileCount(0)
                .build());

        Map<String, ProjectSnapshotItem> mergedSnapshot = cloneSnapshotMap(effectiveTargetSnapshot);
        for (ResolvedMergeChange change : mergePlan.acceptedChanges()) {
            if ("DELETE".equals(change.changeType())) {
                applyMergedDeleteChange(mergeCommit, currentUserId, mergedSnapshot, change);
            } else {
                applyMergedFileChange(repo.getProjectId(), repo, mergeCommit, currentUserId, mergedSnapshot, change);
            }
        }

        for (ProjectSnapshotItem item : mergedSnapshot.values()) {
            item.setSnapshotId(snapshot.getId());
            projectSnapshotItemRepository.save(item);
        }
        snapshot.setFileCount(mergedSnapshot.size());
        projectSnapshotRepository.save(snapshot);

        mergeCommit.setSnapshotId(snapshot.getId());
        projectCommitRepository.save(mergeCommit);

        target.setHeadCommitId(mergeCommit.getId());
        projectBranchRepository.save(target);

        if (Objects.equals(repo.getDefaultBranchId(), target.getId())) {
            repo.setHeadCommitId(mergeCommit.getId());
            projectCodeRepositoryRepository.save(repo);
        }

        mr.setStatus("merged");
        mr.setSourceHeadCommitId(sourceHead.getId());
        mr.setTargetHeadCommitId(mergeCommit.getId());
        mr.setMergedBy(currentUserId);
        mr.setMergedAt(java.time.LocalDateTime.now());
        projectMergeRequestRepository.save(mr);
        recordMergeRequestActivity(
                repo.getProjectId(),
                currentUserId,
                MERGE_REQUEST_MERGE_ACTION,
                mr,
                source,
                mergeCommit.getId(),
                buildMergeRequestActivitySummary("Merged merge request", source, target),
                buildMergeRequestActivityDetails(
                        mr.getTitle(),
                        mr.getStatus(),
                        source.getId(),
                        target.getId(),
                        sourceHead.getId(),
                        targetHead.getId(),
                        null,
                        null,
                        null,
                        mergeCommit.getId(),
                        mergeBase == null ? null : mergeBase.getId()
                )
        );

        return toVO(mr);
    }

    private MergeCheckContext resolveMergeCheckContext(Long mergeRequestId, Long currentUserId, boolean requireManagePermission) {
        ProjectMergeRequest mr = projectMergeRequestRepository.findById(mergeRequestId)
                .orElseThrow(() -> new BusinessException("merge request not found"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(mr.getRepositoryId())
                .orElseThrow(() -> new BusinessException("project repository not found"));
        if (requireManagePermission) {
            projectPermissionService.assertProjectManageMembers(repo.getProjectId(), currentUserId);
        } else {
            projectPermissionService.assertProjectReadable(repo.getProjectId(), currentUserId);
        }
        ProjectBranch source = projectBranchRepository.findById(mr.getSourceBranchId())
                .orElseThrow(() -> new BusinessException("source branch not found"));
        ProjectBranch target = projectBranchRepository.findById(mr.getTargetBranchId())
                .orElseThrow(() -> new BusinessException("target branch not found"));
        assertBranchBelongsToRepository(source, repo.getId(), "source branch");
        assertBranchBelongsToRepository(target, repo.getId(), "target branch");
        mr = syncMergeRequestHeads(mr, source, target);
        return new MergeCheckContext(mr, repo, source, target);
    }

    private MergeCheckResult runMergeCheck(MergeCheckContext context, Long currentUserId, boolean persistResult) {
        MergeExecutionState executionState = loadMergeExecutionState(context);
        MergeCheckResult result = buildMergeCheckResult(context, executionState);
        if (persistResult) {
            persistMergeCheckResult(context, result, currentUserId);
        }
        return result;
    }

    private MergeCheckResult applyStructuredResolutionIfPossible(MergeCheckContext context,
                                                                 MergeExecutionState executionState,
                                                                 MergeCheckResult currentResult) {
        StructuredResolutionState state = loadLatestStructuredResolutionState(context.mr().getId());
        if (!isStructuredResolutionApplicable(state, currentResult)) {
            return currentResult;
        }
        Map<String, ConflictDetail> conflictsById = indexLatestConflictsById(currentResult);
        return applyStructuredResolutionPlan(
                context,
                executionState,
                currentResult,
                state.options(),
                conflictsById
        ).mergeCheckResult();
    }

    private MergeExecutionState loadMergeExecutionState(MergeCheckContext context) {
        ProjectCommit sourceHead = context.source().getHeadCommitId() == null ? null :
                projectCommitRepository.findById(context.source().getHeadCommitId()).orElse(null);
        if (sourceHead == null) {
            throw new BusinessException("source branch has no mergeable commit");
        }
        ProjectCommit targetHead = context.target().getHeadCommitId() == null ? null :
                projectCommitRepository.findById(context.target().getHeadCommitId()).orElse(null);
        if (targetHead == null) {
            throw new BusinessException("target branch has no mergeable commit");
        }

        ProjectCommit mergeBase = resolveMergeBase(sourceHead, targetHead);
        Map<String, ProjectSnapshotItem> baseSnapshot = loadSnapshotMapByCommitId(mergeBase == null ? null : mergeBase.getId());
        Map<String, ProjectSnapshotItem> sourceSnapshot = loadSnapshotMapByCommitId(sourceHead.getId());
        Map<String, ProjectSnapshotItem> targetSnapshot = loadSnapshotMapByCommitId(targetHead.getId());
        return new MergeExecutionState(sourceHead, targetHead, mergeBase, baseSnapshot, sourceSnapshot, targetSnapshot);
    }

    private MergeCheckResult buildMergeCheckResult(MergeCheckContext context, MergeExecutionState executionState) {
        Map<Long, Boolean> binaryCache = new HashMap<>();
        MergeCheckResult result = ProjectMergeDiffSupport.buildMergeCheck(
                context.mr().getId(),
                context.repo().getId(),
                context.source().getId(),
                context.target().getId(),
                executionState.mergeBase() == null ? null : executionState.mergeBase().getId(),
                executionState.sourceHead().getId(),
                executionState.targetHead().getId(),
                executionState.baseSnapshot(),
                executionState.sourceSnapshot(),
                executionState.targetSnapshot(),
                blobId -> resolveBinaryFlagByBlobId(binaryCache, blobId)
        );
        return applyStaleBranchRequirement(result, executionState.sourceHead().getId(), executionState.targetHead().getId());
    }

    private MergeCheckResult applyStaleBranchRequirement(MergeCheckResult result, Long sourceCommitId, Long targetCommitId) {
        if (result == null) {
            return null;
        }
        ensureMutableCollections(result);
        boolean stale = sourceCommitId != null && targetCommitId != null && !isAncestorCommit(sourceCommitId, targetCommitId);
        if (stale) {
            result.setRequiresBranchUpdate(true);
            addBlockingReason(result, "BRANCH_UPDATE_REQUIRED");
            if (!hasConflictType(result, ConflictType.STALE_BRANCH)) {
                result.getConflicts().add(ConflictDetail.builder()
                        .conflictId("stale-" + sourceCommitId + "-" + targetCommitId)
                        .conflictType(ConflictType.STALE_BRANCH)
                        .path(null)
                        .binaryFile(false)
                        .baseCommitId(result.getBaseCommitId())
                        .sourceCommitId(sourceCommitId)
                        .targetCommitId(targetCommitId)
                        .summary("Source branch is behind latest target branch head.")
                        .suggestedAction("Sync source branch with target branch, then re-run merge check.")
                        .severity("WARNING")
                        .metadata(buildNonFileConflictMetadata(ConflictType.STALE_BRANCH, true, false))
                        .build());
            }
        } else if (result.getRequiresBranchUpdate() == null) {
            result.setRequiresBranchUpdate(false);
        }
        finalizeMergeability(result);
        return result;
    }

    private MergeCheckResult applyHeadDriftState(MergeCheckResult result,
                                                 Long currentSourceCommitId,
                                                 Long currentTargetCommitId,
                                                 boolean includeBranchSyncCheck) {
        if (result == null) {
            return null;
        }
        ensureMutableCollections(result);
        boolean sourceDrift = !Objects.equals(result.getSourceCommitId(), currentSourceCommitId);
        boolean targetDrift = !Objects.equals(result.getTargetCommitId(), currentTargetCommitId);
        if (sourceDrift || targetDrift) {
            result.setRequiresRecheck(true);
            addBlockingReason(result, "RECHECK_REQUIRED");
            if (!hasConflictType(result, ConflictType.STALE_BRANCH)) {
                result.getConflicts().add(ConflictDetail.builder()
                        .conflictId("recheck-" + currentSourceCommitId + "-" + currentTargetCommitId)
                        .conflictType(ConflictType.STALE_BRANCH)
                        .path(null)
                        .binaryFile(false)
                        .baseCommitId(result.getBaseCommitId())
                        .sourceCommitId(currentSourceCommitId)
                        .targetCommitId(currentTargetCommitId)
                        .summary("Branch heads changed after last merge check.")
                        .suggestedAction("Re-run merge check on latest source and target branch heads.")
                        .severity("WARNING")
                        .metadata(buildNonFileConflictMetadata(ConflictType.STALE_BRANCH, false, true))
                        .build());
            }
        } else if (result.getRequiresRecheck() == null) {
            result.setRequiresRecheck(false);
        }
        if (includeBranchSyncCheck) {
            applyStaleBranchRequirement(result, currentSourceCommitId, currentTargetCommitId);
        }
        finalizeMergeability(result);
        return result;
    }

    private MergeCheckResult applyPreMergeGates(MergeCheckResult result,
                                                ProjectMergeRequest mr,
                                                ProjectBranch source,
                                                ProjectBranch target) {
        if (result == null) {
            return null;
        }
        ensureMutableCollections(result);

        if (Boolean.TRUE.equals(target.getProtectedFlag())) {
            boolean approved = projectReviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(mr.getId())
                    .stream().anyMatch(item -> "approve".equalsIgnoreCase(item.getReviewResult()));
            if (!approved) {
                addBlockingReason(result, "MISSING_REQUIRED_REVIEW");
            }
            boolean hasFailedCheck = resolveEffectiveChecks(mr.getId(), source.getHeadCommitId()).stream()
                    .filter(item -> !MERGE_CHECK_TYPE.equalsIgnoreCase(normalizeCheckType(item.getCheckType())))
                    .anyMatch(item -> "failed".equalsIgnoreCase(item.getCheckStatus()));
            if (hasFailedCheck) {
                addBlockingReason(result, "FAILED_CHECK_RUN");
            }
        }
        finalizeMergeability(result);
        if (!Boolean.TRUE.equals(result.getMergeable()) && result.getRequiresBranchUpdate() == null) {
            result.setRequiresBranchUpdate(false);
        }
        return result;
    }

    private void persistMergeCheckResult(MergeCheckContext context, MergeCheckResult result, Long currentUserId) {
        ProjectCheckRun checkRun = projectCheckRunRepository.save(ProjectCheckRun.builder()
                .repositoryId(context.repo().getId())
                .commitId(result.getSourceCommitId())
                .mergeRequestId(context.mr().getId())
                .checkType(MERGE_CHECK_TYPE)
                .checkStatus(Boolean.TRUE.equals(result.getMergeable()) ? "success" : "failed")
                .summary(truncate(result.getSummary(), 500))
                .startedAt(java.time.LocalDateTime.now())
                .finishedAt(java.time.LocalDateTime.now())
                .build());

        projectActivityLogRepository.save(ProjectActivityLog.builder()
                .projectId(context.repo().getProjectId())
                .operatorId(currentUserId)
                .action(MERGE_CHECK_ACTION)
                .targetType("merge_request")
                .targetId(context.mr().getId())
                .commitId(result.getSourceCommitId())
                .branchId(context.source().getId())
                .mergeRequestId(context.mr().getId())
                .checkRunId(checkRun.getId())
                .content(truncate(result.getSummary(), 255))
                .details(writeMergeCheckJson(result))
                .build());
    }

    private MergeCheckResult readMergeCheckFromActivity(ProjectActivityLog activityLog) {
        if (activityLog == null || activityLog.getDetails() == null || activityLog.getDetails().isBlank()) {
            return null;
        }
        try {
            MergeCheckResult result = objectMapper.readValue(activityLog.getDetails(), MergeCheckResult.class);
            ensureMutableCollections(result);
            finalizeMergeability(result);
            return result;
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private MergeCheckResult readMergeCheckFromConflictResolutionActivity(ProjectActivityLog activityLog) {
        if (activityLog == null || activityLog.getDetails() == null || activityLog.getDetails().isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(activityLog.getDetails());
            JsonNode latestMergeCheckNode = root.path("latestMergeCheck");
            if (latestMergeCheckNode.isMissingNode() || latestMergeCheckNode.isNull()) {
                return null;
            }
            MergeCheckResult result = objectMapper.treeToValue(latestMergeCheckNode, MergeCheckResult.class);
            ensureMutableCollections(result);
            finalizeMergeability(result);
            return result;
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private MergeCheckResult selectLatestEffectiveMergeCheck(ProjectActivityLog latestMergeCheckLog,
                                                             ProjectActivityLog latestResolutionLog) {
        MergeCheckResult mergeCheckResult = readMergeCheckFromActivity(latestMergeCheckLog);
        MergeCheckResult resolutionResult = readMergeCheckFromConflictResolutionActivity(latestResolutionLog);
        if (resolutionResult == null) {
            return mergeCheckResult;
        }
        if (mergeCheckResult == null) {
            return resolutionResult;
        }
        return isActivityLogNewer(latestResolutionLog, latestMergeCheckLog) ? resolutionResult : mergeCheckResult;
    }

    private boolean isActivityLogNewer(ProjectActivityLog left, ProjectActivityLog right) {
        if (left == null) {
            return false;
        }
        if (right == null) {
            return true;
        }
        int createdAtCompare = Comparator.nullsFirst(java.time.LocalDateTime::compareTo)
                .compare(left.getCreatedAt(), right.getCreatedAt());
        if (createdAtCompare != 0) {
            return createdAtCompare > 0;
        }
        return Comparator.nullsFirst(Long::compareTo).compare(left.getId(), right.getId()) > 0;
    }

    private String writeMergeCheckJson(MergeCheckResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private void recordMergeRequestActivity(Long projectId,
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

    private String writeJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private void recordConflictResolutionActivity(MergeCheckContext context,
                                                  Long operatorId,
                                                  String action,
                                                  String content,
                                                  Map<String, Object> details) {
        projectActivityLogRepository.save(ProjectActivityLog.builder()
                .projectId(context.repo().getProjectId())
                .operatorId(operatorId)
                .action(action)
                .targetType("merge_request")
                .targetId(context.mr().getId())
                .commitId(context.source().getHeadCommitId())
                .branchId(context.source().getId())
                .mergeRequestId(context.mr().getId())
                .content(truncate(content, 255))
                .details(writeJson(details))
                .build());
    }

    private List<Map<String, Object>> buildConflictResolutionOptionTrace(List<ConflictResolutionOption> options,
                                                                         Map<String, ConflictDetail> conflictsById) {
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
            item.put("path", conflict == null ? null : resolveConflictPath(conflict));
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

    private Map<String, Object> buildConflictResolutionTraceDetails(Long mergeRequestId,
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

    private Map<String, Object> buildConflictResolutionRecheckResult(MergeCheckResult recheckResult) {
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

    private String buildMergeRequestActivitySummary(String prefix, ProjectBranch source, ProjectBranch target) {
        StringBuilder builder = new StringBuilder(prefix);
        if (source != null || target != null) {
            builder.append(": ");
            builder.append(source == null ? "-" : source.getName());
            builder.append(" -> ");
            builder.append(target == null ? "-" : target.getName());
        }
        return builder.toString();
    }

    private String buildMergeRequestReviewSummary(String reviewResult, ProjectBranch source, ProjectBranch target) {
        String normalizedResult = reviewResult == null || reviewResult.isBlank() ? "comment" : reviewResult.trim().toLowerCase(Locale.ROOT);
        String prefix = switch (normalizedResult) {
            case "approve" -> "Reviewed merge request: approved";
            case "reject" -> "Reviewed merge request: rejected";
            default -> "Reviewed merge request: commented";
        };
        return buildMergeRequestActivitySummary(prefix, source, target);
    }

    private Map<String, Object> buildMergeRequestActivityDetails(String title,
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

    private Map<String, Object> buildMergeRequestActivityDetails(String title,
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

    private boolean isAncestorCommit(Long commitId, Long maybeAncestorCommitId) {
        if (commitId == null || maybeAncestorCommitId == null) {
            return false;
        }
        return collectAncestorDistances(commitId).containsKey(maybeAncestorCommitId);
    }

    private List<ConflictResolutionOption> resolveConflictOptions(ConflictResolutionRequest request) {
        if (request == null || request.getOptions() == null || request.getOptions().isEmpty()) {
            throw new BusinessException("conflict resolution options cannot be empty");
        }
        List<ConflictResolutionOption> resolvedOptions = request.getOptions().stream()
                .filter(Objects::nonNull)
                .toList();
        if (resolvedOptions.isEmpty()) {
            throw new BusinessException("conflict resolution options cannot be empty");
        }
        return resolvedOptions;
    }

    private Map<String, ConflictDetail> indexLatestConflictsById(MergeCheckResult latestResult) {
        if (latestResult == null || latestResult.getConflicts() == null) {
            return Map.of();
        }
        Map<String, ConflictDetail> index = new LinkedHashMap<>();
        for (ConflictDetail conflict : latestResult.getConflicts()) {
            if (conflict == null || conflict.getConflictId() == null || conflict.getConflictId().isBlank()) {
                continue;
            }
            index.putIfAbsent(conflict.getConflictId(), conflict);
        }
        return index;
    }

    private void validateConflictResolutionOptions(List<ConflictResolutionOption> options,
                                                   Map<String, ConflictDetail> conflictsById) {
        for (ConflictResolutionOption option : options) {
            if (option.getConflictId() == null || option.getConflictId().isBlank()) {
                throw new BusinessException("conflictId is required");
            }
            ConflictDetail conflict = conflictsById.get(option.getConflictId());
            if (conflict == null) {
                throw new BusinessException("conflictId does not belong to latest merge-check result: " + option.getConflictId());
            }
            if (!STRUCTURED_RESOLVABLE_CONFLICT_TYPES.contains(conflict.getConflictType())) {
                throw new BusinessException("conflict type is not supported by structured resolution: " + conflict.getConflictType());
            }
            validateResolutionStrategy(option, conflict.getConflictType());
        }
    }

    private void validateResolutionStrategy(ConflictResolutionOption option, ConflictType conflictType) {
        String strategy = option.normalizedStrategy();
        if (strategy == null) {
            throw new BusinessException("resolutionStrategy is required for conflictId: " + option.getConflictId());
        }
        Set<String> allowedStrategies = switch (conflictType) {
            case STALE_BRANCH -> Set.of("SYNC_SOURCE_WITH_TARGET");
            case DELETE_MODIFY_CONFLICT -> Set.of("KEEP_SOURCE", "KEEP_TARGET");
            case RENAME_CONFLICT, MOVE_CONFLICT -> Set.of("USE_SOURCE_PATH", "USE_TARGET_PATH", "SET_TARGET_PATH");
            case TARGET_PATH_OCCUPIED -> Set.of("KEEP_SOURCE", "KEEP_TARGET", "SET_TARGET_PATH");
            default -> Set.of();
        };
        if (!allowedStrategies.contains(strategy)) {
            throw new BusinessException("resolutionStrategy " + strategy + " is not allowed for conflict type " + conflictType);
        }
        if ("SET_TARGET_PATH".equals(strategy) && (option.getTargetPath() == null || option.getTargetPath().isBlank())) {
            throw new BusinessException("targetPath is required when resolutionStrategy is SET_TARGET_PATH");
        }
    }

    private void validateStructuredResolutionPaths(List<ConflictResolutionOption> options,
                                                   Map<String, ConflictDetail> conflictsById,
                                                   MergeExecutionState executionState) {
        if (options == null || options.isEmpty()) {
            return;
        }
        for (ConflictResolutionOption option : options) {
            if (!"SET_TARGET_PATH".equals(option.normalizedStrategy())) {
                continue;
            }
            ConflictDetail conflict = conflictsById.get(option.getConflictId());
            if (conflict == null) {
                continue;
            }
            validateStructuredResolutionTargetPath(option.getTargetPath(), conflict, executionState);
        }
    }

    private void validateStructuredResolutionTargetPath(String targetPath,
                                                        ConflictDetail conflict,
                                                        MergeExecutionState executionState) {
        String normalizedTargetPath = normalizePath(targetPath);
        if (normalizedTargetPath == null) {
            throw new BusinessException("targetPath is required for conflictId: " + (conflict == null ? null : conflict.getConflictId()));
        }
        String currentSourcePath = normalizePath(conflict == null ? null : conflict.getSourcePath());
        String currentTargetPath = normalizePath(conflict == null ? null : conflict.getTargetPath());
        boolean occupiedByOtherSourcePath = executionState.sourceSnapshot().containsKey(normalizedTargetPath)
                && !Objects.equals(normalizedTargetPath, currentSourcePath);
        boolean occupiedByOtherTargetPath = executionState.targetSnapshot().containsKey(normalizedTargetPath)
                && !Objects.equals(normalizedTargetPath, currentTargetPath);
        if (occupiedByOtherSourcePath || occupiedByOtherTargetPath) {
            throw new BusinessException("targetPath is already occupied on current branch heads: " + normalizedTargetPath);
        }
    }

    private StructuredResolutionState loadLatestStructuredResolutionState(Long mergeRequestId) {
        ProjectActivityLog latestResolutionLog = projectActivityLogRepository
                .findTopByMergeRequestIdAndActionOrderByCreatedAtDescIdDesc(mergeRequestId, MERGE_CONFLICT_RESOLVE_ACTION)
                .orElse(null);
        if (latestResolutionLog == null || latestResolutionLog.getDetails() == null || latestResolutionLog.getDetails().isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(latestResolutionLog.getDetails());
            String resolutionMode = textValue(root.get("resolutionMode"));
            if (!"EQUIVALENT_RESULT".equalsIgnoreCase(resolutionMode)) {
                return null;
            }
            JsonNode latestMergeCheckNode = root.path("latestMergeCheck");
            if (latestMergeCheckNode.isMissingNode() || latestMergeCheckNode.isNull()) {
                return null;
            }
            MergeCheckResult latestMergeCheck = objectMapper.treeToValue(latestMergeCheckNode, MergeCheckResult.class);
            ensureMutableCollections(latestMergeCheck);
            finalizeMergeability(latestMergeCheck);

            List<ConflictResolutionOption> options = new ArrayList<>();
            JsonNode conflictsNode = root.path("conflicts");
            if (conflictsNode.isArray()) {
                for (JsonNode item : conflictsNode) {
                    String conflictId = textValue(item.get("conflictId"));
                    String strategy = textValue(item.get("strategy"));
                    if (conflictId == null || strategy == null) {
                        continue;
                    }
                    options.add(ConflictResolutionOption.builder()
                            .conflictId(conflictId)
                            .resolutionStrategy(strategy)
                            .targetPath(textValue(item.get("targetPath")))
                            .note(textValue(item.get("note")))
                            .metadata(null)
                            .build());
                }
            }
            if (options.isEmpty()) {
                return null;
            }
            return new StructuredResolutionState(options, latestMergeCheck, resolutionMode, latestResolutionLog.getId());
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private boolean isStructuredResolutionApplicable(StructuredResolutionState state, MergeCheckResult currentResult) {
        if (state == null || currentResult == null || state.latestMergeCheck() == null) {
            return false;
        }
        return Objects.equals(state.latestMergeCheck().getSourceCommitId(), currentResult.getSourceCommitId())
                && Objects.equals(state.latestMergeCheck().getTargetCommitId(), currentResult.getTargetCommitId());
    }

    private AppliedStructuredResolution applyStructuredResolutionPlan(MergeCheckContext context,
                                                                     MergeExecutionState executionState,
                                                                     MergeCheckResult rawResult,
                                                                     List<ConflictResolutionOption> options,
                                                                     Map<String, ConflictDetail> conflictsById) {
        Map<String, ProjectSnapshotItem> effectiveSourceSnapshot = cloneSnapshotMap(executionState.sourceSnapshot());
        Map<String, ProjectSnapshotItem> effectiveTargetSnapshot = cloneSnapshotMap(executionState.targetSnapshot());
        Set<String> resolvedConflictIds = new LinkedHashSet<>();
        boolean staleBranchResolved = false;

        for (ConflictResolutionOption option : options) {
            if (option == null) {
                continue;
            }
            ConflictDetail conflict = conflictsById.get(option.getConflictId());
            if (conflict == null) {
                continue;
            }
            String strategy = option.normalizedStrategy();
            switch (conflict.getConflictType()) {
                case STALE_BRANCH -> {
                    staleBranchResolved = "SYNC_SOURCE_WITH_TARGET".equals(strategy);
                    resolvedConflictIds.add(conflict.getConflictId());
                }
                case DELETE_MODIFY_CONFLICT -> {
                    applyDeleteModifyResolution(effectiveSourceSnapshot, effectiveTargetSnapshot, executionState, conflict, strategy);
                    resolvedConflictIds.add(conflict.getConflictId());
                }
                case TARGET_PATH_OCCUPIED -> {
                    applyTargetPathOccupiedResolution(effectiveSourceSnapshot, effectiveTargetSnapshot, executionState, conflict, option);
                    resolvedConflictIds.add(conflict.getConflictId());
                }
                case RENAME_CONFLICT, MOVE_CONFLICT -> {
                    applyRelocationResolution(effectiveSourceSnapshot, effectiveTargetSnapshot, executionState, conflict, option);
                    resolvedConflictIds.add(conflict.getConflictId());
                }
                default -> {
                }
            }
        }

        Map<Long, Boolean> binaryCache = new HashMap<>();
        MergeCheckResult resolvedEquivalentResult = ProjectMergeDiffSupport.buildMergeCheck(
                context.mr().getId(),
                context.repo().getId(),
                context.source().getId(),
                context.target().getId(),
                executionState.mergeBase() == null ? null : executionState.mergeBase().getId(),
                executionState.sourceHead().getId(),
                executionState.targetHead().getId(),
                executionState.baseSnapshot(),
                effectiveSourceSnapshot,
                effectiveTargetSnapshot,
                blobId -> resolveBinaryFlagByBlobId(binaryCache, blobId)
        );
        if (staleBranchResolved) {
            clearStaleBranchRequirement(resolvedEquivalentResult);
        } else {
            resolvedEquivalentResult = applyStaleBranchRequirement(
                    resolvedEquivalentResult,
                    executionState.sourceHead().getId(),
                    executionState.targetHead().getId()
            );
        }
        Map<String, Object> metadata = resolvedEquivalentResult.getMetadata() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(resolvedEquivalentResult.getMetadata());
        metadata.put("structuredResolutionApplied", true);
        metadata.put("resolutionConflictIds", new ArrayList<>(resolvedConflictIds));
        resolvedEquivalentResult.setMetadata(metadata);
        finalizeMergeability(resolvedEquivalentResult);
        return new AppliedStructuredResolution(
                resolvedEquivalentResult,
                effectiveSourceSnapshot,
                effectiveTargetSnapshot,
                new ArrayList<>(options),
                new ArrayList<>(resolvedConflictIds)
        );
    }

    private void applyDeleteModifyResolution(Map<String, ProjectSnapshotItem> effectiveSourceSnapshot,
                                             Map<String, ProjectSnapshotItem> effectiveTargetSnapshot,
                                             MergeExecutionState executionState,
                                             ConflictDetail conflict,
                                             String strategy) {
        String path = conflictPathForSnapshot(conflict);
        if ("KEEP_TARGET".equals(strategy)) {
            restoreSnapshotPath(effectiveSourceSnapshot, executionState.baseSnapshot(), path);
            return;
        }
        restoreSnapshotPath(effectiveTargetSnapshot, executionState.baseSnapshot(), path);
    }

    private void applyTargetPathOccupiedResolution(Map<String, ProjectSnapshotItem> effectiveSourceSnapshot,
                                                   Map<String, ProjectSnapshotItem> effectiveTargetSnapshot,
                                                   MergeExecutionState executionState,
                                                   ConflictDetail conflict,
                                                   ConflictResolutionOption option) {
        String path = conflictPathForSnapshot(conflict);
        String strategy = option.normalizedStrategy();
        if ("KEEP_TARGET".equals(strategy)) {
            restoreSnapshotPath(effectiveSourceSnapshot, executionState.baseSnapshot(), path);
            return;
        }
        if ("KEEP_SOURCE".equals(strategy)) {
            restoreSnapshotPath(effectiveTargetSnapshot, executionState.baseSnapshot(), path);
            return;
        }

        String targetPath = normalizePath(option.getTargetPath());
        ProjectSnapshotItem sourceItem = resolveSourceSnapshotItem(executionState.sourceSnapshot(), conflict);
        restoreSnapshotPath(effectiveSourceSnapshot, executionState.baseSnapshot(), path);
        restoreSnapshotPath(effectiveTargetSnapshot, executionState.baseSnapshot(), targetPath);
        replaceSnapshotPath(effectiveSourceSnapshot, targetPath, sourceItem);
    }

    private void applyRelocationResolution(Map<String, ProjectSnapshotItem> effectiveSourceSnapshot,
                                           Map<String, ProjectSnapshotItem> effectiveTargetSnapshot,
                                           MergeExecutionState executionState,
                                           ConflictDetail conflict,
                                           ConflictResolutionOption option) {
        String sourcePath = normalizePath(conflict.getSourcePath());
        String targetPath = normalizePath(conflict.getTargetPath());
        String strategy = option.normalizedStrategy();
        if ("USE_TARGET_PATH".equals(strategy)) {
            restoreSnapshotPath(effectiveSourceSnapshot, executionState.baseSnapshot(), sourcePath);
            return;
        }
        if ("USE_SOURCE_PATH".equals(strategy)) {
            restoreSnapshotPath(effectiveTargetSnapshot, executionState.baseSnapshot(), targetPath);
            return;
        }

        String newTargetPath = normalizePath(option.getTargetPath());
        ProjectSnapshotItem sourceItem = resolveSourceSnapshotItem(executionState.sourceSnapshot(), conflict);
        restoreSnapshotPath(effectiveSourceSnapshot, executionState.baseSnapshot(), sourcePath);
        restoreSnapshotPath(effectiveTargetSnapshot, executionState.baseSnapshot(), targetPath);
        restoreSnapshotPath(effectiveTargetSnapshot, executionState.baseSnapshot(), newTargetPath);
        replaceSnapshotPath(effectiveSourceSnapshot, newTargetPath, sourceItem);
    }

    private void clearStaleBranchRequirement(MergeCheckResult result) {
        if (result == null) {
            return;
        }
        ensureMutableCollections(result);
        result.setRequiresBranchUpdate(false);
        removeBlockingReason(result, "BRANCH_UPDATE_REQUIRED");
        result.getConflicts().removeIf(item -> item != null && ConflictType.STALE_BRANCH.equals(item.getConflictType()));
        finalizeMergeability(result);
    }

    private void restoreSnapshotPath(Map<String, ProjectSnapshotItem> snapshot,
                                     Map<String, ProjectSnapshotItem> baseSnapshot,
                                     String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        replaceSnapshotPath(snapshot, path, baseSnapshot.get(path));
    }

    private void replaceSnapshotPath(Map<String, ProjectSnapshotItem> snapshot,
                                     String path,
                                     ProjectSnapshotItem item) {
        if (snapshot == null || path == null || path.isBlank()) {
            return;
        }
        if (item == null) {
            snapshot.remove(path);
            return;
        }
        ProjectSnapshotItem cloned = cloneSnapshotItem(item);
        cloned.setCanonicalPath(path);
        snapshot.put(path, cloned);
    }

    private ProjectSnapshotItem resolveSourceSnapshotItem(Map<String, ProjectSnapshotItem> sourceSnapshot,
                                                          ConflictDetail conflict) {
        return resolveSnapshotItem(
                sourceSnapshot,
                conflict == null ? null : conflict.getSourcePath(),
                conflict == null ? null : conflict.getNewPath(),
                conflict == null ? null : conflict.getOldPath(),
                conflict == null ? null : conflict.getBasePath()
        );
    }

    private String conflictPathForSnapshot(ConflictDetail conflict) {
        String basePath = normalizePath(conflict == null ? null : conflict.getBasePath());
        return basePath != null ? basePath : normalizePath(resolveConflictPath(conflict));
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return path.trim();
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText(null);
        return text == null || text.isBlank() ? null : text.trim();
    }

    private void assertLatestMergeCheckUsable(MergeCheckResult latestResult) {
        if (latestResult == null) {
            throw new BusinessException("latest merge-check result not found");
        }
        if (Boolean.TRUE.equals(latestResult.getRequiresRecheck())) {
            throw new BusinessException("merge check is outdated, please re-run merge check first");
        }
    }

    private ConflictDetail requireContentConflict(String conflictId, Map<String, ConflictDetail> conflictsById) {
        if (conflictId == null || conflictId.isBlank()) {
            throw new BusinessException("conflictId is required");
        }
        ConflictDetail conflict = conflictsById.get(conflictId);
        if (conflict == null) {
            throw new BusinessException("conflictId does not belong to latest merge-check result: " + conflictId);
        }
        if (!ConflictType.CONTENT_CONFLICT.equals(conflict.getConflictType())) {
            throw new BusinessException("conflict type is not supported by content editor: " + conflict.getConflictType());
        }
        return conflict;
    }

    private ConflictSnapshotView resolveConflictSnapshotView(ConflictDetail conflict) {
        ProjectSnapshotItem baseItem = resolveSnapshotItem(
                loadSnapshotMapByCommitId(conflict.getBaseCommitId()),
                conflict.getBasePath(),
                conflict.getOldPath(),
                conflict.getNewPath()
        );
        ProjectSnapshotItem sourceItem = resolveSnapshotItem(
                loadSnapshotMapByCommitId(conflict.getSourceCommitId()),
                conflict.getSourcePath(),
                conflict.getNewPath(),
                conflict.getOldPath()
        );
        ProjectSnapshotItem targetItem = resolveSnapshotItem(
                loadSnapshotMapByCommitId(conflict.getTargetCommitId()),
                conflict.getTargetPath(),
                conflict.getNewPath(),
                conflict.getOldPath()
        );
        ProjectBlob baseBlob = loadBlob(baseItem == null ? null : baseItem.getBlobId());
        ProjectBlob sourceBlob = loadBlob(sourceItem == null ? null : sourceItem.getBlobId());
        ProjectBlob targetBlob = loadBlob(targetItem == null ? null : targetItem.getBlobId());
        boolean binaryFile = Boolean.TRUE.equals(conflict.getBinaryFile())
                || isBinaryBlob(baseBlob)
                || isBinaryBlob(sourceBlob)
                || isBinaryBlob(targetBlob);
        String baseContent = binaryFile ? null : readBlobTextContent(baseBlob);
        String sourceContent = binaryFile ? null : readBlobTextContent(sourceBlob);
        String targetContent = binaryFile ? null : readBlobTextContent(targetBlob);
        return new ConflictSnapshotView(
                baseItem,
                sourceItem,
                targetItem,
                baseContent,
                sourceContent,
                targetContent,
                splitContentLines(baseContent),
                splitContentLines(sourceContent),
                splitContentLines(targetContent),
                binaryFile
        );
    }

    private Map<String, Object> buildContentConflictMetadata(ConflictDetail conflict,
                                                             MergeCheckResult latestResult,
                                                             ConflictSnapshotView snapshotView) {
        String path = resolveConflictPath(conflict);
        Boolean binaryFile = snapshotView == null ? conflict.getBinaryFile() : snapshotView.binaryFile();
        boolean onlineEditable = ConflictType.CONTENT_CONFLICT.equals(conflict.getConflictType())
                && !Boolean.TRUE.equals(binaryFile)
                && snapshotView != null
                && snapshotView.sourceItem() != null;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("path", path);
        metadata.put("fileName", extractFileName(path));
        metadata.put("basePath", conflict.getBasePath());
        metadata.put("sourcePath", conflict.getSourcePath());
        metadata.put("targetPath", conflict.getTargetPath());
        metadata.put("fileExtension", resolveFileExtension(path));
        metadata.put("language", inferEditorLanguage(path));
        metadata.put("onlineEditable", onlineEditable);
        metadata.put("allowOnlineEdit", onlineEditable);
        metadata.put("readOnly", !onlineEditable);
        metadata.put("binaryFile", binaryFile);
        metadata.put("hasPathChange", hasPathChange(conflict));
        metadata.put("baseContentHash", conflict.getBaseContentHash());
        metadata.put("sourceContentHash", conflict.getSourceContentHash());
        metadata.put("targetContentHash", conflict.getTargetContentHash());
        metadata.put("blockMode", "PREFIX_SUFFIX_SEGMENTS");
        metadata.put("mergeable", latestResult == null ? null : latestResult.getMergeable());
        metadata.put("requiresRecheck", latestResult == null ? null : latestResult.getRequiresRecheck());
        metadata.put("requiresBranchUpdate", latestResult == null ? null : latestResult.getRequiresBranchUpdate());
        metadata.put("blockingReasons", latestResult == null ? List.of() : latestResult.getBlockingReasons());
        return metadata;
    }

    private String normalizeResolvedContent(ContentConflictResolveRequest request) {
        if (request == null) {
            throw new BusinessException("content conflict resolution request is required");
        }
        if (request.getResolvedContent() == null) {
            throw new BusinessException("resolvedContent is required");
        }
        return request.getResolvedContent();
    }

    private void validateResolvedContent(ConflictDetail conflict, String resolvedContent) {
        if (resolvedContent == null) {
            throw new BusinessException("resolvedContent is required");
        }
        ConflictSnapshotView snapshotView = resolveConflictSnapshotView(conflict);
        if (Boolean.TRUE.equals(snapshotView.binaryFile())) {
            throw new BusinessException("binary content conflict is not supported for online editing");
        }
        if (snapshotView.sourceItem() == null) {
            throw new BusinessException("source content for conflict is missing on current source branch");
        }
        String path = resolveConflictPath(conflict);
        if (path == null || path.isBlank()) {
            throw new BusinessException("conflict path is missing");
        }
        try {
            resolvedContent.getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException("resolvedContent cannot be encoded as UTF-8");
        }
    }

    private Map<String, Object> buildManualContentConflictTrace(ConflictDetail conflict) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("conflictId", conflict.getConflictId());
        item.put("conflictType", Objects.toString(conflict.getConflictType(), null));
        item.put("strategy", CONTENT_CONFLICT_MANUAL_STRATEGY);
        item.put("path", resolveConflictPath(conflict));
        return item;
    }

    private Long createManualContentResolutionCommit(MergeCheckContext context,
                                                     ConflictDetail conflict,
                                                     String resolvedContent,
                                                     Long currentUserId) {
        ProjectBranch source = projectBranchRepository.findById(context.source().getId())
                .orElseThrow(() -> new BusinessException("source branch not found"));
        ProjectCommit sourceHead = source.getHeadCommitId() == null ? null
                : projectCommitRepository.findById(source.getHeadCommitId()).orElse(null);
        if (sourceHead == null) {
            throw new BusinessException("source branch has no mergeable commit");
        }

        Long nextNo = projectCommitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(source.getRepositoryId(), source.getId())
                .map(ProjectCommit::getCommitNo).orElse(0L) + 1L;
        String path = resolveConflictPath(conflict);
        ProjectCommit resolutionCommit = projectCommitRepository.save(ProjectCommit.builder()
                .repositoryId(source.getRepositoryId())
                .branchId(source.getId())
                .commitNo(nextNo)
                .displaySha(UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .message("resolve content conflict: " + path)
                .commitType("normal")
                .operatorId(currentUserId)
                .baseCommitId(conflict.getBaseCommitId())
                .isMergeCommit(false)
                .build());

        projectCommitParentRepository.save(ProjectCommitParent.builder()
                .commitId(resolutionCommit.getId())
                .parentCommitId(sourceHead.getId())
                .parentOrder(1)
                .build());

        ProjectSnapshot snapshot = projectSnapshotRepository.save(ProjectSnapshot.builder()
                .repositoryId(source.getRepositoryId())
                .commitId(resolutionCommit.getId())
                .manifestHash(UUID.randomUUID().toString().replace("-", ""))
                .fileCount(0)
                .build());

        Map<String, ProjectSnapshotItem> sourceSnapshot = loadSnapshotMapByCommitId(sourceHead.getId());
        ProjectSnapshotItem currentItem = resolveSnapshotItem(sourceSnapshot, conflict.getSourcePath(), path, conflict.getOldPath(), conflict.getNewPath());
        if (currentItem == null) {
            throw new BusinessException("source content for conflict is missing on current source branch");
        }
        applyResolvedContentChange(
                context.repo().getProjectId(),
                context.repo(),
                resolutionCommit,
                currentUserId,
                sourceSnapshot,
                path,
                currentItem,
                resolvedContent
        );

        for (ProjectSnapshotItem item : sourceSnapshot.values()) {
            item.setSnapshotId(snapshot.getId());
            projectSnapshotItemRepository.save(item);
        }
        snapshot.setFileCount(sourceSnapshot.size());
        projectSnapshotRepository.save(snapshot);

        resolutionCommit.setSnapshotId(snapshot.getId());
        projectCommitRepository.save(resolutionCommit);

        source.setHeadCommitId(resolutionCommit.getId());
        projectBranchRepository.save(source);
        if (Objects.equals(context.repo().getDefaultBranchId(), source.getId())) {
            context.repo().setHeadCommitId(resolutionCommit.getId());
            projectCodeRepositoryRepository.save(context.repo());
        }
        context.mr().setSourceHeadCommitId(resolutionCommit.getId());
        projectMergeRequestRepository.save(context.mr());
        return resolutionCommit.getId();
    }

    private void applyResolvedContentChange(Long projectId,
                                            ProjectCodeRepository repo,
                                            ProjectCommit resolutionCommit,
                                            Long currentUserId,
                                            Map<String, ProjectSnapshotItem> snapshot,
                                            String path,
                                            ProjectSnapshotItem currentItem,
                                            String resolvedContent) {
        ProjectBlob blob = projectRepoStorageSupport.saveTextContent(resolvedContent, extractFileName(path));
        ProjectFile file = resolveProjectFile(projectId, currentItem, currentItem, path, repo.getId(), blob);
        Long parentVersionId = currentItem.getProjectFileVersionId() != null
                ? currentItem.getProjectFileVersionId()
                : file.getLatestVersionId();
        int nextVersionSeq = (int) projectFileVersionRepository.countByFileId(file.getId()) + 1;
        ProjectFileVersion fileVersion = projectFileVersionRepository.save(ProjectFileVersion.builder()
                .fileId(file.getId())
                .repositoryId(repo.getId())
                .commitId(resolutionCommit.getId())
                .blobId(blob.getId())
                .version("v" + nextVersionSeq)
                .versionSeq(nextVersionSeq)
                .contentHash(blob.getSha256())
                .pathAtVersion(path)
                .changeType("MODIFY")
                .parentVersionId(parentVersionId)
                .serverPath(blob.getStoragePath())
                .fileSizeBytes(blob.getSizeBytes())
                .uploadedBy(currentUserId)
                .commitMessage(resolutionCommit.getMessage())
                .build());

        file.setRepositoryId(repo.getId());
        file.setFileName(extractFileName(path));
        file.setCanonicalPath(path);
        file.setFileKey(path);
        file.setFilePath(blob.getStoragePath());
        file.setFileSizeBytes(blob.getSizeBytes());
        file.setFileType(ProjectFileTypeSupport.resolve(path, file.getFileName()));
        file.setVersion(fileVersion.getVersion());
        file.setLatestBlobId(blob.getId());
        file.setLatestVersionId(fileVersion.getId());
        file.setLatestCommitId(resolutionCommit.getId());
        file.setIsLatest(true);
        file.setDeletedFlag(false);
        file.setContentHash(blob.getSha256());
        file.setLastModifiedAt(java.time.LocalDateTime.now());
        projectFileRepository.save(file);

        snapshot.put(path, ProjectSnapshotItem.builder()
                .projectFileId(file.getId())
                .projectFileVersionId(fileVersion.getId())
                .blobId(blob.getId())
                .canonicalPath(path)
                .contentHash(blob.getSha256())
                .build());

        projectCommitChangeRepository.save(ProjectCommitChange.builder()
                .commitId(resolutionCommit.getId())
                .projectFileId(file.getId())
                .oldBlobId(currentItem.getBlobId())
                .newBlobId(blob.getId())
                .oldPath(path)
                .newPath(path)
                .changeType("MODIFY")
                .diffSummaryJson("{\"path\":\"" + path + "\",\"changeType\":\"MODIFY\",\"contentConflictResolve\":true}")
                .build());
    }

    private ProjectSnapshotItem resolveSnapshotItem(Map<String, ProjectSnapshotItem> snapshot, String... candidatePaths) {
        if (snapshot == null || snapshot.isEmpty() || candidatePaths == null) {
            return null;
        }
        for (String candidatePath : candidatePaths) {
            if (candidatePath == null || candidatePath.isBlank()) {
                continue;
            }
            ProjectSnapshotItem item = snapshot.get(candidatePath);
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    private String resolveConflictPath(ConflictDetail conflict) {
        if (conflict == null) {
            return null;
        }
        if (conflict.getPath() != null && !conflict.getPath().isBlank()) {
            return conflict.getPath();
        }
        if (conflict.getSourcePath() != null && !conflict.getSourcePath().isBlank()) {
            return conflict.getSourcePath();
        }
        if (conflict.getTargetPath() != null && !conflict.getTargetPath().isBlank()) {
            return conflict.getTargetPath();
        }
        if (conflict.getNewPath() != null && !conflict.getNewPath().isBlank()) {
            return conflict.getNewPath();
        }
        if (conflict.getOldPath() != null && !conflict.getOldPath().isBlank()) {
            return conflict.getOldPath();
        }
        return conflict.getBasePath();
    }

    private ProjectBlob loadBlob(Long blobId) {
        if (blobId == null) {
            return null;
        }
        return projectBlobRepository.findById(blobId).orElse(null);
    }

    private boolean isBinaryBlob(ProjectBlob blob) {
        return blob != null && isBinaryMimeType(blob.getMimeType());
    }

    private String readBlobTextContent(ProjectBlob blob) {
        if (blob == null) {
            return null;
        }
        if (isBinaryBlob(blob)) {
            throw new BusinessException("binary content conflict is not supported for online editing");
        }
        try {
            Path storagePath = Path.of(blob.getStoragePath());
            if (!Files.exists(storagePath) || !Files.isReadable(storagePath)) {
                throw new BusinessException("conflict content blob is unavailable: " + blob.getId());
            }
            return Files.readString(storagePath, StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("failed to read conflict content blob: " + blob.getId());
        }
    }

    private List<String> splitContentLines(String content) {
        if (content == null) {
            return List.of();
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(normalized.split("\n", -1));
    }

    private List<ContentConflictBlock> buildContentConflictBlocks(String baseContent,
                                                                  String sourceContent,
                                                                  String targetContent) {
        List<String> baseLines = splitContentLines(baseContent);
        List<String> sourceLines = splitContentLines(sourceContent);
        List<String> targetLines = splitContentLines(targetContent);
        int prefix = 0;
        int prefixLimit = Math.min(baseLines.size(), Math.min(sourceLines.size(), targetLines.size()));
        while (prefix < prefixLimit
                && Objects.equals(baseLines.get(prefix), sourceLines.get(prefix))
                && Objects.equals(baseLines.get(prefix), targetLines.get(prefix))) {
            prefix++;
        }

        int suffix = 0;
        while (suffix < baseLines.size() - prefix
                && suffix < sourceLines.size() - prefix
                && suffix < targetLines.size() - prefix
                && Objects.equals(baseLines.get(baseLines.size() - 1 - suffix), sourceLines.get(sourceLines.size() - 1 - suffix))
                && Objects.equals(baseLines.get(baseLines.size() - 1 - suffix), targetLines.get(targetLines.size() - 1 - suffix))) {
            suffix++;
        }

        List<ContentConflictBlock> blocks = new ArrayList<>();
        if (prefix > 0) {
            blocks.add(buildContentConflictBlock("COMMON", 1, prefix, 1, prefix, 1, prefix, baseLines, sourceLines, targetLines));
        }

        int baseConflictCount = baseLines.size() - prefix - suffix;
        int sourceConflictCount = sourceLines.size() - prefix - suffix;
        int targetConflictCount = targetLines.size() - prefix - suffix;
        if (baseConflictCount > 0 || sourceConflictCount > 0 || targetConflictCount > 0 || blocks.isEmpty()) {
            blocks.add(buildContentConflictBlock(
                    "CONFLICT",
                    prefix + 1,
                    Math.max(baseConflictCount, 0),
                    prefix + 1,
                    Math.max(sourceConflictCount, 0),
                    prefix + 1,
                    Math.max(targetConflictCount, 0),
                    baseLines.subList(prefix, Math.max(prefix, baseLines.size() - suffix)),
                    sourceLines.subList(prefix, Math.max(prefix, sourceLines.size() - suffix)),
                    targetLines.subList(prefix, Math.max(prefix, targetLines.size() - suffix))
            ));
        }

        if (suffix > 0) {
            blocks.add(buildContentConflictBlock(
                    "COMMON",
                    baseLines.size() - suffix + 1,
                    suffix,
                    sourceLines.size() - suffix + 1,
                    suffix,
                    targetLines.size() - suffix + 1,
                    suffix,
                    baseLines.subList(baseLines.size() - suffix, baseLines.size()),
                    sourceLines.subList(sourceLines.size() - suffix, sourceLines.size()),
                    targetLines.subList(targetLines.size() - suffix, targetLines.size())
            ));
        }
        return blocks;
    }

    private ContentConflictBlock buildContentConflictBlock(String blockType,
                                                           int baseStartLine,
                                                           int baseLineCount,
                                                           int sourceStartLine,
                                                           int sourceLineCount,
                                                           int targetStartLine,
                                                           int targetLineCount,
                                                           List<String> baseLines,
                                                           List<String> sourceLines,
                                                           List<String> targetLines) {
        return ContentConflictBlock.builder()
                .blockType(blockType)
                .baseStartLine(baseStartLine)
                .baseLineCount(baseLineCount)
                .sourceStartLine(sourceStartLine)
                .sourceLineCount(sourceLineCount)
                .targetStartLine(targetStartLine)
                .targetLineCount(targetLineCount)
                .baseLines(new ArrayList<>(baseLines))
                .sourceLines(new ArrayList<>(sourceLines))
                .targetLines(new ArrayList<>(targetLines))
                .build();
    }

    private MergeCheckResult buildResolvedEquivalentResult(MergeCheckResult latestResult,
                                                           List<ConflictResolutionOption> options,
                                                           Map<String, ConflictDetail> conflictsById) {
        MergeCheckResult resolved = objectMapper.convertValue(latestResult, MergeCheckResult.class);
        ensureMutableCollections(resolved);
        Set<String> resolvedConflictIds = new HashSet<>();
        for (ConflictResolutionOption option : options) {
            resolvedConflictIds.add(option.getConflictId());
            ConflictDetail conflict = conflictsById.get(option.getConflictId());
            if (conflict != null && ConflictType.STALE_BRANCH.equals(conflict.getConflictType())) {
                resolved.setRequiresBranchUpdate(false);
                removeBlockingReason(resolved, "BRANCH_UPDATE_REQUIRED");
            }
        }
        resolved.setConflicts(resolved.getConflicts().stream()
                .filter(Objects::nonNull)
                .filter(conflict -> !resolvedConflictIds.contains(conflict.getConflictId()))
                .toList());
        if (resolved.getConflicts().isEmpty()) {
            removeBlockingReason(resolved, "UNRESOLVED_CONFLICTS");
        }
        Map<String, Object> metadata = resolved.getMetadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(resolved.getMetadata());
        metadata.put("structuredResolutionApplied", true);
        metadata.put("resolutionConflictIds", new ArrayList<>(resolvedConflictIds));
        resolved.setMetadata(metadata);
        finalizeMergeability(resolved);
        return resolved;
    }

    private boolean hasConflictType(MergeCheckResult result, ConflictType conflictType) {
        if (result == null || result.getConflicts() == null || conflictType == null) {
            return false;
        }
        return result.getConflicts().stream()
                .filter(Objects::nonNull)
                .anyMatch(conflict -> conflictType.equals(conflict.getConflictType()));
    }

    private void addBlockingReason(MergeCheckResult result, String reason) {
        if (result == null || reason == null || reason.isBlank()) {
            return;
        }
        ensureMutableCollections(result);
        if (!result.getBlockingReasons().contains(reason)) {
            result.getBlockingReasons().add(reason);
        }
    }

    private void removeBlockingReason(MergeCheckResult result, String reason) {
        if (result == null || reason == null || reason.isBlank()) {
            return;
        }
        ensureMutableCollections(result);
        result.getBlockingReasons().removeIf(reason::equals);
    }

    private void ensureMutableCollections(MergeCheckResult result) {
        if (result == null) {
            return;
        }
        if (result.getConflicts() == null) {
            result.setConflicts(new ArrayList<>());
        } else if (!(result.getConflicts() instanceof ArrayList<?>)) {
            result.setConflicts(new ArrayList<>(result.getConflicts()));
        }
        if (result.getBlockingReasons() == null) {
            result.setBlockingReasons(new ArrayList<>());
        } else if (!(result.getBlockingReasons() instanceof ArrayList<?>)) {
            result.setBlockingReasons(new ArrayList<>(result.getBlockingReasons()));
        }
        if (result.getRequiresBranchUpdate() == null) {
            result.setRequiresBranchUpdate(false);
        }
        if (result.getRequiresRecheck() == null) {
            result.setRequiresRecheck(false);
        }
    }

    private void finalizeMergeability(MergeCheckResult result) {
        if (result == null) {
            return;
        }
        ensureMutableCollections(result);
        normalizeMergeCheckConflictDetails(result);
        result.setTotalConflicts(result.getConflicts().size());
        boolean hasBlockingReasons = !result.getBlockingReasons().isEmpty();
        boolean hasConflicts = !result.getConflicts().isEmpty();
        boolean mergeable = !hasConflicts && !hasBlockingReasons;
        result.setMergeable(mergeable);
        if (mergeable) {
            result.setSeverity("INFO");
            result.setSummary("Merge check passed.");
            result.setSuggestedAction("Merge can proceed.");
            return;
        }

        result.setSeverity("ERROR");
        if (result.getRequiresRecheck()) {
            result.setSummary("Merge check is outdated because branch heads changed.");
            result.setSuggestedAction("Re-run merge check on latest branch heads.");
            return;
        }
        if (result.getRequiresBranchUpdate()) {
            result.setSummary("Source branch must be updated with latest target branch changes.");
            result.setSuggestedAction("Sync source branch with target branch and re-run merge check.");
            return;
        }
        if (result.getBlockingReasons().contains("MISSING_REQUIRED_REVIEW")) {
            result.setSummary("Protected branch requires at least one approval review.");
            result.setSuggestedAction("Request and obtain approval before merging.");
            return;
        }
        if (result.getBlockingReasons().contains("FAILED_CHECK_RUN")) {
            result.setSummary("There are failed check runs on the merge request.");
            result.setSuggestedAction("Fix failed checks and re-run check pipeline before merging.");
            return;
        }
        if (result.getSummary() == null || result.getSummary().isBlank()) {
            result.setSummary("Merge check found blocking conflicts.");
        }
        if (result.getSuggestedAction() == null || result.getSuggestedAction().isBlank()) {
            result.setSuggestedAction("Resolve conflicts before merging.");
        }
    }

    private void normalizeMergeCheckConflictDetails(MergeCheckResult result) {
        if (result == null || result.getConflicts() == null) {
            return;
        }
        for (ConflictDetail conflict : result.getConflicts()) {
            normalizeConflictDetail(conflict);
        }
    }

    private void normalizeConflictDetail(ConflictDetail conflict) {
        if (conflict == null) {
            return;
        }
        String path = normalizePath(conflict.getPath());
        if (path == null) {
            path = normalizePath(resolveConflictPath(conflict));
            conflict.setPath(path);
        }
        if ((conflict.getFileName() == null || conflict.getFileName().isBlank()) && path != null) {
            conflict.setFileName(extractFileName(path));
        }
        if (conflict.getBinaryFile() == null) {
            conflict.setBinaryFile(false);
        }
        Map<String, Object> metadata = conflict.getMetadata() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(conflict.getMetadata());
        metadata.putIfAbsent("path", path);
        metadata.putIfAbsent("fileName", path == null ? null : extractFileName(path));
        metadata.putIfAbsent("basePath", conflict.getBasePath());
        metadata.putIfAbsent("sourcePath", conflict.getSourcePath());
        metadata.putIfAbsent("targetPath", conflict.getTargetPath());
        metadata.putIfAbsent("binaryFile", conflict.getBinaryFile());
        metadata.putIfAbsent("hasPathChange", hasPathChange(conflict));
        metadata.putIfAbsent("fileExtension", resolveFileExtension(path));
        metadata.putIfAbsent("language", inferEditorLanguage(path));
        metadata.putIfAbsent("sourceChangeType", conflict.getSourceChangeType() == null ? null : conflict.getSourceChangeType().name());
        metadata.putIfAbsent("targetChangeType", conflict.getTargetChangeType() == null ? null : conflict.getTargetChangeType().name());
        metadata.putIfAbsent("baseContentHash", conflict.getBaseContentHash());
        metadata.putIfAbsent("sourceContentHash", conflict.getSourceContentHash());
        metadata.putIfAbsent("targetContentHash", conflict.getTargetContentHash());
        metadata.putIfAbsent("resolutionStrategies", resolutionStrategies(conflict.getConflictType()));
        metadata.putIfAbsent("onlineEditable", ConflictType.CONTENT_CONFLICT.equals(conflict.getConflictType())
                && !Boolean.TRUE.equals(conflict.getBinaryFile()));
        metadata.putIfAbsent("allowOnlineEdit", ConflictType.CONTENT_CONFLICT.equals(conflict.getConflictType())
                && !Boolean.TRUE.equals(conflict.getBinaryFile()));
        metadata.putIfAbsent("readOnly", !ConflictType.CONTENT_CONFLICT.equals(conflict.getConflictType())
                || Boolean.TRUE.equals(conflict.getBinaryFile()));
        conflict.setMetadata(metadata);
    }

    private Map<String, Object> buildNonFileConflictMetadata(ConflictType conflictType,
                                                             boolean requiresBranchUpdate,
                                                             boolean requiresRecheck) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nonFileConflict", true);
        metadata.put("binaryFile", false);
        metadata.put("requiresBranchUpdate", requiresBranchUpdate);
        metadata.put("requiresRecheck", requiresRecheck);
        metadata.put("onlineEditable", false);
        metadata.put("allowOnlineEdit", false);
        metadata.put("readOnly", true);
        metadata.put("resolutionStrategies", resolutionStrategies(conflictType));
        return metadata;
    }

    private boolean shouldRecommendRecheck(ConflictResolutionRequest request, MergeCheckResult result) {
        return (request != null && Boolean.TRUE.equals(request.getRecheckAfterResolve()))
                || (result != null && (Boolean.TRUE.equals(result.getRequiresRecheck())
                || Boolean.TRUE.equals(result.getRequiresBranchUpdate())));
    }

    private boolean hasPathChange(ConflictDetail conflict) {
        if (conflict == null) {
            return false;
        }
        String basePath = normalizePath(conflict.getBasePath());
        String sourcePath = normalizePath(conflict.getSourcePath());
        String targetPath = normalizePath(conflict.getTargetPath());
        return (basePath != null && sourcePath != null && !Objects.equals(basePath, sourcePath))
                || (basePath != null && targetPath != null && !Objects.equals(basePath, targetPath))
                || (sourcePath != null && targetPath != null && !Objects.equals(sourcePath, targetPath));
    }

    private String resolveFileExtension(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String fileName = extractFileName(path);
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String inferEditorLanguage(String path) {
        String extension = resolveFileExtension(path);
        if (extension == null) {
            return "plaintext";
        }
        return switch (extension) {
            case "java" -> "java";
            case "js", "mjs", "cjs" -> "javascript";
            case "ts", "tsx" -> "typescript";
            case "vue" -> "vue";
            case "json" -> "json";
            case "xml", "pom" -> "xml";
            case "html", "htm" -> "html";
            case "css" -> "css";
            case "scss" -> "scss";
            case "md", "markdown" -> "markdown";
            case "yml", "yaml" -> "yaml";
            case "sql" -> "sql";
            case "py" -> "python";
            case "go" -> "go";
            case "kt", "kts" -> "kotlin";
            case "sh", "bash" -> "shell";
            case "bat", "cmd" -> "bat";
            case "ps1" -> "powershell";
            default -> "plaintext";
        };
    }

    private List<String> resolutionStrategies(ConflictType type) {
        if (type == null) {
            return List.of();
        }
        return switch (type) {
            case CONTENT_CONFLICT -> List.of(CONTENT_CONFLICT_MANUAL_STRATEGY);
            case DELETE_MODIFY_CONFLICT -> List.of("KEEP_SOURCE", "KEEP_TARGET");
            case RENAME_CONFLICT, MOVE_CONFLICT -> List.of("USE_SOURCE_PATH", "USE_TARGET_PATH", "SET_TARGET_PATH");
            case TARGET_PATH_OCCUPIED -> List.of("KEEP_SOURCE", "KEEP_TARGET", "SET_TARGET_PATH");
            case STALE_BRANCH -> List.of("SYNC_SOURCE_WITH_TARGET");
            case MISSING_BASE -> List.of();
        };
    }

    private String resolvePreMergeBlockMessage(MergeCheckResult preMergeResult) {
        if (preMergeResult == null) {
            return "merge blocked by pre-merge check";
        }
        if (preMergeResult.getSuggestedAction() != null && !preMergeResult.getSuggestedAction().isBlank()) {
            return preMergeResult.getSuggestedAction();
        }
        if (preMergeResult.getSummary() != null && !preMergeResult.getSummary().isBlank()) {
            return preMergeResult.getSummary();
        }
        return "merge blocked by pre-merge check";
    }

    private String truncate(String text, int maxLength) {
        if (text == null || maxLength <= 0) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private ProjectCommit resolveMergeBase(ProjectCommit sourceHead, ProjectCommit targetHead) {
        if (sourceHead == null || targetHead == null) {
            return null;
        }
        Map<Long, Integer> targetDistances = collectAncestorDistances(targetHead.getId());
        Queue<CommitDistance> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.offer(new CommitDistance(sourceHead.getId(), 0));
        visited.add(sourceHead.getId());
        Long bestCommitId = null;
        int bestScore = Integer.MAX_VALUE;
        int bestTargetDistance = Integer.MAX_VALUE;
        while (!queue.isEmpty()) {
            CommitDistance current = queue.poll();
            Integer targetDistance = targetDistances.get(current.commitId());
            if (targetDistance != null) {
                int score = current.distance() + targetDistance;
                if (score < bestScore || (score == bestScore && targetDistance < bestTargetDistance)) {
                    bestCommitId = current.commitId();
                    bestScore = score;
                    bestTargetDistance = targetDistance;
                }
            }
            for (ProjectCommitParent parent : projectCommitParentRepository.findByCommitIdOrderByParentOrderAsc(current.commitId())) {
                if (visited.add(parent.getParentCommitId())) {
                    queue.offer(new CommitDistance(parent.getParentCommitId(), current.distance() + 1));
                }
            }
        }
        return bestCommitId == null ? null : projectCommitRepository.findById(bestCommitId).orElse(null);
    }

    private Map<Long, Integer> collectAncestorDistances(Long startCommitId) {
        Map<Long, Integer> distances = new HashMap<>();
        if (startCommitId == null) {
            return distances;
        }
        Queue<CommitDistance> queue = new ArrayDeque<>();
        queue.offer(new CommitDistance(startCommitId, 0));
        distances.put(startCommitId, 0);
        while (!queue.isEmpty()) {
            CommitDistance current = queue.poll();
            for (ProjectCommitParent parent : projectCommitParentRepository.findByCommitIdOrderByParentOrderAsc(current.commitId())) {
                int nextDistance = current.distance() + 1;
                Integer previous = distances.get(parent.getParentCommitId());
                if (previous == null || nextDistance < previous) {
                    distances.put(parent.getParentCommitId(), nextDistance);
                    queue.offer(new CommitDistance(parent.getParentCommitId(), nextDistance));
                }
            }
        }
        return distances;
    }

    private ThreeWayMergePlan buildThreeWayMergePlan(Map<String, ProjectSnapshotItem> baseSnapshot,
                                                     Map<String, ProjectSnapshotItem> sourceSnapshot,
                                                     Map<String, ProjectSnapshotItem> targetSnapshot) {
        Map<String, ProjectSnapshotItem> safeBase = baseSnapshot == null ? Map.of() : baseSnapshot;
        Map<String, ProjectSnapshotItem> safeSource = sourceSnapshot == null ? Map.of() : sourceSnapshot;
        Map<String, ProjectSnapshotItem> safeTarget = targetSnapshot == null ? Map.of() : targetSnapshot;
        Set<String> paths = new TreeSet<>();
        paths.addAll(safeBase.keySet());
        paths.addAll(safeSource.keySet());
        paths.addAll(safeTarget.keySet());

        List<String> conflictPaths = new ArrayList<>();
        List<ResolvedMergeChange> acceptedChanges = new ArrayList<>();
        for (String path : paths) {
            ProjectSnapshotItem baseItem = safeBase.get(path);
            ProjectSnapshotItem sourceItem = safeSource.get(path);
            ProjectSnapshotItem targetItem = safeTarget.get(path);

            if (sameSnapshotItem(sourceItem, targetItem)) {
                continue;
            }
            if (sameSnapshotItem(baseItem, targetItem)) {
                acceptedChanges.add(buildResolvedMergeChange(path, targetItem, sourceItem));
                continue;
            }
            if (sameSnapshotItem(baseItem, sourceItem)) {
                continue;
            }
            conflictPaths.add(path);
        }
        return new ThreeWayMergePlan(conflictPaths, acceptedChanges);
    }

    private ResolvedMergeChange buildResolvedMergeChange(String path,
                                                         ProjectSnapshotItem targetItem,
                                                         ProjectSnapshotItem mergedItem) {
        String changeType;
        if (mergedItem == null) {
            changeType = "DELETE";
        } else if (targetItem == null) {
            changeType = "ADD";
        } else {
            changeType = "MODIFY";
        }
        return new ResolvedMergeChange(path, changeType, targetItem, mergedItem);
    }

    private String buildMergeConflictMessage(List<String> conflictPaths) {
        if (conflictPaths == null || conflictPaths.isEmpty()) {
            return "三方合并检测到冲突，请先处理分支差异";
        }
        List<String> sample = conflictPaths.size() > 8 ? conflictPaths.subList(0, 8) : conflictPaths;
        String suffix = conflictPaths.size() > sample.size() ? " 等 " + conflictPaths.size() + " 个路径" : "";
        return "三方合并冲突，以下路径存在不同结果：" + String.join("，", sample) + suffix;
    }

    private void applyMergedDeleteChange(ProjectCommit mergeCommit,
                                         Long currentUserId,
                                         Map<String, ProjectSnapshotItem> mergedSnapshot,
                                         ResolvedMergeChange change) {
        ProjectSnapshotItem targetItem = change.targetItem();
        if (targetItem == null) {
            return;
        }
        mergedSnapshot.remove(change.path());
        ProjectFile file = projectFileRepository.findById(targetItem.getProjectFileId()).orElse(null);
        if (file != null) {
            ProjectFileVersion deleteVersion = projectFileVersionRepository.save(ProjectFileVersion.builder()
                    .fileId(file.getId())
                    .repositoryId(file.getRepositoryId())
                    .commitId(mergeCommit.getId())
                    .blobId(targetItem.getBlobId())
                    .version("v" + (projectFileVersionRepository.countByFileId(file.getId()) + 1))
                    .versionSeq((int) projectFileVersionRepository.countByFileId(file.getId()) + 1)
                    .contentHash(targetItem.getContentHash())
                    .pathAtVersion(change.path())
                    .changeType("DELETE")
                    .parentVersionId(targetItem.getProjectFileVersionId())
                    .serverPath(file.getFilePath())
                    .fileSizeBytes(file.getFileSizeBytes())
                    .uploadedBy(currentUserId)
                    .commitMessage(mergeCommit.getMessage())
                    .build());
            file.setDeletedFlag(true);
            file.setLatestCommitId(mergeCommit.getId());
            file.setLatestBlobId(null);
            file.setLatestVersionId(deleteVersion.getId());
            file.setLastModifiedAt(java.time.LocalDateTime.now());
            projectFileRepository.save(file);
        }
        projectCommitChangeRepository.save(ProjectCommitChange.builder()
                .commitId(mergeCommit.getId())
                .projectFileId(targetItem.getProjectFileId())
                .oldBlobId(targetItem.getBlobId())
                .newBlobId(null)
                .oldPath(change.path())
                .newPath(null)
                .changeType("DELETE")
                .diffSummaryJson("{\"path\":\"" + change.path() + "\",\"changeType\":\"DELETE\",\"merge\":true}")
                .build());
    }

    private void applyMergedFileChange(Long projectId,
                                       ProjectCodeRepository repo,
                                       ProjectCommit mergeCommit,
                                       Long currentUserId,
                                       Map<String, ProjectSnapshotItem> mergedSnapshot,
                                       ResolvedMergeChange change) {
        ProjectSnapshotItem mergedItem = change.mergedItem();
        if (mergedItem == null) {
            throw new BusinessException("合并结果文件内容不存在");
        }
        ProjectBlob blob = projectBlobRepository.findById(mergedItem.getBlobId())
                .orElseThrow(() -> new BusinessException("合并结果 blob 不存在"));
        ProjectFile file = resolveProjectFile(projectId, change.targetItem(), mergedItem, change.path(), repo.getId(), blob);
        Long parentVersionId = change.targetItem() != null && change.targetItem().getProjectFileVersionId() != null
                ? change.targetItem().getProjectFileVersionId()
                : file.getLatestVersionId();
        int nextVersionSeq = (int) projectFileVersionRepository.countByFileId(file.getId()) + 1;
        ProjectFileVersion fileVersion = projectFileVersionRepository.save(ProjectFileVersion.builder()
                .fileId(file.getId())
                .repositoryId(repo.getId())
                .commitId(mergeCommit.getId())
                .blobId(blob.getId())
                .version("v" + nextVersionSeq)
                .versionSeq(nextVersionSeq)
                .contentHash(blob.getSha256())
                .pathAtVersion(change.path())
                .changeType(change.changeType())
                .parentVersionId(parentVersionId)
                .serverPath(blob.getStoragePath())
                .fileSizeBytes(blob.getSizeBytes())
                .uploadedBy(currentUserId)
                .commitMessage(mergeCommit.getMessage())
                .build());

        file.setRepositoryId(repo.getId());
        file.setFileName(extractFileName(change.path()));
        file.setCanonicalPath(change.path());
        file.setFileKey(change.path());
        file.setFilePath(blob.getStoragePath());
        file.setFileSizeBytes(blob.getSizeBytes());
        file.setFileType(ProjectFileTypeSupport.resolve(change.path(), file.getFileName()));
        file.setVersion(fileVersion.getVersion());
        file.setLatestBlobId(blob.getId());
        file.setLatestVersionId(fileVersion.getId());
        file.setLatestCommitId(mergeCommit.getId());
        file.setIsLatest(true);
        file.setDeletedFlag(false);
        file.setContentHash(blob.getSha256());
        file.setLastModifiedAt(java.time.LocalDateTime.now());
        projectFileRepository.save(file);

        mergedSnapshot.put(change.path(), ProjectSnapshotItem.builder()
                .projectFileId(file.getId())
                .projectFileVersionId(fileVersion.getId())
                .blobId(blob.getId())
                .canonicalPath(change.path())
                .contentHash(blob.getSha256())
                .build());

        projectCommitChangeRepository.save(ProjectCommitChange.builder()
                .commitId(mergeCommit.getId())
                .projectFileId(file.getId())
                .oldBlobId(change.targetItem() == null ? null : change.targetItem().getBlobId())
                .newBlobId(blob.getId())
                .oldPath(change.path())
                .newPath(change.path())
                .changeType(change.changeType())
                .diffSummaryJson("{\"path\":\"" + change.path() + "\",\"changeType\":\"" + change.changeType() + "\",\"merge\":true}")
                .build());
    }

    private ProjectFile resolveProjectFile(Long projectId,
                                           ProjectSnapshotItem targetItem,
                                           ProjectSnapshotItem mergedItem,
                                           String path,
                                           Long repositoryId,
                                           ProjectBlob blob) {
        ProjectFile file = null;
        if (targetItem != null) {
            file = projectFileRepository.findById(targetItem.getProjectFileId()).orElse(null);
        }
        if (file == null && mergedItem != null) {
            file = projectFileRepository.findById(mergedItem.getProjectFileId()).orElse(null);
        }
        if (file == null) {
            file = projectFileRepository.findByProjectIdAndCanonicalPathAndDeletedFlagFalse(projectId, path).orElse(null);
        }
        if (file == null) {
            file = ProjectFile.builder()
                    .projectId(projectId)
                    .repositoryId(repositoryId)
                    .fileName(extractFileName(path))
                    .canonicalPath(path)
                    .fileKey(path)
                    .filePath(blob.getStoragePath())
                    .fileSizeBytes(blob.getSizeBytes())
                    .fileType(ProjectFileTypeSupport.resolve(path, extractFileName(path)))
                    .isMain(false)
                    .isLatest(true)
                    .deletedFlag(false)
                    .build();
        }
        if (file.getId() == null) {
            file = projectFileRepository.save(file);
        }
        return file;
    }

    private Map<String, ProjectSnapshotItem> loadSnapshotMapByCommitId(Long commitId) {
        if (commitId == null) {
            return new LinkedHashMap<>();
        }
        ProjectCommit commit = projectCommitRepository.findById(commitId).orElse(null);
        if (commit == null || commit.getSnapshotId() == null) {
            return new LinkedHashMap<>();
        }
        Map<String, ProjectSnapshotItem> map = new LinkedHashMap<>();
        for (ProjectSnapshotItem item : projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(commit.getSnapshotId())) {
            map.put(item.getCanonicalPath(), cloneSnapshotItem(item));
        }
        return map;
    }

    private Map<String, ProjectSnapshotItem> cloneSnapshotMap(Map<String, ProjectSnapshotItem> source) {
        Map<String, ProjectSnapshotItem> cloned = new LinkedHashMap<>();
        if (source == null) {
            return cloned;
        }
        for (Map.Entry<String, ProjectSnapshotItem> entry : source.entrySet()) {
            cloned.put(entry.getKey(), cloneSnapshotItem(entry.getValue()));
        }
        return cloned;
    }

    private ProjectSnapshotItem cloneSnapshotItem(ProjectSnapshotItem item) {
        if (item == null) {
            return null;
        }
        return ProjectSnapshotItem.builder()
                .projectFileId(item.getProjectFileId())
                .projectFileVersionId(item.getProjectFileVersionId())
                .blobId(item.getBlobId())
                .canonicalPath(item.getCanonicalPath())
                .contentHash(item.getContentHash())
                .build();
    }

    private boolean sameSnapshotItem(ProjectSnapshotItem left, ProjectSnapshotItem right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (Objects.equals(left.getBlobId(), right.getBlobId())) {
            return true;
        }
        return Objects.equals(left.getContentHash(), right.getContentHash());
    }

    private String extractFileName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int index = path.lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : path;
    }

    private ProjectMergeRequest refreshHeadsIfNeeded(ProjectMergeRequest mr) {
        if (mr == null || !"open".equalsIgnoreCase(mr.getStatus())) {
            return mr;
        }
        ProjectBranch source = projectBranchRepository.findById(mr.getSourceBranchId()).orElse(null);
        ProjectBranch target = projectBranchRepository.findById(mr.getTargetBranchId()).orElse(null);
        return syncMergeRequestHeads(mr, source, target);
    }

    private ProjectMergeRequest syncMergeRequestHeads(ProjectMergeRequest mr, ProjectBranch source, ProjectBranch target) {
        boolean changed = false;
        if (source != null && source.getHeadCommitId() != null && !Objects.equals(mr.getSourceHeadCommitId(), source.getHeadCommitId())) {
            mr.setSourceHeadCommitId(source.getHeadCommitId());
            changed = true;
        }
        if (target != null && target.getHeadCommitId() != null && !Objects.equals(mr.getTargetHeadCommitId(), target.getHeadCommitId())) {
            mr.setTargetHeadCommitId(target.getHeadCommitId());
            changed = true;
        }
        return changed ? projectMergeRequestRepository.save(mr) : mr;
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

    private String normalizeCheckType(String checkType) {
        if (checkType == null || checkType.isBlank()) {
            return "custom";
        }
        return checkType.trim().toLowerCase(Locale.ROOT);
    }

    private Boolean resolveBinaryFlagByBlobId(Map<Long, Boolean> cache, Long blobId) {
        if (blobId == null) {
            return null;
        }
        return cache.computeIfAbsent(blobId, id -> projectBlobRepository.findById(id)
                .map(blob -> isBinaryMimeType(blob.getMimeType()))
                .orElse(Boolean.FALSE));
    }

    private boolean isBinaryMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return false;
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("text/")) {
            return false;
        }
        return !(normalized.contains("json")
                || normalized.contains("xml")
                || normalized.contains("javascript")
                || normalized.contains("ecmascript")
                || normalized.contains("yaml")
                || normalized.contains("yml")
                || normalized.contains("x-sh")
                || normalized.contains("sql"));
    }

    private String resolveTitle(String rawTitle, ProjectBranch source, ProjectBranch target) {
        if (rawTitle != null && !rawTitle.isBlank()) {
            return rawTitle.trim();
        }
        return "Merge " + source.getName() + " -> " + target.getName();
    }

    private void assertBranchBelongsToRepository(ProjectBranch branch, Long repositoryId, String label) {
        if (branch == null || !Objects.equals(branch.getRepositoryId(), repositoryId)) {
            throw new BusinessException(label + "不属于当前项目仓库");
        }
    }

    private record MergeCheckContext(ProjectMergeRequest mr,
                                     ProjectCodeRepository repo,
                                     ProjectBranch source,
                                     ProjectBranch target) {
    }

    private record ConflictSnapshotView(ProjectSnapshotItem baseItem,
                                        ProjectSnapshotItem sourceItem,
                                        ProjectSnapshotItem targetItem,
                                        String baseContent,
                                        String sourceContent,
                                        String targetContent,
                                        List<String> baseLines,
                                        List<String> sourceLines,
                                        List<String> targetLines,
                                        Boolean binaryFile) {
    }

    private record CommitDistance(Long commitId, int distance) {
    }

    private record ResolvedMergeChange(String path,
                                       String changeType,
                                       ProjectSnapshotItem targetItem,
                                       ProjectSnapshotItem mergedItem) {
    }

    private record ThreeWayMergePlan(List<String> conflictPaths,
                                     List<ResolvedMergeChange> acceptedChanges) {
    }

    private record MergeExecutionState(ProjectCommit sourceHead,
                                       ProjectCommit targetHead,
                                       ProjectCommit mergeBase,
                                       Map<String, ProjectSnapshotItem> baseSnapshot,
                                       Map<String, ProjectSnapshotItem> sourceSnapshot,
                                       Map<String, ProjectSnapshotItem> targetSnapshot) {
    }

    private record StructuredResolutionState(List<ConflictResolutionOption> options,
                                             MergeCheckResult latestMergeCheck,
                                             String resolutionMode,
                                             Long activityLogId) {
    }

    private record AppliedStructuredResolution(MergeCheckResult mergeCheckResult,
                                               Map<String, ProjectSnapshotItem> effectiveSourceSnapshot,
                                               Map<String, ProjectSnapshotItem> effectiveTargetSnapshot,
                                               List<ConflictResolutionOption> appliedOptions,
                                               List<String> resolvedConflictIds) {
    }

    private ProjectMergeRequestVO toVO(ProjectMergeRequest mr) {
        return ProjectMergeRequestVO.builder()
                .id(mr.getId())
                .repositoryId(mr.getRepositoryId())
                .sourceBranchId(mr.getSourceBranchId())
                .targetBranchId(mr.getTargetBranchId())
                .sourceHeadCommitId(mr.getSourceHeadCommitId())
                .targetHeadCommitId(mr.getTargetHeadCommitId())
                .title(mr.getTitle())
                .description(mr.getDescription())
                .status(mr.getStatus())
                .createdBy(mr.getCreatedBy())
                .mergedBy(mr.getMergedBy())
                .mergedAt(mr.getMergedAt())
                .createdAt(mr.getCreatedAt())
                .reviews(new ArrayList<>(projectReviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(mr.getId())))
                .checks(new ArrayList<>(projectCheckRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(mr.getId())))
                .build();
    }
}

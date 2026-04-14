package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectMergeRequestCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectReviewSubmitRequest;
import com.alikeyou.itmoduleproject.entity.*;
import com.alikeyou.itmoduleproject.repository.*;
import com.alikeyou.itmoduleproject.service.ProjectMergeRequestService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectFileTypeSupport;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.diff.MergeCheckResult;
import com.alikeyou.itmoduleproject.support.diff.ProjectMergeDiffSupport;
import com.alikeyou.itmoduleproject.vo.ProjectMergeRequestVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProjectMergeRequestServiceImpl implements ProjectMergeRequestService {

    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectMergeRequestRepository projectMergeRequestRepository;
    private final ProjectReviewRepository projectReviewRepository;
    private final ProjectCheckRunRepository projectCheckRunRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectCommitParentRepository projectCommitParentRepository;
    private final ProjectSnapshotRepository projectSnapshotRepository;
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository;
    private final ProjectCommitChangeRepository projectCommitChangeRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectBlobRepository projectBlobRepository;
    private final ProjectPermissionService projectPermissionService;

    public ProjectMergeRequestServiceImpl(ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                          ProjectBranchRepository projectBranchRepository,
                                          ProjectMergeRequestRepository projectMergeRequestRepository,
                                          ProjectReviewRepository projectReviewRepository,
                                          ProjectCheckRunRepository projectCheckRunRepository,
                                          ProjectCommitRepository projectCommitRepository,
                                          ProjectCommitParentRepository projectCommitParentRepository,
                                          ProjectSnapshotRepository projectSnapshotRepository,
                                          ProjectSnapshotItemRepository projectSnapshotItemRepository,
                                          ProjectCommitChangeRepository projectCommitChangeRepository,
                                          ProjectFileRepository projectFileRepository,
                                          ProjectFileVersionRepository projectFileVersionRepository,
                                          ProjectBlobRepository projectBlobRepository,
                                          ProjectPermissionService projectPermissionService) {
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectBranchRepository = projectBranchRepository;
        this.projectMergeRequestRepository = projectMergeRequestRepository;
        this.projectReviewRepository = projectReviewRepository;
        this.projectCheckRunRepository = projectCheckRunRepository;
        this.projectCommitRepository = projectCommitRepository;
        this.projectCommitParentRepository = projectCommitParentRepository;
        this.projectSnapshotRepository = projectSnapshotRepository;
        this.projectSnapshotItemRepository = projectSnapshotItemRepository;
        this.projectCommitChangeRepository = projectCommitChangeRepository;
        this.projectFileRepository = projectFileRepository;
        this.projectFileVersionRepository = projectFileVersionRepository;
        this.projectBlobRepository = projectBlobRepository;
        this.projectPermissionService = projectPermissionService;
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
        projectReviewRepository.save(ProjectReview.builder()
                .mergeRequestId(mr.getId())
                .reviewerId(currentUserId)
                .reviewResult(request.getReviewResult() == null ? "comment" : request.getReviewResult())
                .reviewComment(request.getReviewComment())
                .build());
        return toVO(mr);
    }

    @Override
    public MergeCheckResult checkMerge(Long mergeRequestId, Long currentUserId) {
        ProjectMergeRequest mr = projectMergeRequestRepository.findById(mergeRequestId)
                .orElseThrow(() -> new BusinessException("merge request not found"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(mr.getRepositoryId())
                .orElseThrow(() -> new BusinessException("project repository not found"));
        projectPermissionService.assertProjectReadable(repo.getProjectId(), currentUserId);
        ProjectBranch source = projectBranchRepository.findById(mr.getSourceBranchId())
                .orElseThrow(() -> new BusinessException("source branch not found"));
        ProjectBranch target = projectBranchRepository.findById(mr.getTargetBranchId())
                .orElseThrow(() -> new BusinessException("target branch not found"));
        assertBranchBelongsToRepository(source, repo.getId(), "source branch");
        assertBranchBelongsToRepository(target, repo.getId(), "target branch");
        mr = syncMergeRequestHeads(mr, source, target);

        ProjectCommit sourceHead = source.getHeadCommitId() == null ? null :
                projectCommitRepository.findById(source.getHeadCommitId()).orElse(null);
        if (sourceHead == null) {
            throw new BusinessException("source branch has no mergeable commit");
        }
        ProjectCommit targetHead = target.getHeadCommitId() == null ? null :
                projectCommitRepository.findById(target.getHeadCommitId()).orElse(null);
        if (targetHead == null) {
            throw new BusinessException("target branch has no mergeable commit");
        }

        ProjectCommit mergeBase = resolveMergeBase(sourceHead, targetHead);
        Map<String, ProjectSnapshotItem> baseSnapshot = loadSnapshotMapByCommitId(mergeBase == null ? null : mergeBase.getId());
        Map<String, ProjectSnapshotItem> sourceSnapshot = loadSnapshotMapByCommitId(sourceHead.getId());
        Map<String, ProjectSnapshotItem> targetSnapshot = loadSnapshotMapByCommitId(targetHead.getId());
        return ProjectMergeDiffSupport.buildMergeCheck(
                mr.getId(),
                repo.getId(),
                source.getId(),
                target.getId(),
                mergeBase == null ? null : mergeBase.getId(),
                sourceHead.getId(),
                targetHead.getId(),
                baseSnapshot,
                sourceSnapshot,
                targetSnapshot
        );
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

        if (Boolean.TRUE.equals(target.getProtectedFlag())) {
            boolean approved = projectReviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(mr.getId())
                    .stream().anyMatch(item -> "approve".equalsIgnoreCase(item.getReviewResult()));
            if (!approved) {
                throw new BusinessException("受保护分支合并前至少需要一个通过评审");
            }
            boolean hasFailedCheck = resolveEffectiveChecks(mr.getId(), source.getHeadCommitId())
                    .stream().anyMatch(item -> "failed".equalsIgnoreCase(item.getCheckStatus()));
            if (hasFailedCheck) {
                throw new BusinessException("当前合并请求存在失败检查，不能合并");
            }
        }

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
        ThreeWayMergePlan mergePlan = buildThreeWayMergePlan(baseSnapshot, sourceSnapshot, targetSnapshot);
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

        Map<String, ProjectSnapshotItem> mergedSnapshot = cloneSnapshotMap(targetSnapshot);
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

        return toVO(mr);
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

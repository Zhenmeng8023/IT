package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectMilestone;
import com.alikeyou.itmoduleproject.entity.ProjectRelease;
import com.alikeyou.itmoduleproject.entity.ProjectReleaseFile;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMilestoneRepository;
import com.alikeyou.itmoduleproject.repository.ProjectReleaseFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectReleaseRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotItemRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectReleaseService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectSnapshotDiffSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectReleaseServiceImpl implements ProjectReleaseService {

    private static final Set<String> RELEASE_TYPE_SET = Set.of("draft", "beta", "stable", "hotfix");
    private static final Set<String> RELEASE_STATUS_SET = Set.of("draft", "published", "archived");

    private final ProjectReleaseRepository projectReleaseRepository;
    private final ProjectReleaseFileRepository projectReleaseFileRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectRepository projectRepository;
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectActivityLogService projectActivityLogService;

    @Override
    public List<ProjectRelease> listReleases(Long projectId, String status, Long currentUserId) {
        assertProjectExists(projectId);
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        if (!StringUtils.hasText(status)) {
            return projectReleaseRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        }
        validateReleaseStatus(status);
        return projectReleaseRepository.findByProjectIdAndStatusOrderByPublishedAtDescIdDesc(projectId, status.trim());
    }

    @Override
    public Map<String, Object> getReleaseDetail(Long id, Long currentUserId) {
        ProjectRelease release = getReleaseOrThrow(id);
        projectPermissionService.assertProjectReadable(release.getProjectId(), currentUserId);
        ProjectCommit basedCommit = release.getBasedCommitId() == null ? null
                : projectCommitRepository.findById(release.getBasedCommitId()).orElse(null);
        ProjectBranch branch = release.getBranchId() == null ? null
                : projectBranchRepository.findById(release.getBranchId()).orElse(null);
        ProjectMilestone milestone = release.getBasedMilestoneId() == null ? null
                : projectMilestoneRepository.findById(release.getBasedMilestoneId()).orElse(null);
        ProjectRelease previousRelease = resolvePreviousComparableRelease(release);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("release", release);
        map.put("files", projectReleaseFileRepository.findByReleaseIdOrderBySortOrderAscIdAsc(id));
        map.put("commit", basedCommit);
        map.put("branch", branch);
        map.put("milestone", milestone);
        map.put("previousRelease", previousRelease);
        map.put("compare", buildReleaseCompare(release, previousRelease));
        return map;
    }

    @Override
    @Transactional
    public ProjectRelease createRelease(ProjectRelease request, Long currentUserId) {
        if (request == null || request.getProjectId() == null) {
            throw new BusinessException("项目ID不能为空");
        }
        projectPermissionService.assertProjectManageMembers(request.getProjectId(), currentUserId);
        assertProjectExists(request.getProjectId());
        validateReleaseInput(request, true);
        if (projectReleaseRepository.existsByProjectIdAndVersion(request.getProjectId(), request.getVersion().trim())) {
            throw new BusinessException("发布版本号已存在");
        }
        ProjectCodeRepository repository = resolveRepository(request.getProjectId());
        String normalizedStatus = normalizeReleaseStatus(request.getStatus());
        ProjectRelease entity = ProjectRelease.builder()
                .projectId(request.getProjectId())
                .repositoryId(repository == null ? null : repository.getId())
                .branchId(request.getBranchId())
                .basedCommitId(request.getBasedCommitId())
                .basedMilestoneId(request.getBasedMilestoneId())
                .recommendedFlag(Boolean.TRUE.equals(request.getRecommendedFlag()))
                .version(request.getVersion().trim())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .releaseNotes(request.getReleaseNotes())
                .releaseType(normalizeReleaseType(request.getReleaseType()))
                .status(normalizedStatus)
                .createdBy(currentUserId)
                .publishedBy(isPublished(normalizedStatus) ? currentUserId : null)
                .publishedAt(isPublished(normalizedStatus) ? LocalDateTime.now() : null)
                .build();
        applyReleaseBinding(entity, repository, null);
        ProjectRelease saved = projectReleaseRepository.save(entity);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "create_release", "release", saved.getId(), "创建发布版本：" + saved.getVersion());
        if (isPublished(saved.getStatus())) {
            projectActivityLogService.record(saved.getProjectId(), currentUserId, "publish_release", "release", saved.getId(), "发布版本：" + saved.getVersion());
            touchCurrentRelease(repository, saved);
        }
        return saved;
    }

    @Override
    @Transactional
    public ProjectRelease updateRelease(Long id, ProjectRelease request, Long currentUserId) {
        ProjectRelease entity = getReleaseOrThrow(id);
        projectPermissionService.assertProjectManageMembers(entity.getProjectId(), currentUserId);
        validateReleaseInput(request, false);
        if (!entity.getVersion().equals(request.getVersion().trim())
                && projectReleaseRepository.existsByProjectIdAndVersion(entity.getProjectId(), request.getVersion().trim())) {
            throw new BusinessException("发布版本号已存在");
        }
        ProjectCodeRepository repository = resolveRepository(entity.getProjectId());
        Long previousBasedCommitId = entity.getBasedCommitId();
        entity.setVersion(request.getVersion().trim());
        entity.setTitle(request.getTitle().trim());
        entity.setDescription(request.getDescription());
        entity.setReleaseNotes(request.getReleaseNotes());
        entity.setReleaseType(normalizeReleaseType(request.getReleaseType()));
        entity.setStatus(normalizeReleaseStatus(request.getStatus()));
        entity.setRepositoryId(repository == null ? null : repository.getId());
        entity.setBranchId(request.getBranchId());
        entity.setBasedCommitId(request.getBasedCommitId());
        entity.setBasedMilestoneId(request.getBasedMilestoneId());
        entity.setRecommendedFlag(Boolean.TRUE.equals(request.getRecommendedFlag()));
        applyReleaseBinding(entity, repository, previousBasedCommitId);
        if (isPublished(entity.getStatus()) && entity.getPublishedAt() == null) {
            entity.setPublishedAt(LocalDateTime.now());
            entity.setPublishedBy(currentUserId);
        }
        if (!isPublished(entity.getStatus())) {
            entity.setPublishedAt(null);
            entity.setPublishedBy(null);
        }
        ProjectRelease saved = projectReleaseRepository.save(entity);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "update_release", "release", saved.getId(), "更新发布版本：" + saved.getVersion());
        if (isPublished(saved.getStatus())) {
            touchCurrentRelease(repository, saved);
        }
        return saved;
    }

    @Override
    @Transactional
    public ProjectRelease publishRelease(Long id, Long currentUserId) {
        ProjectRelease entity = getReleaseOrThrow(id);
        projectPermissionService.assertProjectManageMembers(entity.getProjectId(), currentUserId);
        ProjectCodeRepository repository = resolveRepository(entity.getProjectId());
        applyReleaseBinding(entity, repository, entity.getBasedCommitId());
        entity.setStatus("published");
        entity.setPublishedAt(LocalDateTime.now());
        entity.setPublishedBy(currentUserId);
        ProjectRelease saved = projectReleaseRepository.save(entity);
        touchCurrentRelease(repository, saved);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "publish_release", "release", saved.getId(), "发布版本：" + saved.getVersion());
        return saved;
    }

    @Override
    @Transactional
    public ProjectRelease archiveRelease(Long id, Long currentUserId) {
        ProjectRelease entity = getReleaseOrThrow(id);
        projectPermissionService.assertProjectManageMembers(entity.getProjectId(), currentUserId);
        entity.setStatus("archived");
        ProjectRelease saved = projectReleaseRepository.save(entity);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "archive_release", "release", saved.getId(), "归档版本：" + saved.getVersion());
        return saved;
    }

    @Override
    @Transactional
    public List<ProjectReleaseFile> bindProjectFiles(Long releaseId, List<Long> projectFileIds, Long currentUserId) {
        ProjectRelease release = getReleaseOrThrow(releaseId);
        projectPermissionService.assertProjectManageMembers(release.getProjectId(), currentUserId);
        if (projectFileIds == null || projectFileIds.isEmpty()) {
            throw new BusinessException("请选择要绑定的项目文件");
        }
        ProjectCommit basedCommit = requireReleaseCommit(release);
        Map<String, ProjectSnapshotItem> snapshotMap = loadSnapshotMapByCommit(basedCommit.getId());
        List<ProjectFile> files = projectFileRepository.findByProjectIdAndIdInOrderByUploadTimeDesc(release.getProjectId(), projectFileIds);
        if (files.isEmpty()) {
            throw new BusinessException("没有可绑定的项目文件");
        }
        List<ProjectReleaseFile> existingFiles = projectReleaseFileRepository.findByReleaseIdOrderBySortOrderAscIdAsc(releaseId);
        java.util.Set<String> existingPaths = new java.util.HashSet<>();
        for (ProjectReleaseFile releaseFile : existingFiles) {
            if (StringUtils.hasText(releaseFile.getFilePath())) {
                existingPaths.add(releaseFile.getFilePath().trim());
            }
        }

        List<String> missingPaths = new ArrayList<>();
        int baseOrder = (int) projectReleaseFileRepository.countByReleaseId(releaseId);
        List<ProjectReleaseFile> list = new ArrayList<>();
        for (ProjectFile file : files) {
            String canonicalPath = resolveCanonicalPath(file);
            ProjectSnapshotItem snapshotItem = snapshotMap.get(canonicalPath);
            if (snapshotItem == null) {
                missingPaths.add(canonicalPath);
                continue;
            }
            if (existingPaths.contains(canonicalPath)) {
                continue;
            }
            ProjectFileVersion releaseVersion = projectFileVersionRepository.findById(snapshotItem.getProjectFileVersionId()).orElse(null);
            list.add(ProjectReleaseFile.builder()
                    .releaseId(releaseId)
                    .projectFileId(snapshotItem.getProjectFileId())
                    .fileVersionId(snapshotItem.getProjectFileVersionId())
                    .fileName(extractFileName(canonicalPath))
                    .filePath(canonicalPath)
                    .fileSizeBytes(releaseVersion == null ? file.getFileSizeBytes() : releaseVersion.getFileSizeBytes())
                    .sortOrder(baseOrder++)
                    .build());
            existingPaths.add(canonicalPath);
        }
        if (!missingPaths.isEmpty()) {
            throw new BusinessException("所选文件不在该发布绑定的 commit 快照内：" + String.join("、", missingPaths));
        }
        if (list.isEmpty()) {
            return List.of();
        }
        List<ProjectReleaseFile> saved = projectReleaseFileRepository.saveAll(list);
        projectActivityLogService.record(release.getProjectId(), currentUserId, "bind_release_files", "release", releaseId, "绑定发布文件，版本：" + release.getVersion());
        return saved;
    }

    @Override
    @Transactional
    public void removeReleaseFile(Long releaseId, Long releaseFileId, Long currentUserId) {
        ProjectRelease release = getReleaseOrThrow(releaseId);
        projectPermissionService.assertProjectManageMembers(release.getProjectId(), currentUserId);
        projectReleaseFileRepository.deleteByReleaseIdAndId(releaseId, releaseFileId);
        projectActivityLogService.record(release.getProjectId(), currentUserId, "remove_release_file", "release", releaseId, "移除发布文件，版本：" + release.getVersion());
    }

    @Override
    public Map<String, Object> getLatestReleaseSummary(Long projectId, Long currentUserId) {
        assertProjectExists(projectId);
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        ProjectRelease release = projectReleaseRepository.findFirstByProjectIdAndStatusOrderByPublishedAtDescIdDesc(projectId, "published").orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("latest", release);
        map.put("total", projectReleaseRepository.findByProjectIdOrderByCreatedAtDesc(projectId).size());
        map.put("draftCount", projectReleaseRepository.findByProjectIdAndStatusOrderByPublishedAtDescIdDesc(projectId, "draft").size());
        map.put("publishedCount", projectReleaseRepository.findByProjectIdAndStatusOrderByPublishedAtDescIdDesc(projectId, "published").size());
        map.put("archivedCount", projectReleaseRepository.findByProjectIdAndStatusOrderByPublishedAtDescIdDesc(projectId, "archived").size());
        return map;
    }

    private ProjectRelease getReleaseOrThrow(Long id) {
        return projectReleaseRepository.findById(id).orElseThrow(() -> new BusinessException("发布版本不存在"));
    }

    private void assertProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException("项目不存在");
        }
    }

    private void validateReleaseInput(ProjectRelease request, boolean create) {
        if (request == null) {
            throw new BusinessException("请求数据不能为空");
        }
        if (!StringUtils.hasText(request.getVersion())) {
            throw new BusinessException("发布版本号不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException("发布标题不能为空");
        }
        validateReleaseType(request.getReleaseType());
        validateReleaseStatus(request.getStatus());
        if (create && request.getProjectId() == null) {
            throw new BusinessException("项目ID不能为空");
        }
        if (request.getBasedCommitId() == null) {
            throw new BusinessException("发布必须绑定明确的 commit");
        }
    }

    private void validateReleaseType(String releaseType) {
        if (!RELEASE_TYPE_SET.contains(normalizeReleaseType(releaseType))) {
            throw new BusinessException("发布类型不合法");
        }
    }

    private void validateReleaseStatus(String status) {
        if (!RELEASE_STATUS_SET.contains(normalizeReleaseStatus(status))) {
            throw new BusinessException("发布状态不合法");
        }
    }

    private String normalizeReleaseType(String value) {
        return StringUtils.hasText(value) ? value.trim() : "draft";
    }

    private String normalizeReleaseStatus(String value) {
        return StringUtils.hasText(value) ? value.trim() : "draft";
    }

    private boolean isPublished(String status) {
        return "published".equals(status);
    }

    private ProjectCodeRepository resolveRepository(Long projectId) {
        return projectCodeRepositoryRepository.findByProjectId(projectId).orElse(null);
    }

    private void applyReleaseBinding(ProjectRelease entity, ProjectCodeRepository repository, Long previousBasedCommitId) {
        if (entity == null) {
            return;
        }
        if (entity.getRecommendedFlag() == null) {
            entity.setRecommendedFlag(false);
        }
        ProjectCommit commit = validateCommitBelongsToRepository(repository, entity.getBasedCommitId());
        entity.setRepositoryId(repository == null ? null : repository.getId());
        if (entity.getBranchId() == null) {
            entity.setBranchId(commit.getBranchId());
        } else if (!Objects.equals(entity.getBranchId(), commit.getBranchId())) {
            throw new BusinessException("发布分支必须和基线 commit 所属分支一致");
        }
        validateBranchBelongsToRepository(repository, entity.getBranchId());
        if (entity.getFrozenAt() == null || !Objects.equals(previousBasedCommitId, entity.getBasedCommitId())) {
            entity.setFrozenAt(LocalDateTime.now());
        }
        validateMilestoneBelongsToProject(entity.getProjectId(), entity.getBasedMilestoneId());
    }

    private ProjectCommit validateCommitBelongsToRepository(ProjectCodeRepository repository, Long commitId) {
        if (commitId == null) {
            throw new BusinessException("发布必须绑定明确的 commit");
        }
        if (repository == null) {
            throw new BusinessException("发布依赖的提交不存在，请先初始化项目仓库");
        }
        ProjectCommit commit = projectCommitRepository.findById(commitId)
                .orElseThrow(() -> new BusinessException("发布依赖的提交不存在"));
        if (!repository.getId().equals(commit.getRepositoryId())) {
            throw new BusinessException("发布依赖的提交不属于当前项目仓库");
        }
        return commit;
    }

    private void validateBranchBelongsToRepository(ProjectCodeRepository repository, Long branchId) {
        if (branchId == null || repository == null) {
            return;
        }
        ProjectBranch branch = projectBranchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("发布依赖的分支不存在"));
        if (!repository.getId().equals(branch.getRepositoryId())) {
            throw new BusinessException("发布依赖的分支不属于当前项目仓库");
        }
    }

    private void validateMilestoneBelongsToProject(Long projectId, Long milestoneId) {
        if (milestoneId == null) {
            return;
        }
        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException("发布依赖的里程碑不存在"));
        if (!projectId.equals(milestone.getProjectId())) {
            throw new BusinessException("发布依赖的里程碑不属于当前项目");
        }
    }

    private void touchCurrentRelease(ProjectCodeRepository repository, ProjectRelease release) {
        if (repository == null || release == null) {
            return;
        }
        repository.setCurrentReleaseId(release.getId());
        projectCodeRepositoryRepository.save(repository);
    }

    private ProjectCommit requireReleaseCommit(ProjectRelease release) {
        if (release.getBasedCommitId() == null) {
            throw new BusinessException("发布必须先绑定基线 commit");
        }
        return projectCommitRepository.findById(release.getBasedCommitId())
                .orElseThrow(() -> new BusinessException("发布基线 commit 不存在"));
    }

    private Map<String, ProjectSnapshotItem> loadSnapshotMapByCommit(Long commitId) {
        if (commitId == null) {
            return Map.of();
        }
        ProjectCommit commit = projectCommitRepository.findById(commitId).orElse(null);
        if (commit == null || commit.getSnapshotId() == null) {
            return Map.of();
        }
        Map<String, ProjectSnapshotItem> map = new LinkedHashMap<>();
        for (ProjectSnapshotItem item : projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(commit.getSnapshotId())) {
            map.put(item.getCanonicalPath(), item);
        }
        return map;
    }

    private ProjectRelease resolvePreviousComparableRelease(ProjectRelease release) {
        List<ProjectRelease> releases = projectReleaseRepository.findByProjectIdOrderByCreatedAtDesc(release.getProjectId());
        for (ProjectRelease candidate : releases) {
            if (candidate == null || Objects.equals(candidate.getId(), release.getId()) || candidate.getBasedCommitId() == null) {
                continue;
            }
            if (isEarlierRelease(candidate, release)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isEarlierRelease(ProjectRelease candidate, ProjectRelease current) {
        if (candidate.getCreatedAt() == null || current.getCreatedAt() == null) {
            return !Objects.equals(candidate.getId(), current.getId());
        }
        if (candidate.getCreatedAt().isBefore(current.getCreatedAt())) {
            return true;
        }
        if (candidate.getCreatedAt().isEqual(current.getCreatedAt())) {
            return candidate.getId() < current.getId();
        }
        return false;
    }

    private Map<String, Object> buildReleaseCompare(ProjectRelease release, ProjectRelease previousRelease) {
        if (release.getBasedCommitId() == null) {
            return null;
        }
        Map<String, ProjectSnapshotItem> fromMap = previousRelease == null
                ? Map.of()
                : loadSnapshotMapByCommit(previousRelease.getBasedCommitId());
        Map<String, ProjectSnapshotItem> toMap = loadSnapshotMapByCommit(release.getBasedCommitId());
        Map<String, Object> compare = new LinkedHashMap<>(ProjectSnapshotDiffSupport.buildComparePayload(fromMap, toMap));
        compare.put("fromReleaseId", previousRelease == null ? null : previousRelease.getId());
        compare.put("toReleaseId", release.getId());
        compare.put("fromCommitId", previousRelease == null ? null : previousRelease.getBasedCommitId());
        compare.put("toCommitId", release.getBasedCommitId());
        return compare;
    }

    private String resolveCanonicalPath(ProjectFile file) {
        if (file == null) {
            return "";
        }
        if (StringUtils.hasText(file.getCanonicalPath())) {
            return file.getCanonicalPath().trim();
        }
        if (StringUtils.hasText(file.getFileKey())) {
            return file.getFileKey().trim();
        }
        return file.getFileName() == null ? "" : file.getFileName().trim();
    }

    private String extractFileName(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String value = path.trim();
        int idx = value.lastIndexOf('/');
        return idx >= 0 ? value.substring(idx + 1) : value;
    }
}

package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectRelease;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectReleaseFile;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectMilestone;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectReleaseFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectReleaseRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMilestoneRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectReleaseService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectRepository projectRepository;
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
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("release", release);
        map.put("files", projectReleaseFileRepository.findByReleaseIdOrderBySortOrderAscIdAsc(id));
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
                .status(normalizeReleaseStatus(request.getStatus()))
                .createdBy(currentUserId)
                .publishedBy(isPublished(normalizeReleaseStatus(request.getStatus())) ? currentUserId : null)
                .publishedAt(isPublished(normalizeReleaseStatus(request.getStatus())) ? LocalDateTime.now() : null)
                .build();
        applyReleaseBinding(entity, repository);
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
        applyReleaseBinding(entity, repository);
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
        entity.setStatus("published");
        entity.setPublishedAt(LocalDateTime.now());
        entity.setPublishedBy(currentUserId);
        ProjectRelease saved = projectReleaseRepository.save(entity);
        touchCurrentRelease(resolveRepository(entity.getProjectId()), saved);
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
        List<ProjectFile> files = projectFileRepository.findByProjectIdAndIdInOrderByUploadTimeDesc(release.getProjectId(), projectFileIds);
        if (files.isEmpty()) {
            throw new BusinessException("没有可绑定的项目文件");
        }
        int baseOrder = (int) projectReleaseFileRepository.countByReleaseId(releaseId);
        List<ProjectReleaseFile> list = new ArrayList<>();
        for (ProjectFile file : files) {
            if (projectReleaseFileRepository.existsByReleaseIdAndProjectFileId(releaseId, file.getId())) {
                continue;
            }
            ProjectFileVersion latestVersion = projectFileVersionRepository.findByFileIdOrderByUploadedAtDesc(file.getId()).stream().findFirst().orElse(null);
            list.add(ProjectReleaseFile.builder()
                    .releaseId(releaseId)
                    .projectFileId(file.getId())
                    .fileVersionId(latestVersion == null ? null : latestVersion.getId())
                    .fileName(file.getFileName())
                    .filePath(file.getFilePath())
                    .fileSizeBytes(file.getFileSizeBytes())
                    .sortOrder(baseOrder++)
                    .build());
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

    private void applyReleaseBinding(ProjectRelease entity, ProjectCodeRepository repository) {
        if (entity == null) {
            return;
        }
        if (entity.getRecommendedFlag() == null) {
            entity.setRecommendedFlag(false);
        }
        if (entity.getFrozenAt() == null && entity.getBasedCommitId() != null) {
            entity.setFrozenAt(LocalDateTime.now());
        }
        validateCommitBelongsToRepository(repository, entity.getBasedCommitId());
        validateMilestoneBelongsToProject(entity.getProjectId(), entity.getBasedMilestoneId());
    }

    private void validateCommitBelongsToRepository(ProjectCodeRepository repository, Long commitId) {
        if (commitId == null) {
            return;
        }
        if (repository == null) {
            throw new BusinessException("发布依赖的提交不存在，请先初始化项目仓库");
        }
        ProjectCommit commit = projectCommitRepository.findById(commitId)
                .orElseThrow(() -> new BusinessException("发布依赖的提交不存在"));
        if (!repository.getId().equals(commit.getRepositoryId())) {
            throw new BusinessException("发布依赖的提交不属于当前项目仓库");
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
}

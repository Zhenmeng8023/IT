package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectCommitParent;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitParentRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotRepository;
import com.alikeyou.itmoduleproject.service.ProjectFileService;
import com.alikeyou.itmoduleproject.service.ProjectWorkspaceService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.FileStorageService;
import com.alikeyou.itmoduleproject.support.ProjectPathUtils;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectCommitParentRepository projectCommitParentRepository;
    private final ProjectSnapshotRepository projectSnapshotRepository;
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository;
    private final ProjectPermissionService projectPermissionService;
    private final FileStorageService fileStorageService;
    private final ProjectWorkspaceService projectWorkspaceService;
    private final ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport;

    @Override
    @Transactional
    public ProjectWorkspaceItemVO uploadFile(Long projectId, Long branchId, String canonicalPath, MultipartFile file, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        Long resolvedBranchId = requireWriteBranchId(projectId, branchId, currentUserId);
        return projectWorkspaceService.stageFile(
                projectId,
                resolvedBranchId,
                currentUserId,
                resolveUploadPath(canonicalPath, file),
                file
        );
    }

    @Override
    @Transactional
    public List<ProjectWorkspaceItemVO> uploadZip(Long projectId, Long branchId, MultipartFile file, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        requireWriteBranchId(projectId, branchId, currentUserId);
        throw new BusinessException("ZIP upload is temporarily disabled");
    }

    @Override
    @Transactional
    public List<ProjectWorkspaceItemVO> uploadFiles(Long projectId, Long branchId, String targetDir, List<MultipartFile> files, Long currentUserId) {
        projectPermissionService.assertProjectWritable(projectId, currentUserId);
        Long resolvedBranchId = requireWriteBranchId(projectId, branchId, currentUserId);
        return projectWorkspaceService.stageFiles(projectId, resolvedBranchId, currentUserId, targetDir, files, null);
    }

    @Override
    @Transactional
    public ProjectWorkspaceItemVO uploadNewVersion(Long fileId, Long branchId, MultipartFile file, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectWritable(projectFile.getProjectId(), currentUserId);
        Long resolvedBranchId = requireWriteBranchId(projectFile.getProjectId(), branchId, currentUserId);
        BranchReadContext branchContext = resolveReadBranchContext(projectFile.getProjectId(), resolvedBranchId, currentUserId);
        ProjectSnapshotItem snapshotItem = requireSnapshotItemInBranchView(branchContext, fileId);
        return projectWorkspaceService.stageFile(
                projectFile.getProjectId(),
                resolvedBranchId,
                currentUserId,
                ProjectPathUtils.normalize(snapshotItem.getCanonicalPath()),
                file
        );
    }

    @Override
    public List<ProjectFileVO> listFiles(Long projectId, Long currentUserId) {
        return listFiles(projectId, null, currentUserId);
    }

    @Override
    public List<ProjectFileVO> listFiles(Long projectId, Long branchId, Long currentUserId) {
        BranchReadContext branchContext = resolveReadBranchContext(projectId, branchId, currentUserId);
        List<ProjectSnapshotItem> snapshotItems = listSnapshotItems(branchContext);
        if (snapshotItems.isEmpty()) {
            return List.of();
        }

        Map<Long, ProjectFileVersion> currentVersionMap = resolveCurrentVersionMap(snapshotItems);
        Map<Long, List<ProjectFileVersionVO>> reachableVersionMap = buildReachableVersionMapForSnapshot(branchContext, snapshotItems);
        Set<Long> mainFileIds = branchContext.defaultBranchView()
                ? projectFileRepository.findByProjectIdAndIsMainTrue(projectId).stream()
                .map(ProjectFile::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet())
                : Set.of();

        List<ProjectFileVO> results = new ArrayList<>(snapshotItems.size());
        for (ProjectSnapshotItem item : snapshotItems) {
            ProjectFileVersion currentVersion = currentVersionMap.get(item.getProjectFileVersionId());
            List<ProjectFileVersionVO> versions = reachableVersionMap.get(item.getProjectFileId());
            if ((versions == null || versions.isEmpty()) && currentVersion != null) {
                versions = List.of(ProjectVoMapper.toProjectFileVersionVO(currentVersion));
            }
            results.add(ProjectVoMapper.toBranchProjectFileVO(
                    projectId,
                    branchContext.branchId(),
                    branchContext.defaultBranchView(),
                    item.getProjectFileId(),
                    item.getCanonicalPath(),
                    currentVersion,
                    mainFileIds.contains(item.getProjectFileId()),
                    versions == null ? List.of() : versions
            ));
        }
        return results;
    }

    @Override
    public List<ProjectFileVersionVO> listVersions(Long fileId, Long currentUserId) {
        return listVersions(fileId, null, currentUserId);
    }

    @Override
    public List<ProjectFileVersionVO> listVersions(Long fileId, Long branchId, Long currentUserId) {
        Long projectId = resolveProjectIdByFileId(fileId);
        BranchReadContext branchContext = resolveReadBranchContext(projectId, branchId, currentUserId);
        ProjectSnapshotItem snapshotItem = requireSnapshotItemInBranchView(branchContext, fileId);
        Set<Long> reachableCommitIds = resolveReachableCommitIds(branchContext);
        Long currentVersionId = snapshotItem.getProjectFileVersionId();
        return projectFileVersionRepository.findByFileIdOrderByUploadedAtDesc(fileId)
                .stream()
                .filter(version -> isVersionReachableFromHead(version, currentVersionId, reachableCommitIds))
                .map(ProjectVoMapper::toProjectFileVersionVO)
                .toList();
    }

    @Override
    public Resource previewFile(Long fileId, Long currentUserId) {
        return previewFile(fileId, null, currentUserId);
    }

    @Override
    public Resource previewFile(Long fileId, Long branchId, Long currentUserId) {
        Long projectId = resolveProjectIdByFileId(fileId);
        BranchReadContext branchContext = resolveReadBranchContext(projectId, branchId, currentUserId);
        ProjectSnapshotItem snapshotItem = requireSnapshotItemInBranchView(branchContext, fileId);
        ProjectFileVersion version = requireVersionForSnapshotItem(snapshotItem);
        return loadFileResource(version.getServerPath());
    }

    @Override
    @Transactional
    public Resource downloadFile(Long fileId, Long currentUserId) {
        return downloadFile(fileId, null, currentUserId);
    }

    @Override
    @Transactional
    public Resource downloadFile(Long fileId, Long branchId, Long currentUserId) {
        Long projectId = resolveProjectIdByFileId(fileId);
        BranchReadContext branchContext = resolveReadBranchContext(projectId, branchId, currentUserId);
        ProjectSnapshotItem snapshotItem = requireSnapshotItemInBranchView(branchContext, fileId);
        ProjectFileVersion version = requireVersionForSnapshotItem(snapshotItem);
        return loadFileResource(version.getServerPath());
    }

    @Override
    @Transactional
    public Resource downloadFiles(Long projectId, List<Long> fileIds, Long currentUserId) {
        return downloadFiles(projectId, null, fileIds, currentUserId);
    }

    @Override
    @Transactional
    public Resource downloadFiles(Long projectId, Long branchId, List<Long> fileIds, Long currentUserId) {
        BranchReadContext branchContext = resolveReadBranchContext(projectId, branchId, currentUserId);
        List<ProjectSnapshotItem> snapshotItems = listSnapshotItems(branchContext);
        if (snapshotItems.isEmpty()) {
            throw new BusinessException("No downloadable files in current branch view");
        }

        List<ProjectSnapshotItem> downloadItems;
        if (fileIds == null || fileIds.isEmpty()) {
            downloadItems = snapshotItems;
        } else {
            Map<Long, ProjectSnapshotItem> byFileId = new LinkedHashMap<>();
            for (ProjectSnapshotItem item : snapshotItems) {
                byFileId.put(item.getProjectFileId(), item);
            }
            List<Long> distinctIds = fileIds.stream().filter(Objects::nonNull).distinct().toList();
            downloadItems = new ArrayList<>(distinctIds.size());
            for (Long id : distinctIds) {
                ProjectSnapshotItem item = byFileId.get(id);
                if (item == null) {
                    throw new BusinessException("Some files are not present in selected branch view");
                }
                downloadItems.add(item);
            }
        }

        if (downloadItems.isEmpty()) {
            throw new BusinessException("No downloadable files in current branch view");
        }

        Map<Long, ProjectFileVersion> versionMap = resolveCurrentVersionMap(downloadItems);
        byte[] zipBytes = buildZipBytesFromSnapshotItems(downloadItems, versionMap);
        return new NamedByteArrayResource(zipBytes, "project-" + projectId + "-files.zip");
    }

    @Override
    @Transactional
    public ProjectFileVO setMainFile(Long fileId, Long currentUserId) {
        return setMainFile(fileId, null, currentUserId);
    }

    @Override
    @Transactional
    public ProjectFileVO setMainFile(Long fileId, Long branchId, Long currentUserId) {
        if (branchId == null) {
            throw new BusinessException("branchId is required for setMainFile");
        }
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectWritable(projectFile.getProjectId(), currentUserId);

        BranchReadContext defaultBranchContext = resolveReadBranchContext(projectFile.getProjectId(), null, currentUserId);
        if (!Objects.equals(branchId, defaultBranchContext.branchId())) {
            throw new BusinessException("setMainFile is only allowed on default branch view");
        }
        requireSnapshotItemInBranchView(defaultBranchContext, fileId);

        clearProjectMainFile(projectFile.getProjectId());
        projectFile.setIsMain(true);
        ProjectFile saved = projectFileRepository.save(projectFile);

        return listFiles(projectFile.getProjectId(), defaultBranchContext.branchId(), currentUserId)
                .stream()
                .filter(item -> Objects.equals(item.getId(), fileId))
                .findFirst()
                .orElseGet(() -> toFileVO(saved));
    }

    @Override
    @Transactional
    public ProjectWorkspaceItemVO deleteFile(Long fileId, Long branchId, Long currentUserId) {
        ProjectFile projectFile = getProjectFile(fileId);
        projectPermissionService.assertProjectWritable(projectFile.getProjectId(), currentUserId);
        Long resolvedBranchId = requireWriteBranchId(projectFile.getProjectId(), branchId, currentUserId);
        BranchReadContext branchContext = resolveReadBranchContext(projectFile.getProjectId(), resolvedBranchId, currentUserId);
        ProjectSnapshotItem snapshotItem = requireSnapshotItemInBranchView(branchContext, fileId);
        return projectWorkspaceService.stageDelete(
                projectFile.getProjectId(),
                resolvedBranchId,
                currentUserId,
                ProjectPathUtils.normalize(snapshotItem.getCanonicalPath())
        );
    }

    private Long requireWriteBranchId(Long projectId, Long branchId, Long currentUserId) {
        if (branchId == null) {
            throw new BusinessException("branchId is required for write operations");
        }
        return resolveReadBranchContext(projectId, branchId, currentUserId).branchId();
    }

    private BranchReadContext resolveReadBranchContext(Long projectId, Long branchId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);

        ProjectCodeRepository repository = projectCodeRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("Project repository not found"));
        projectRepositoryBootstrapSupport.ensureRepositorySnapshotInitialized(repository, currentUserId);

        Long defaultBranchId = repository.getDefaultBranchId();
        Long resolvedBranchId = branchId != null ? branchId : defaultBranchId;
        if (resolvedBranchId == null) {
            throw new BusinessException("Default branch not found for project repository");
        }

        ProjectBranch branch = projectBranchRepository.findById(resolvedBranchId)
                .orElseThrow(() -> new BusinessException("Branch not found"));
        if (!Objects.equals(branch.getRepositoryId(), repository.getId())) {
            throw new BusinessException("Branch does not belong to current project repository");
        }

        Long headCommitId = branch.getHeadCommitId();
        if (headCommitId == null) {
            return new BranchReadContext(
                    projectId,
                    repository.getId(),
                    branch.getId(),
                    defaultBranchId,
                    null,
                    null,
                    Objects.equals(branch.getId(), defaultBranchId)
            );
        }

        ProjectCommit headCommit = projectCommitRepository.findById(headCommitId)
                .orElseThrow(() -> new BusinessException("Branch head commit not found"));
        if (!Objects.equals(headCommit.getRepositoryId(), repository.getId())) {
            throw new BusinessException("Branch head commit does not belong to repository");
        }

        Long snapshotId = headCommit.getSnapshotId();
        if (snapshotId != null && projectSnapshotRepository.findById(snapshotId).isEmpty()) {
            throw new BusinessException("Head commit snapshot not found");
        }

        return new BranchReadContext(
                projectId,
                repository.getId(),
                branch.getId(),
                defaultBranchId,
                headCommit.getId(),
                snapshotId,
                Objects.equals(branch.getId(), defaultBranchId)
        );
    }

    private Long resolveProjectIdByFileId(Long fileId) {
        return getProjectFile(fileId).getProjectId();
    }

    private List<ProjectSnapshotItem> listSnapshotItems(BranchReadContext branchContext) {
        if (branchContext.snapshotId() == null) {
            return List.of();
        }
        return projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(branchContext.snapshotId());
    }

    private ProjectSnapshotItem requireSnapshotItemInBranchView(BranchReadContext branchContext, Long fileId) {
        if (branchContext.snapshotId() == null) {
            throw new BusinessException("File is not present in current branch view (branchId absent means default branch view)");
        }
        return projectSnapshotItemRepository.findBySnapshotIdAndProjectFileId(branchContext.snapshotId(), fileId)
                .orElseThrow(() -> new BusinessException("File is not present in current branch view (branchId absent means default branch view)"));
    }

    private ProjectFileVersion requireVersionForSnapshotItem(ProjectSnapshotItem snapshotItem) {
        Long versionId = snapshotItem == null ? null : snapshotItem.getProjectFileVersionId();
        if (versionId == null) {
            throw new BusinessException("Snapshot item does not have file version");
        }
        return projectFileVersionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException("Snapshot item file version not found"));
    }

    private Resource loadFileResource(String serverPath) {
        if (!StringUtils.hasText(serverPath)) {
            throw new BusinessException("File path is empty");
        }
        Resource resource = fileStorageService.loadAsResource(serverPath);
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            throw new BusinessException("File does not exist or is not readable");
        }
        return resource;
    }

    private Map<Long, ProjectFileVersion> resolveCurrentVersionMap(Collection<ProjectSnapshotItem> snapshotItems) {
        Map<Long, ProjectFileVersion> versionMap = new HashMap<>();
        if (snapshotItems == null || snapshotItems.isEmpty()) {
            return versionMap;
        }
        List<Long> versionIds = snapshotItems.stream()
                .map(ProjectSnapshotItem::getProjectFileVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (versionIds.isEmpty()) {
            return versionMap;
        }
        for (ProjectFileVersion version : projectFileVersionRepository.findAllById(versionIds)) {
            versionMap.put(version.getId(), version);
        }
        return versionMap;
    }

    private Map<Long, List<ProjectFileVersionVO>> buildReachableVersionMapForSnapshot(BranchReadContext branchContext,
                                                                                       List<ProjectSnapshotItem> snapshotItems) {
        Map<Long, List<ProjectFileVersionVO>> grouped = new LinkedHashMap<>();
        if (snapshotItems == null || snapshotItems.isEmpty()) {
            return grouped;
        }
        List<Long> fileIds = snapshotItems.stream()
                .map(ProjectSnapshotItem::getProjectFileId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (fileIds.isEmpty()) {
            return grouped;
        }

        Set<Long> reachableCommitIds = resolveReachableCommitIds(branchContext);
        Set<Long> currentVersionIds = snapshotItems.stream()
                .map(ProjectSnapshotItem::getProjectFileVersionId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        for (ProjectFileVersion version : projectFileVersionRepository.findByFileIdInOrderByUploadedAtDesc(fileIds)) {
            if (!isVersionReachableFromHead(version, currentVersionIds, reachableCommitIds)) {
                continue;
            }
            grouped.computeIfAbsent(version.getFileId(), ignored -> new ArrayList<>())
                    .add(ProjectVoMapper.toProjectFileVersionVO(version));
        }
        return grouped;
    }

    private boolean isVersionReachableFromHead(ProjectFileVersion version,
                                               Long currentVersionId,
                                               Set<Long> reachableCommitIds) {
        if (version == null) {
            return false;
        }
        if (version.getId() != null && Objects.equals(version.getId(), currentVersionId)) {
            return true;
        }
        Long commitId = version.getCommitId();
        return commitId != null && reachableCommitIds.contains(commitId);
    }

    private boolean isVersionReachableFromHead(ProjectFileVersion version,
                                               Set<Long> currentVersionIds,
                                               Set<Long> reachableCommitIds) {
        if (version == null) {
            return false;
        }
        if (version.getId() != null && currentVersionIds.contains(version.getId())) {
            return true;
        }
        Long commitId = version.getCommitId();
        return commitId != null && reachableCommitIds.contains(commitId);
    }

    private Set<Long> resolveReachableCommitIds(BranchReadContext branchContext) {
        Long headCommitId = branchContext.headCommitId();
        if (headCommitId == null) {
            return Set.of();
        }

        Set<Long> visited = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(headCommitId);
        while (!queue.isEmpty()) {
            List<Long> batch = new ArrayList<>(256);
            while (!queue.isEmpty() && batch.size() < 256) {
                Long commitId = queue.poll();
                if (commitId == null || !visited.add(commitId)) {
                    continue;
                }
                batch.add(commitId);
            }
            if (batch.isEmpty()) {
                continue;
            }
            for (ProjectCommitParent parent : projectCommitParentRepository.findByCommitIdIn(batch)) {
                if (parent != null && parent.getParentCommitId() != null && !visited.contains(parent.getParentCommitId())) {
                    queue.add(parent.getParentCommitId());
                }
            }
        }

        Set<Long> scoped = new LinkedHashSet<>();
        for (ProjectCommit commit : projectCommitRepository.findAllById(visited)) {
            if (commit != null && Objects.equals(commit.getRepositoryId(), branchContext.repositoryId())) {
                scoped.add(commit.getId());
            }
        }
        return scoped;
    }

    private String resolveUploadPath(String canonicalPath, MultipartFile file) {
        if (StringUtils.hasText(canonicalPath)) {
            return ProjectPathUtils.normalize(canonicalPath);
        }
        String originalFilename = file == null ? null : file.getOriginalFilename();
        String fileName = StringUtils.hasText(originalFilename)
                ? originalFilename.replace('\\', '/').trim()
                : "unnamed-file";
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        }
        return ProjectPathUtils.normalize("/" + fileName);
    }

    private byte[] buildZipBytesFromSnapshotItems(List<ProjectSnapshotItem> items, Map<Long, ProjectFileVersion> versionMap) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos)) {
            Set<String> usedNames = new HashSet<>();
            int writtenCount = 0;
            for (ProjectSnapshotItem item : items) {
                ProjectFileVersion version = versionMap.get(item.getProjectFileVersionId());
                if (version == null || !StringUtils.hasText(version.getServerPath())) {
                    continue;
                }
                Resource resource = fileStorageService.loadAsResource(version.getServerPath());
                if (resource == null || !resource.exists()) {
                    continue;
                }
                String entryName = buildUniqueEntryName(
                        usedNames,
                        normalizeZipEntryName(item.getCanonicalPath()),
                        version.getVersion(),
                        item.getProjectFileId()
                );
                zos.putNextEntry(new ZipEntry(entryName));
                try (InputStream inputStream = resource.getInputStream()) {
                    inputStream.transferTo(zos);
                }
                zos.closeEntry();
                writtenCount++;
            }
            if (writtenCount == 0) {
                throw new BusinessException("No files can be packaged for download");
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Batch download packaging failed: " + e.getMessage());
        }
    }

    private String normalizeZipEntryName(String canonicalPath) {
        if (!StringUtils.hasText(canonicalPath)) {
            return "file";
        }
        String normalized = canonicalPath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return StringUtils.hasText(normalized) ? normalized : "file";
    }

    private String buildUniqueEntryName(Set<String> usedNames, String fileName, String version, Long fileId) {
        String safeName = StringUtils.hasText(fileName) ? fileName : "file-" + fileId;
        if (usedNames.add(safeName)) {
            return safeName;
        }
        String extension = StringUtils.getFilenameExtension(safeName);
        String ext = StringUtils.hasText(extension) ? "." + extension : "";
        String base = StringUtils.hasText(extension) ? safeName.substring(0, safeName.length() - ext.length()) : safeName;
        String suffix = StringUtils.hasText(version) ? "-v" + version : "";
        String candidate = base + suffix + ext;
        int index = 1;
        while (!usedNames.add(candidate)) {
            candidate = base + suffix + "-" + index + ext;
            index++;
        }
        return candidate;
    }

    private ProjectFile getProjectFile(Long fileId) {
        return projectFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("Project file not found"));
    }

    private void clearProjectMainFile(Long projectId) {
        List<ProjectFile> mainFiles = projectFileRepository.findByProjectIdAndIsMainTrue(projectId);
        for (ProjectFile mainFile : mainFiles) {
            mainFile.setIsMain(false);
            projectFileRepository.save(mainFile);
        }
    }

    private ProjectFileVO toFileVO(ProjectFile projectFile) {
        List<ProjectFileVersionVO> versions = projectFileVersionRepository.findByFileIdOrderByUploadedAtDesc(projectFile.getId())
                .stream()
                .map(ProjectVoMapper::toProjectFileVersionVO)
                .toList();
        return ProjectVoMapper.toProjectFileVO(projectFile, versions);
    }

    private record BranchReadContext(Long projectId,
                                     Long repositoryId,
                                     Long branchId,
                                     Long defaultBranchId,
                                     Long headCommitId,
                                     Long snapshotId,
                                     boolean defaultBranchView) {
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}

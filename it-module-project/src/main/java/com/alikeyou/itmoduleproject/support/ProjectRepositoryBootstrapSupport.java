package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshot;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProjectRepositoryBootstrapSupport {

    private static final String BOOTSTRAP_MESSAGE = "初始化仓库并接入现有项目文件";

    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectSnapshotRepository projectSnapshotRepository;
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectRepoStorageSupport projectRepoStorageSupport;

    @Transactional
    public void ensureRepositorySnapshotInitialized(ProjectCodeRepository repository, Long preferredOperatorId) {
        if (repository == null || repository.getId() == null || repository.getProjectId() == null || repository.getDefaultBranchId() == null) {
            return;
        }

        ProjectBranch branch = projectBranchRepository.findById(repository.getDefaultBranchId()).orElse(null);
        if (branch == null || !Objects.equals(branch.getRepositoryId(), repository.getId()) || branch.getHeadCommitId() == null) {
            return;
        }

        ProjectCommit headCommit = projectCommitRepository.findById(branch.getHeadCommitId()).orElse(null);
        if (headCommit == null || headCommit.getSnapshotId() == null) {
            return;
        }

        List<ProjectFile> activeFiles = projectFileRepository.findByProjectIdAndDeletedFlagFalseOrderByUploadTimeDesc(repository.getProjectId())
                .stream()
                .filter(this::isImportableFile)
                .toList();
        if (activeFiles.isEmpty()) {
            return;
        }

        List<ProjectSnapshotItem> existingSnapshotItems = projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(headCommit.getSnapshotId());
        Long operatorId = preferredOperatorId != null ? preferredOperatorId : repository.getCreatedBy();
        Set<String> usedPaths = new LinkedHashSet<>();
        List<ProjectSnapshotItem> snapshotItems = new ArrayList<>();
        for (ProjectSnapshotItem item : existingSnapshotItems) {
            if (item == null || !StringUtils.hasText(item.getCanonicalPath())) {
                continue;
            }
            usedPaths.add(item.getCanonicalPath());
        }
        Set<Long> trackedFileIds = existingSnapshotItems.stream()
                .map(ProjectSnapshotItem::getProjectFileId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> trackedPaths = existingSnapshotItems.stream()
                .map(ProjectSnapshotItem::getCanonicalPath)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        for (ProjectFile file : activeFiles) {
            String canonicalPath = resolveCanonicalPath(file, usedPaths);
            if (trackedFileIds.contains(file.getId()) || trackedPaths.contains(canonicalPath)) {
                continue;
            }
            ProjectBlob blob = projectRepoStorageSupport.saveExistingFile(file.getFilePath(), file.getFileName());
            ProjectFileVersion latestVersion = upsertLatestVersion(file, repository, headCommit, blob, canonicalPath, operatorId);

            file.setRepositoryId(repository.getId());
            file.setCanonicalPath(canonicalPath);
            file.setFileKey(canonicalPath);
            file.setFileName(ProjectPathUtils.extractFileName(canonicalPath));
            file.setFileType(ProjectFileTypeSupport.resolve(canonicalPath, file.getFileName()));
            file.setLatestBlobId(blob.getId());
            file.setLatestVersionId(latestVersion.getId());
            file.setLatestCommitId(headCommit.getId());
            file.setIsLatest(true);
            file.setDeletedFlag(false);
            file.setContentHash(blob.getSha256());
            projectFileRepository.save(file);

            snapshotItems.add(ProjectSnapshotItem.builder()
                    .snapshotId(headCommit.getSnapshotId())
                    .projectFileId(file.getId())
                    .projectFileVersionId(latestVersion.getId())
                    .blobId(blob.getId())
                    .canonicalPath(canonicalPath)
                    .contentHash(blob.getSha256())
                    .build());
            trackedFileIds.add(file.getId());
            trackedPaths.add(canonicalPath);
        }

        if (snapshotItems.isEmpty()) {
            return;
        }

        projectSnapshotItemRepository.saveAll(snapshotItems);

        ProjectSnapshot snapshot = projectSnapshotRepository.findById(headCommit.getSnapshotId()).orElse(null);
        if (snapshot != null) {
            snapshot.setCommitId(headCommit.getId());
            snapshot.setFileCount(existingSnapshotItems.size() + snapshotItems.size());
            projectSnapshotRepository.save(snapshot);
        }

        if (!StringUtils.hasText(headCommit.getMessage()) || "bootstrap repository".equalsIgnoreCase(headCommit.getMessage().trim())) {
            headCommit.setMessage(BOOTSTRAP_MESSAGE);
            projectCommitRepository.save(headCommit);
        }
    }

    private ProjectFileVersion upsertLatestVersion(ProjectFile file,
                                                   ProjectCodeRepository repository,
                                                   ProjectCommit headCommit,
                                                   ProjectBlob blob,
                                                   String canonicalPath,
                                                   Long operatorId) {
        ProjectFileVersion latestVersion = projectFileVersionRepository.findTopByFileIdOrderByUploadedAtDesc(file.getId()).orElse(null);
        if (latestVersion == null) {
            int nextVersionSeq = (int) projectFileVersionRepository.countByFileId(file.getId()) + 1;
            return projectFileVersionRepository.save(ProjectFileVersion.builder()
                    .fileId(file.getId())
                    .repositoryId(repository.getId())
                    .version(StringUtils.hasText(file.getVersion()) ? file.getVersion().trim() : "v" + nextVersionSeq)
                    .commitId(headCommit.getId())
                    .blobId(blob.getId())
                    .versionSeq(nextVersionSeq)
                    .contentHash(blob.getSha256())
                    .pathAtVersion(canonicalPath)
                    .changeType("ADD")
                    .serverPath(file.getFilePath())
                    .fileSizeBytes(file.getFileSizeBytes())
                    .uploadedBy(operatorId)
                    .commitMessage(BOOTSTRAP_MESSAGE)
                    .build());
        }

        latestVersion.setRepositoryId(repository.getId());
        if (latestVersion.getCommitId() == null) {
            latestVersion.setCommitId(headCommit.getId());
        }
        latestVersion.setBlobId(blob.getId());
        latestVersion.setContentHash(blob.getSha256());
        latestVersion.setPathAtVersion(canonicalPath);
        if (!StringUtils.hasText(latestVersion.getChangeType())) {
            latestVersion.setChangeType("ADD");
        }
        latestVersion.setServerPath(file.getFilePath());
        latestVersion.setFileSizeBytes(file.getFileSizeBytes());
        if (!StringUtils.hasText(latestVersion.getCommitMessage())) {
            latestVersion.setCommitMessage(BOOTSTRAP_MESSAGE);
        }
        return projectFileVersionRepository.save(latestVersion);
    }

    private boolean isImportableFile(ProjectFile file) {
        return file != null
                && !Boolean.TRUE.equals(file.getDeletedFlag())
                && !"folder".equalsIgnoreCase(String.valueOf(file.getFileType()))
                && StringUtils.hasText(file.getFilePath());
    }

    private String resolveCanonicalPath(ProjectFile file, Set<String> usedPaths) {
        String candidate;
        if (StringUtils.hasText(file.getCanonicalPath())) {
            candidate = ProjectPathUtils.normalize(file.getCanonicalPath());
        } else if (StringUtils.hasText(file.getFileName())) {
            candidate = ProjectPathUtils.normalize("/" + file.getFileName().replace('\\', '/'));
        } else {
            candidate = ProjectPathUtils.normalize("/file-" + file.getId());
        }
        if (usedPaths.add(candidate)) {
            return candidate;
        }

        String fileName = ProjectPathUtils.extractFileName(candidate);
        String extension = StringUtils.getFilenameExtension(fileName);
        String ext = StringUtils.hasText(extension) ? "." + extension.toLowerCase(Locale.ROOT) : "";
        String baseName = StringUtils.hasText(extension)
                ? fileName.substring(0, fileName.length() - ext.length())
                : fileName;
        String prefix = candidate.substring(0, candidate.length() - fileName.length());
        String deduplicated = ProjectPathUtils.normalize(prefix + baseName + "-" + file.getId() + ext);
        usedPaths.add(deduplicated);
        return deduplicated;
    }
}

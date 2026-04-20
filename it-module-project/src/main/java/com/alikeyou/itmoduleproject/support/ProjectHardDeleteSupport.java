package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectRelease;
import com.alikeyou.itmoduleproject.entity.ProjectWorkspace;
import com.alikeyou.itmoduleproject.entity.ProjectWorkspaceItem;
import com.alikeyou.itmoduleproject.repository.ProjectBlobRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitChangeRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectReleaseRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectWorkspaceItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectWorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ProjectHardDeleteSupport {

    private static final Logger log = LoggerFactory.getLogger(ProjectHardDeleteSupport.class);

    private final ProjectRepository projectRepository;
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileVersionRepository projectFileVersionRepository;
    private final ProjectReleaseRepository projectReleaseRepository;
    private final ProjectWorkspaceRepository projectWorkspaceRepository;
    private final ProjectWorkspaceItemRepository projectWorkspaceItemRepository;
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository;
    private final ProjectCommitChangeRepository projectCommitChangeRepository;
    private final ProjectBlobRepository projectBlobRepository;

    @Value("${project.storage.root:uploads/project}")
    private String projectStorageRoot;

    @Transactional
    public void hardDeleteProject(Long projectId) {
        Set<Long> candidateBlobIds = collectCandidateBlobIds(projectId);
        projectRepository.deleteById(projectId);
        projectRepository.flush();

        cleanupProjectStorageDirectory(projectId);
        cleanupOrphanBlobs(candidateBlobIds);
    }

    private Set<Long> collectCandidateBlobIds(Long projectId) {
        Set<Long> blobIds = new LinkedHashSet<>();

        List<ProjectFile> files = projectFileRepository.findByProjectIdOrderByUploadTimeDesc(projectId);
        for (ProjectFile file : files) {
            if (file.getLatestBlobId() != null) {
                blobIds.add(file.getLatestBlobId());
            }
        }

        List<Long> fileIds = files.stream()
                .map(ProjectFile::getId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (!fileIds.isEmpty()) {
            for (ProjectFileVersion version : projectFileVersionRepository.findByFileIdInOrderByUploadedAtDesc(fileIds)) {
                if (version.getBlobId() != null) {
                    blobIds.add(version.getBlobId());
                }
            }
        }

        for (ProjectRelease release : projectReleaseRepository.findByProjectIdOrderByCreatedAtDesc(projectId)) {
            if (release.getPackageBlobId() != null) {
                blobIds.add(release.getPackageBlobId());
            }
        }

        ProjectCodeRepository repository = projectCodeRepositoryRepository.findByProjectId(projectId).orElse(null);
        if (repository != null && repository.getId() != null) {
            List<Long> workspaceIds = projectWorkspaceRepository.findByRepositoryId(repository.getId())
                    .stream()
                    .map(ProjectWorkspace::getId)
                    .filter(id -> id != null)
                    .toList();
            if (!workspaceIds.isEmpty()) {
                for (ProjectWorkspaceItem item : projectWorkspaceItemRepository.findByWorkspaceIdIn(workspaceIds)) {
                    if (item.getBlobId() != null) {
                        blobIds.add(item.getBlobId());
                    }
                }
            }
        }
        return blobIds;
    }

    private void cleanupOrphanBlobs(Set<Long> blobIds) {
        if (blobIds == null || blobIds.isEmpty()) {
            return;
        }
        for (Long blobId : blobIds) {
            if (blobId == null || isBlobStillReferenced(blobId)) {
                continue;
            }
            projectBlobRepository.findById(blobId).ifPresent(this::deleteBlobSafely);
        }
    }

    private boolean isBlobStillReferenced(Long blobId) {
        return projectFileRepository.existsByLatestBlobId(blobId)
                || projectFileVersionRepository.existsByBlobId(blobId)
                || projectSnapshotItemRepository.existsByBlobId(blobId)
                || projectWorkspaceItemRepository.existsByBlobId(blobId)
                || projectCommitChangeRepository.existsByOldBlobIdOrNewBlobId(blobId, blobId)
                || projectReleaseRepository.existsByPackageBlobId(blobId);
    }

    private void deleteBlobSafely(ProjectBlob blob) {
        if (blob == null || blob.getId() == null) {
            return;
        }
        String storagePath = blob.getStoragePath();
        if (StringUtils.hasText(storagePath)) {
            try {
                Files.deleteIfExists(Paths.get(storagePath));
            } catch (IOException e) {
                log.warn("failed to delete blob file path={}, blobId={}", storagePath, blob.getId(), e);
            }
        }
        try {
            projectBlobRepository.delete(blob);
            projectBlobRepository.flush();
        } catch (DataIntegrityViolationException e) {
            log.debug("skip deleting referenced blob blobId={}", blob.getId(), e);
        }
    }

    private void cleanupProjectStorageDirectory(Long projectId) {
        if (!StringUtils.hasText(projectStorageRoot) || projectId == null) {
            return;
        }
        Path root = Paths.get(projectStorageRoot).toAbsolutePath().normalize();
        Path projectDir = root.resolve(String.valueOf(projectId)).normalize();
        if (!projectDir.startsWith(root) || Files.notExists(projectDir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(projectDir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("failed to delete project storage path={}", path, e);
                }
            });
        } catch (IOException e) {
            log.warn("failed to cleanup project storage directory projectId={}, path={}", projectId, projectDir, e);
        }
    }
}


package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectSizeSyncService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;

    @Transactional
    public void syncProjectSize(Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return;
        }
        long totalBytes = projectFileRepository.findByProjectIdOrderByUploadTimeDesc(projectId)
            .stream()
            .map(ProjectFile::getFileSizeBytes)
            .filter(Objects::nonNull)
            .mapToLong(Long::longValue)
            .sum();
        BigDecimal sizeMb = BigDecimal.valueOf(totalBytes)
            .divide(BigDecimal.valueOf(1024 * 1024L), 2, RoundingMode.HALF_UP);
        project.setSizeMb(sizeMb);
        projectRepository.save(project);
    }
}

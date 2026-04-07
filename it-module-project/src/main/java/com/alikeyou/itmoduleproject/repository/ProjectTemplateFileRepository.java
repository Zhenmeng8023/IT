package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectTemplateFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTemplateFileRepository extends JpaRepository<ProjectTemplateFile, Long> {

    List<ProjectTemplateFile> findByTemplateIdOrderBySortOrderAscIdAsc(Long templateId);

    List<ProjectTemplateFile> findByTemplateIdOrderByCreatedAtAsc(Long templateId);

    void deleteByTemplateId(Long templateId);
}

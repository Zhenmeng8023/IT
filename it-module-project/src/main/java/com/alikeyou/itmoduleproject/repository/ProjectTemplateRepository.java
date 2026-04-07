
package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, Long> {

    List<ProjectTemplate> findByCreatorIdOrderByUpdatedAtDesc(Long creatorId);

    List<ProjectTemplate> findByIsPublicTrueOrderByUpdatedAtDesc();

    List<ProjectTemplate> findByIsPublicTrueOrCreatorIdOrderByUpdatedAtDesc(Long creatorId);
}

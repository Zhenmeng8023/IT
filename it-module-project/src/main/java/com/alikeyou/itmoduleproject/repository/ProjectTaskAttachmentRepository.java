package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectTaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTaskAttachmentRepository extends JpaRepository<ProjectTaskAttachment, Long> {
    List<ProjectTaskAttachment> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<ProjectTaskAttachment> findByTaskIdInOrderByCreatedAtDesc(List<Long> taskIds);
}

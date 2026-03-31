package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectTaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTaskCommentRepository extends JpaRepository<ProjectTaskComment, Long> {
    List<ProjectTaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}

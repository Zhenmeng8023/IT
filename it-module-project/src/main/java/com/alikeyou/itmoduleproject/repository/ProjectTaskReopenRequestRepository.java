package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectTaskReopenRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectTaskReopenRequestRepository extends JpaRepository<ProjectTaskReopenRequest, Long> {
    List<ProjectTaskReopenRequest> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    Optional<ProjectTaskReopenRequest> findByIdAndTaskId(Long id, Long taskId);

    boolean existsByTaskIdAndStatus(Long taskId, String status);

    List<ProjectTaskReopenRequest> findByTaskIdAndStatusOrderByCreatedAtDesc(Long taskId, String status);
}

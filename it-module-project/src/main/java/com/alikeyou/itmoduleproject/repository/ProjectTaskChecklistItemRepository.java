package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectTaskChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTaskChecklistItemRepository extends JpaRepository<ProjectTaskChecklistItem, Long> {
    List<ProjectTaskChecklistItem> findByTaskIdOrderBySortOrderAscIdAsc(Long taskId);

    long countByTaskId(Long taskId);
}

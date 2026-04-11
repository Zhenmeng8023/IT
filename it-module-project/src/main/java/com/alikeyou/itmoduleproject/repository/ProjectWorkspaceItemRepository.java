package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectWorkspaceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectWorkspaceItemRepository extends JpaRepository<ProjectWorkspaceItem, Long> {
    List<ProjectWorkspaceItem> findByWorkspaceIdOrderByIdAsc(Long workspaceId);
    void deleteByWorkspaceId(Long workspaceId);
}

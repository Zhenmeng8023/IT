package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.ProjectWorkspaceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectWorkspaceItemRepository extends JpaRepository<ProjectWorkspaceItem, Long> {
    List<ProjectWorkspaceItem> findByWorkspaceIdOrderByIdAsc(Long workspaceId);
    Optional<ProjectWorkspaceItem> findFirstByWorkspaceIdAndCanonicalPath(Long workspaceId, String canonicalPath);
    void deleteByWorkspaceId(Long workspaceId);
}

package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    List<Project> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    @Query("""
            select distinct p
            from Project p
            left join ProjectMember pm on pm.projectId = p.id and pm.status = 'active'
            where p.authorId = :userId or pm.userId = :userId
            order by p.createdAt desc
            """)
    List<Project> findMyProjects(@Param("userId") Long userId);

    @Query("""
            select distinct p
            from Project p
            left join ProjectMember pm on pm.projectId = p.id and pm.status = 'active'
            where p.authorId = :userId or pm.userId = :userId
            """)
    Page<Project> findMyProjects(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select distinct p
            from Project p
            join ProjectMember pm on pm.projectId = p.id and pm.status = 'active'
            where pm.userId = :userId and p.authorId <> :userId
            """)
    Page<Project> findParticipatedProjects(@Param("userId") Long userId, Pageable pageable);
}

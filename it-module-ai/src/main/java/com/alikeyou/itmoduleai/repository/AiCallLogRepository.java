package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.AiCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {

    @Query("SELECT acl FROM AiCallLog acl WHERE acl.user.id = :userId")
    Page<AiCallLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // Deleted:Page<AiCallLog> findByProjectId(Long projectId, Pageable pageable);

    @Query("SELECT acl FROM AiCallLog acl WHERE acl.user.id = :userId AND acl.createdAt BETWEEN :start AND :end")
    List<AiCallLog> findUserLogsByDateRange(@Param("userId") Long userId,
                                            @Param("start") Instant start,
                                            @Param("end") Instant end);

    @Query("SELECT COUNT(acl) FROM AiCallLog acl WHERE acl.user.id = :userId AND acl.status = 'SUCCESS'")
    long countSuccessfulCallsByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(acl.totalTokens) FROM AiCallLog acl WHERE acl.user.id = :userId")
    Long sumTokensByUser(@Param("userId") Long userId);
}

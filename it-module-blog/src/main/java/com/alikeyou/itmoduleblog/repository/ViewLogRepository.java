package com.alikeyou.itmoduleblog.repository;

import com.alikeyou.itmoduleblog.entity.ViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ViewLogRepository extends JpaRepository<ViewLog, Long> {
    long countByUserId(Long userId);

    long countByTargetTypeAndCreatedAtGreaterThanEqual(String targetType, Instant createdAt);

    java.util.List<ViewLog> findByUserIdAndCreatedAtBetween(Long userId, java.time.Instant start, java.time.Instant end);

    java.util.Optional<ViewLog> findTopByUserIdAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long userId,
            String targetType,
            Long targetId,
            java.time.Instant createdAt
    );

    java.util.Optional<ViewLog> findTopByUserIdIsNullAndTargetTypeAndTargetIdAndIpAddressAndUserAgentAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String targetType,
            Long targetId,
            String ipAddress,
            String userAgent,
            java.time.Instant createdAt
    );
}

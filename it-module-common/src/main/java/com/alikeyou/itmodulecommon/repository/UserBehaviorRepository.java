package com.alikeyou.itmodulecommon.repository;

import com.alikeyou.itmodulecommon.entity.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {
    List<UserBehavior> findByUser_IdAndOccurredAtBetween(Long userId, Instant start, Instant end);
}

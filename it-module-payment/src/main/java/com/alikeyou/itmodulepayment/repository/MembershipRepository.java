package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findByUserId(Long userId);

    List<Membership> findByUserIdAndStatus(Long userId, String status);

    Optional<Membership> findTopByUserIdAndStatusAndEndTimeAfterOrderByEndTimeDesc(Long userId, String status, LocalDateTime now);

    List<Membership> findByUserIdAndStatusAndEndTimeBefore(Long userId, String status, LocalDateTime time);
}

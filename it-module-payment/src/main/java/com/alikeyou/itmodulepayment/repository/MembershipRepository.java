package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.Membership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findByUserId(Long userId);

    List<Membership> findByUserIdAndStatus(Long userId, String status);

    Optional<Membership> findTopByUserIdAndStatusAndEndTimeAfterOrderByEndTimeDesc(Long userId, String status, LocalDateTime now);

    List<Membership> findByUserIdAndStatusAndEndTimeBefore(Long userId, String status, LocalDateTime time);
    
    // 后台管理专用方法
    Page<Membership> findAll(Pageable pageable);
    Page<Membership> findByUserId(Long userId, Pageable pageable);
    Page<Membership> findByStatus(String status, Pageable pageable);
    List<Membership> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByStatus(String status);
    
    @Query("SELECT m.levelId, COUNT(m) FROM Membership m GROUP BY m.levelId")
    List<Object[]> countByLevelId();
}

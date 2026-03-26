package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.CreatorWithdrawRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreatorWithdrawRequestRepository extends JpaRepository<CreatorWithdrawRequest, Long> {

    Page<CreatorWithdrawRequest> findByCreatorIdOrderByCreatedAtDesc(Long creatorId, Pageable pageable);

    boolean existsByRequestNo(String requestNo);
}

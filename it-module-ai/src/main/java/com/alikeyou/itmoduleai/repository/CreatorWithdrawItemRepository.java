package com.alikeyou.itmoduleai.repository;

import com.alikeyou.itmoduleai.entity.CreatorWithdrawItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreatorWithdrawItemRepository extends JpaRepository<CreatorWithdrawItem, Long> {

    List<CreatorWithdrawItem> findByWithdrawRequest_IdOrderByIdAsc(Long withdrawRequestId);

    boolean existsByRevenueRecordId(Long revenueRecordId);
}

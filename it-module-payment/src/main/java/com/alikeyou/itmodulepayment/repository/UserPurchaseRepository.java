package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.UserPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPurchaseRepository extends JpaRepository<UserPurchase, Long> {

    Optional<UserPurchase> findByUserIdAndPaidContentId(Long userId, Long paidContentId);

    boolean existsByUserIdAndPaidContentId(Long userId, Long paidContentId);

    List<UserPurchase> findByUserId(Long userId);

    List<UserPurchase> findByPaidContentId(Long paidContentId);
}

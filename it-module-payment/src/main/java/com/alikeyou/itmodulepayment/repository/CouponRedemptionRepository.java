package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.CouponRedemption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 优惠券核销记录 Repository
 */
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    /**
     * 根据用户ID查询核销记录
     */
    List<CouponRedemption> findByUserId(Long userId);

    /**
     * 分页查询用户的核销记录
     */
    Page<CouponRedemption> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据订单ID查询核销记录
     */
    Optional<CouponRedemption> findByOrderId(Long orderId);

    /**
     * 根据用户优惠券ID查询核销记录
     */
    Optional<CouponRedemption> findByUserCouponId(Long userCouponId);

    /**
     * 查询成功的核销记录
     */
    List<CouponRedemption> findByUserIdAndStatus(Long userId, CouponRedemption.RedemptionStatus status);

    /**
     * 统计用户的优惠总金额
     */
    @Query("SELECT SUM(cr.discountAmount) FROM CouponRedemption cr WHERE cr.userId = :userId AND cr.status = 'SUCCESS'")
    Double sumDiscountAmountByUserId(Long userId);

    /**
     * 分页查询所有核销记录
     */
    Page<CouponRedemption> findAll(Pageable pageable);
}

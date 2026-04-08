package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 优惠券模板 Repository
 */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * 根据优惠券码查询
     */
    Optional<Coupon> findByCode(String code);

    /**
     * 查询所有启用的优惠券
     */
    List<Coupon> findByIsEnabledTrue();

    /**
     * 分页查询所有优惠券
     */
    Page<Coupon> findAll(Pageable pageable);

    /**
     * 根据启用状态分页查询
     */
    Page<Coupon> findByIsEnabled(Boolean isEnabled, Pageable pageable);

    /**
     * 查询在指定时间范围内有效的优惠券
     */
    @Query("SELECT c FROM Coupon c WHERE c.isEnabled = true AND c.startTime <= :now AND c.endTime >= :now")
    List<Coupon> findValidCoupons(LocalDateTime now);

    /**
     * 统计已发放的优惠券数量（通过user_coupon表）
     */
    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.couponId = :couponId")
    long countIssuedCoupons(Long couponId);
}

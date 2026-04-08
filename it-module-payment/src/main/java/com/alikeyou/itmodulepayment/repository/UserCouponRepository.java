package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.UserCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户优惠券 Repository
 */
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    /**
     * 查询用户的所有优惠券
     */
    List<UserCoupon> findByUserId(Long userId);

    /**
     * 查询用户的指定状态的优惠券
     */
    List<UserCoupon> findByUserIdAndReceiveStatus(Long userId, UserCoupon.ReceiveStatus status);

    /**
     * 分页查询用户的优惠券
     */
    Page<UserCoupon> findByUserId(Long userId, Pageable pageable);

    /**
     * 分页查询用户的指定状态的优惠券
     */
    Page<UserCoupon> findByUserIdAndReceiveStatus(Long userId, UserCoupon.ReceiveStatus status, Pageable pageable);

    /**
     * 查询用户可用的优惠券（已领取且未过期）
     */
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.receiveStatus = 'RECEIVED' AND (uc.endTime IS NULL OR uc.endTime > :now)")
    List<UserCoupon> findAvailableUserCoupons(Long userId, LocalDateTime now);

    /**
     * 查询用户已过期的优惠券
     */
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.receiveStatus = 'RECEIVED' AND uc.endTime < :now")
    List<UserCoupon> findExpiredUserCoupons(Long userId, LocalDateTime now);

    /**
     * 统计用户在某个优惠券上的使用次数
     */
    long countByUserIdAndCouponIdAndReceiveStatus(Long userId, Long couponId, UserCoupon.ReceiveStatus status);

    /**
     * 查询锁定中的优惠券
     */
    List<UserCoupon> findByUserIdAndReceiveStatusAndOrderIdNotNull(Long userId, UserCoupon.ReceiveStatus status);

    /**
     * 根据订单ID查询使用的优惠券
     */
    Optional<UserCoupon> findByOrderId(Long orderId);
}

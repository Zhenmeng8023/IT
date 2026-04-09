package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.UserCouponDTO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 用户优惠券服务接口
 */
public interface UserCouponService {

    /**
     * 发放优惠券给用户
     */
    UserCouponDTO issueCoupon(Long couponId, Long userId, String sourceType, String remark);

    /**
     * 批量发放优惠券
     */
    List<UserCouponDTO> batchIssueCoupons(Long couponId, List<Long> userIds, String sourceType, String remark);

    /**
     * 领取优惠券（通过兑换码）
     */
    UserCouponDTO redeemCoupon(String couponCode, Long userId);

    /**
     * 查询用户的优惠券列表
     */
    List<UserCouponDTO> getUserCoupons(Long userId);

    /**
     * 根据ID查询用户优惠券
     */
    UserCouponDTO getUserCouponById(Long userCouponId);

    /**
     * 分页查询用户的优惠券
     */
    Page<UserCouponDTO> getUserCouponsPaged(Long userId, int page, int size);

    /**
     * 查询用户可用的优惠券
     */
    List<UserCouponDTO> getAvailableCoupons(Long userId);

    /**
     * 查询用户已过期的优惠券
     */
    List<UserCouponDTO> getExpiredCoupons(Long userId);

    /**
     * 锁定优惠券（下单时使用）
     */
    UserCouponDTO lockCoupon(Long userCouponId, Long orderId);

    /**
     * 使用优惠券（支付成功后）
     */
    void useCoupon(Long userCouponId, Long orderId);

    /**
     * 释放锁定的优惠券（订单取消时）
     */
    void releaseCoupon(Long userCouponId);

    /**
     * 作废优惠券
     */
    void voidCoupon(Long userCouponId, String reason);

    /**
     * 检查用户是否可以领取某个优惠券
     */
    boolean canUserReceiveCoupon(Long couponId, Long userId);

    /**
     * 自动过期优惠券
     */
    void expireOverdueCoupons();
}

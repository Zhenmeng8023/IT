package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.CouponRedemptionDTO;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券核销服务接口
 */
public interface CouponRedemptionService {

    /**
     * 创建核销记录（锁定优惠券）
     */
    CouponRedemptionDTO createRedemption(Long userCouponId, Long orderId, BigDecimal originalAmount);

    /**
     * 确认核销成功（支付成功后）
     */
    CouponRedemptionDTO confirmRedemption(Long redemptionId);

    /**
     * 取消核销（订单取消时）
     */
    CouponRedemptionDTO cancelRedemption(Long redemptionId);

    /**
     * 回滚核销（退款时）
     */
    CouponRedemptionDTO rollbackRedemption(Long redemptionId, String reason);

    /**
     * 根据订单ID查询核销记录
     */
    CouponRedemptionDTO getRedemptionByOrderId(Long orderId);

    /**
     * 查询用户的核销记录
     */
    List<CouponRedemptionDTO> getUserRedemptions(Long userId);

    /**
     * 分页查询用户的核销记录
     */
    Page<CouponRedemptionDTO> getUserRedemptionsPaged(Long userId, int page, int size);

    /**
     * 统计用户优惠总金额
     */
    Double getUserTotalDiscount(Long userId);
}

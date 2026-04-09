package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.CouponRedemptionDTO;
import com.alikeyou.itmodulepayment.entity.Coupon;
import com.alikeyou.itmodulepayment.entity.CouponRedemption;
import com.alikeyou.itmodulepayment.entity.UserCoupon;
import com.alikeyou.itmodulepayment.repository.CouponRedemptionRepository;
import com.alikeyou.itmodulepayment.repository.CouponRepository;
import com.alikeyou.itmodulepayment.repository.UserCouponRepository;
import com.alikeyou.itmodulepayment.service.CouponRedemptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券核销服务实现类
 */
@Service
@Transactional
public class CouponRedemptionServiceImpl implements CouponRedemptionService {

    private static final Logger logger = LoggerFactory.getLogger(CouponRedemptionServiceImpl.class);

    private final CouponRedemptionRepository redemptionRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    public CouponRedemptionServiceImpl(CouponRedemptionRepository redemptionRepository,
                                       UserCouponRepository userCouponRepository,
                                       CouponRepository couponRepository) {
        this.redemptionRepository = redemptionRepository;
        this.userCouponRepository = userCouponRepository;
        this.couponRepository = couponRepository;
    }

    @Override
    public CouponRedemptionDTO createRedemption(Long userCouponId, Long orderId, BigDecimal originalAmount) {
        logger.info("创建优惠券核销记录: userCouponId={}, orderId={}, amount={}", 
                userCouponId, orderId, originalAmount);

        // 查询用户优惠券
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("用户优惠券不存在: " + userCouponId));

        if (userCoupon.getReceiveStatus() != UserCoupon.ReceiveStatus.locked) {
            throw new IllegalStateException("优惠券状态不正确，无法核销: " + userCoupon.getReceiveStatus());
        }

        // 查询优惠券模板
        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new IllegalArgumentException("优惠券模板不存在"));

        // 计算优惠金额
        BigDecimal discountAmount;
        if (coupon.getType() == Coupon.CouponType.discount) {
            // 折扣券
            BigDecimal discountRate = coupon.getValue().divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP);
            discountAmount = originalAmount.multiply(discountRate);
        } else {
            // 现金减免券
            discountAmount = coupon.getValue();
        }

        // 确保优惠金额不超过订单金额
        if (discountAmount.compareTo(originalAmount) > 0) {
            discountAmount = originalAmount;
        }

        BigDecimal finalAmount = originalAmount.subtract(discountAmount);

        // 创建核销记录
        CouponRedemption redemption = new CouponRedemption();
        redemption.setUserCouponId(userCouponId);
        redemption.setCouponId(userCoupon.getCouponId());
        redemption.setUserId(userCoupon.getUserId());
        redemption.setOrderId(orderId);
        redemption.setOriginalAmount(originalAmount);
        redemption.setDiscountAmount(discountAmount);
        redemption.setFinalAmount(finalAmount);
        redemption.setStatus(CouponRedemption.RedemptionStatus.SUCCESS);
        redemption.setRedeemedAt(LocalDateTime.now());
        redemption.setCreatedAt(LocalDateTime.now());
        redemption.setUpdatedAt(LocalDateTime.now());

        CouponRedemption saved = redemptionRepository.save(redemption);
        logger.info("优惠券核销记录创建成功: redemptionId={}, discountAmount={}", 
                saved.getId(), discountAmount);

        return convertToDTO(saved, coupon.getName());
    }

    @Override
    public CouponRedemptionDTO confirmRedemption(Long redemptionId) {
        logger.info("确认核销成功: redemptionId={}", redemptionId);

        CouponRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new IllegalArgumentException("核销记录不存在: " + redemptionId));

        if (redemption.getStatus() != CouponRedemption.RedemptionStatus.LOCKED) {
            throw new IllegalStateException("核销记录状态不正确: " + redemption.getStatus());
        }

        redemption.setStatus(CouponRedemption.RedemptionStatus.SUCCESS);
        redemption.setUpdatedAt(LocalDateTime.now());

        CouponRedemption updated = redemptionRepository.save(redemption);
        logger.info("核销确认成功: redemptionId={}", updated.getId());

        return convertToDTO(updated, null);
    }

    @Override
    public CouponRedemptionDTO cancelRedemption(Long redemptionId) {
        logger.info("取消核销: redemptionId={}", redemptionId);

        CouponRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new IllegalArgumentException("核销记录不存在: " + redemptionId));

        if (redemption.getStatus() == CouponRedemption.RedemptionStatus.SUCCESS) {
            throw new IllegalStateException("核销已成功，无法取消");
        }

        redemption.setStatus(CouponRedemption.RedemptionStatus.CANCELLED);
        redemption.setUpdatedAt(LocalDateTime.now());

        CouponRedemption updated = redemptionRepository.save(redemption);

        // 释放优惠券
        userCouponRepository.findById(redemption.getUserCouponId()).ifPresent(userCoupon -> {
            userCoupon.setReceiveStatus(UserCoupon.ReceiveStatus.received);
            userCoupon.setOrderId(null);
            userCoupon.setUpdatedAt(LocalDateTime.now());
            userCouponRepository.save(userCoupon);
        });

        logger.info("核销取消成功: redemptionId={}", updated.getId());
        return convertToDTO(updated, null);
    }

    @Override
    public CouponRedemptionDTO rollbackRedemption(Long redemptionId, String reason) {
        logger.info("回滚核销: redemptionId={}, 原因: {}", redemptionId, reason);

        CouponRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new IllegalArgumentException("核销记录不存在: " + redemptionId));

        if (redemption.getStatus() != CouponRedemption.RedemptionStatus.SUCCESS) {
            throw new IllegalStateException("只有成功的核销才能回滚");
        }

        redemption.setStatus(CouponRedemption.RedemptionStatus.ROLLBACK);
        redemption.setRollbackAt(LocalDateTime.now());
        redemption.setRemark(reason);
        redemption.setUpdatedAt(LocalDateTime.now());

        CouponRedemption updated = redemptionRepository.save(redemption);

        // 恢复优惠券状态
        userCouponRepository.findById(redemption.getUserCouponId()).ifPresent(userCoupon -> {
            userCoupon.setReceiveStatus(UserCoupon.ReceiveStatus.received);
            userCoupon.setOrderId(null);
            userCoupon.setUsedAt(null);
            userCoupon.setUpdatedAt(LocalDateTime.now());
            userCouponRepository.save(userCoupon);
        });

        logger.info("核销回滚成功: redemptionId={}", updated.getId());
        return convertToDTO(updated, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponRedemptionDTO getRedemptionByOrderId(Long orderId) {
        CouponRedemption redemption = redemptionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单未使用优惠券: " + orderId));

        return convertToDTO(redemption, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponRedemptionDTO> getUserRedemptions(Long userId) {
        List<CouponRedemption> redemptions = redemptionRepository.findByUserId(userId);
        return redemptions.stream()
                .map(r -> convertToDTO(r, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CouponRedemptionDTO> getUserRedemptionsPaged(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "redeemedAt"));
        Page<CouponRedemption> redemptionPage = redemptionRepository.findByUserId(userId, pageable);

        return redemptionPage.map(r -> convertToDTO(r, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Double getUserTotalDiscount(Long userId) {
        Double totalDiscount = redemptionRepository.sumDiscountAmountByUserId(userId);
        return totalDiscount != null ? totalDiscount : 0.0;
    }

    /**
     * 将实体转换为DTO
     */
    private CouponRedemptionDTO convertToDTO(CouponRedemption redemption, String couponName) {
        CouponRedemptionDTO dto = new CouponRedemptionDTO();
        BeanUtils.copyProperties(redemption, dto);
        dto.setStatus(redemption.getStatus().name());

        // 如果未传入优惠券名称，则查询
        if (couponName == null) {
            couponRepository.findById(redemption.getCouponId())
                    .ifPresent(coupon -> dto.setCouponName(coupon.getName()));
        } else {
            dto.setCouponName(couponName);
        }

        return dto;
    }
}

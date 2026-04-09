package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.UserCouponDTO;
import com.alikeyou.itmodulepayment.entity.Coupon;
import com.alikeyou.itmodulepayment.entity.UserCoupon;
import com.alikeyou.itmodulepayment.repository.CouponRepository;
import com.alikeyou.itmodulepayment.repository.UserCouponRepository;
import com.alikeyou.itmodulepayment.service.UserCouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户优惠券服务实现类
 */
@Service
@Transactional
public class UserCouponServiceImpl implements UserCouponService {

    private static final Logger logger = LoggerFactory.getLogger(UserCouponServiceImpl.class);

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    public UserCouponServiceImpl(UserCouponRepository userCouponRepository, CouponRepository couponRepository) {
        this.userCouponRepository = userCouponRepository;
        this.couponRepository = couponRepository;
    }

    @Override
    public UserCouponDTO issueCoupon(Long couponId, Long userId, String sourceType, String remark) {
        logger.info("发放优惠券给用户: couponId={}, userId={}", couponId, userId);

        // 检查优惠券是否存在
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("优惠券不存在: " + couponId));

        // 检查用户是否可以领取
        if (!canUserReceiveCoupon(couponId, userId)) {
            throw new IllegalArgumentException("用户已达到该优惠券的领取上限");
        }

        // 创建用户优惠券记录
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setCouponId(couponId);
        userCoupon.setUserId(userId);
        userCoupon.setSourceType(UserCoupon.SourceType.valueOf(sourceType.toLowerCase()));
        userCoupon.setReceiveStatus(UserCoupon.ReceiveStatus.received);
        userCoupon.setReceivedAt(LocalDateTime.now());
        userCoupon.setStartTime(coupon.getStartTime());
        userCoupon.setEndTime(coupon.getEndTime());
        userCoupon.setRemark(remark);
        userCoupon.setCreatedAt(LocalDateTime.now());
        userCoupon.setUpdatedAt(LocalDateTime.now());

        UserCoupon saved = userCouponRepository.save(userCoupon);
        logger.info("优惠券发放成功: userCouponId={}", saved.getId());

        return convertToDTO(saved, coupon);
    }

    @Override
    public List<UserCouponDTO> batchIssueCoupons(Long couponId, List<Long> userIds, String sourceType, String remark) {
        logger.info("批量发放优惠券: couponId={}, 用户数={}", couponId, userIds.size());

        List<UserCouponDTO> results = new ArrayList<>();
        for (Long userId : userIds) {
            try {
                UserCouponDTO dto = issueCoupon(couponId, userId, sourceType, remark);
                results.add(dto);
            } catch (Exception e) {
                logger.warn("为用户 {} 发放优惠券失败: {}", userId, e.getMessage());
            }
        }

        logger.info("批量发放完成: 成功 {}/{}", results.size(), userIds.size());
        return results;
    }

    @Override
    public UserCouponDTO redeemCoupon(String couponCode, Long userId) {
        logger.info("用户领取优惠券: code={}, userId={}", couponCode, userId);

        // 根据优惠券码查找
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new IllegalArgumentException("优惠券码无效: " + couponCode));

        // 检查是否已领取过
        if (!canUserReceiveCoupon(coupon.getId(), userId)) {
            throw new IllegalArgumentException("您已领取过该优惠券或已达到领取上限");
        }

        return issueCoupon(coupon.getId(), userId, "EXCHANGE", "通过兑换码领取");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCouponDTO> getUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        return userCoupons.stream()
                .map(uc -> convertToDTO(uc, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserCouponDTO getUserCouponById(Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("用户优惠券不存在: " + userCouponId));
        return convertToDTO(userCoupon, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserCouponDTO> getUserCouponsPaged(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<UserCoupon> userCouponPage = userCouponRepository.findByUserId(userId, pageable);

        return userCouponPage.map(uc -> convertToDTO(uc, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCouponDTO> getAvailableCoupons(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> availableCoupons = userCouponRepository.findAvailableUserCoupons(userId, now);
        
        return availableCoupons.stream()
                .map(uc -> convertToDTO(uc, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCouponDTO> getExpiredCoupons(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> expiredCoupons = userCouponRepository.findExpiredUserCoupons(userId, now);
        
        return expiredCoupons.stream()
                .map(uc -> convertToDTO(uc, null))
                .collect(Collectors.toList());
    }

    @Override
    public UserCouponDTO lockCoupon(Long userCouponId, Long orderId) {
        logger.info("锁定优惠券: userCouponId={}, orderId={}", userCouponId, orderId);

        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("用户优惠券不存在: " + userCouponId));

        if (userCoupon.getReceiveStatus() != UserCoupon.ReceiveStatus.received) {
            throw new IllegalStateException("优惠券状态不正确，无法锁定: " + userCoupon.getReceiveStatus());
        }

        userCoupon.setReceiveStatus(UserCoupon.ReceiveStatus.locked);
        userCoupon.setOrderId(orderId);
        userCoupon.setUpdatedAt(LocalDateTime.now());

        UserCoupon updated = userCouponRepository.save(userCoupon);
        logger.info("优惠券锁定成功: userCouponId={}", updated.getId());

        return convertToDTO(updated, null);
    }

    @Override
    public void useCoupon(Long userCouponId, Long orderId) {
        logger.info("使用优惠券: userCouponId={}, orderId={}", userCouponId, orderId);

        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("用户优惠券不存在: " + userCouponId));

        if (userCoupon.getReceiveStatus() != UserCoupon.ReceiveStatus.locked) {
            throw new IllegalStateException("优惠券状态不正确，无法使用: " + userCoupon.getReceiveStatus());
        }

        userCoupon.setReceiveStatus(UserCoupon.ReceiveStatus.used);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCoupon.setOrderId(orderId);
        userCoupon.setUpdatedAt(LocalDateTime.now());

        userCouponRepository.save(userCoupon);
        logger.info("优惠券使用成功: userCouponId={}", userCouponId);
    }

    @Override
    public void releaseCoupon(Long userCouponId) {
        logger.info("释放优惠券: userCouponId={}", userCouponId);

        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("用户优惠券不存在: " + userCouponId));

        if (userCoupon.getReceiveStatus() != UserCoupon.ReceiveStatus.locked) {
            throw new IllegalStateException("优惠券状态不正确，无法释放: " + userCoupon.getReceiveStatus());
        }

        userCoupon.setReceiveStatus(UserCoupon.ReceiveStatus.received);
        userCoupon.setOrderId(null);
        userCoupon.setUpdatedAt(LocalDateTime.now());

        userCouponRepository.save(userCoupon);
        logger.info("优惠券释放成功: userCouponId={}", userCouponId);
    }

    @Override
    public void voidCoupon(Long userCouponId, String reason) {
        logger.info("作废优惠券: userCouponId={}, 原因: {}", userCouponId, reason);

        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new IllegalArgumentException("用户优惠券不存在: " + userCouponId));

        userCoupon.setReceiveStatus(UserCoupon.ReceiveStatus.voided);
        userCoupon.setRemark(reason);
        userCoupon.setUpdatedAt(LocalDateTime.now());

        userCouponRepository.save(userCoupon);
        logger.info("优惠券作废成功: userCouponId={}", userCouponId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserReceiveCoupon(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("优惠券不存在: " + couponId));

        // 检查用户已使用该优惠券的次数
        long usedCount = userCouponRepository.countByUserIdAndCouponIdAndReceiveStatus(
                userId, couponId, UserCoupon.ReceiveStatus.used);

        if (coupon.getUsageLimitPerUser() != null && usedCount >= coupon.getUsageLimitPerUser()) {
            return false;
        }

        return true;
    }

    @Override
    public void expireOverdueCoupons() {
        logger.info("开始执行优惠券过期任务");

        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> receivedCoupons = userCouponRepository.findByUserIdAndReceiveStatus(0L, UserCoupon.ReceiveStatus.received);

        int expiredCount = 0;
        for (UserCoupon userCoupon : receivedCoupons) {
            if (userCoupon.getEndTime() != null && userCoupon.getEndTime().isBefore(now)) {
                userCoupon.setReceiveStatus(UserCoupon.ReceiveStatus.expired);
                userCoupon.setUpdatedAt(LocalDateTime.now());
                userCouponRepository.save(userCoupon);
                expiredCount++;
            }
        }

        logger.info("优惠券过期任务完成: 过期数量={}", expiredCount);
    }

    /**
     * 将实体转换为DTO
     */
    private UserCouponDTO convertToDTO(UserCoupon userCoupon, Coupon coupon) {
        UserCouponDTO dto = new UserCouponDTO();
        BeanUtils.copyProperties(userCoupon, dto);
        dto.setSourceType(userCoupon.getSourceType().name());
        dto.setReceiveStatus(userCoupon.getReceiveStatus().name());

        // 如果未传入coupon对象，则查询
        if (coupon == null) {
            couponRepository.findById(userCoupon.getCouponId()).ifPresent(c -> {
                dto.setCouponName(c.getName());
                dto.setCouponCode(c.getCode());
                dto.setCouponType(c.getType().name());
                dto.setValue(c.getValue()); // 优惠值
                dto.setMinAmount(c.getMinAmount()); // 最低消费金额
            });
        } else {
            dto.setCouponName(coupon.getName());
            dto.setCouponCode(coupon.getCode());
            dto.setCouponType(coupon.getType().name());
            dto.setValue(coupon.getValue()); // 优惠值
            dto.setMinAmount(coupon.getMinAmount()); // 最低消费金额
        }

        return dto;
    }
}

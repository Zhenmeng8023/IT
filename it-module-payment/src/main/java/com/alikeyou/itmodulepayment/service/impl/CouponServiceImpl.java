package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.CouponDTO;
import com.alikeyou.itmodulepayment.entity.Coupon;
import com.alikeyou.itmodulepayment.repository.CouponRepository;
import com.alikeyou.itmodulepayment.repository.UserCouponRepository;
import com.alikeyou.itmodulepayment.service.CouponService;
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
 * 优惠券服务实现类
 */
@Service
@Transactional
public class CouponServiceImpl implements CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponServiceImpl.class);

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    public CouponServiceImpl(CouponRepository couponRepository, UserCouponRepository userCouponRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
    }

    @Override
    public CouponDTO createCoupon(CouponDTO couponDTO) {
        logger.info("创建优惠券: {}", couponDTO.getName());

        // 检查优惠券码是否已存在
        if (couponRepository.findByCode(couponDTO.getCode()).isPresent()) {
            throw new IllegalArgumentException("优惠券码已存在: " + couponDTO.getCode());
        }

        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(couponDTO, coupon);
        coupon.setType(parseCouponType(couponDTO.getType()));
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        coupon.setIsEnabled(couponDTO.getIsEnabled() != null ? couponDTO.getIsEnabled() : true);

        Coupon saved = couponRepository.save(coupon);
        logger.info("优惠券创建成功, ID: {}", saved.getId());

        return convertToDTO(saved);
    }

    @Override
    public CouponDTO updateCoupon(Long id, CouponDTO couponDTO) {
        logger.info("更新优惠券: {}", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("优惠券不存在: " + id));

        // 更新字段
        if (couponDTO.getName() != null) {
            coupon.setName(couponDTO.getName());
        }
        if (couponDTO.getValue() != null) {
            coupon.setValue(couponDTO.getValue());
        }
        if (couponDTO.getMinAmount() != null) {
            coupon.setMinAmount(couponDTO.getMinAmount());
        }
        if (couponDTO.getUsageLimitPerUser() != null) {
            coupon.setUsageLimitPerUser(couponDTO.getUsageLimitPerUser());
        }
        if (couponDTO.getTotalLimit() != null) {
            coupon.setTotalLimit(couponDTO.getTotalLimit());
        }
        if (couponDTO.getStartTime() != null) {
            coupon.setStartTime(couponDTO.getStartTime());
        }
        if (couponDTO.getEndTime() != null) {
            coupon.setEndTime(couponDTO.getEndTime());
        }
        if (couponDTO.getType() != null) {
            coupon.setType(parseCouponType(couponDTO.getType()));
        }
        coupon.setUpdatedAt(LocalDateTime.now());

        Coupon updated = couponRepository.save(coupon);
        logger.info("优惠券更新成功, ID: {}", updated.getId());

        return convertToDTO(updated);
    }

    @Override
    public void deleteCoupon(Long id) {
        logger.info("删除优惠券: {}", id);

        if (!couponRepository.existsById(id)) {
            throw new IllegalArgumentException("优惠券不存在: " + id);
        }

        couponRepository.deleteById(id);
        logger.info("优惠券删除成功, ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDTO getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("优惠券不存在: " + id));
        return convertToDTO(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDTO getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("优惠券不存在: " + code));
        return convertToDTO(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CouponDTO> getAllCoupons(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Coupon> couponPage = couponRepository.findAll(pageable);

        return couponPage.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponDTO> getEnabledCoupons() {
        List<Coupon> coupons = couponRepository.findByIsEnabledTrue();
        return coupons.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CouponDTO toggleCouponStatus(Long id, Boolean enabled) {
        logger.info("{}优惠券: {}", enabled ? "启用" : "禁用", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("优惠券不存在: " + id));

        coupon.setIsEnabled(enabled);
        coupon.setUpdatedAt(LocalDateTime.now());

        Coupon updated = couponRepository.save(coupon);
        logger.info("优惠券状态更新成功, ID: {}, 状态: {}", id, enabled);

        return convertToDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscountAmount(Long couponId, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("优惠券不存在: " + couponId));

        // 验证优惠券是否可用
        if (!isCouponValid(couponId, orderAmount)) {
            throw new IllegalArgumentException("优惠券不可用");
        }

        BigDecimal discountAmount;
        if (coupon.getType() == Coupon.CouponType.discount) {
            // 折扣券：value表示打几折（如8.8表示打88折，支付88%，优惠12%）
            // payRate = value / 10  （支付比例，如0.88）
            // discountRate = 1 - payRate  （优惠比例，如0.12）
            // discountAmount = orderAmount × discountRate
            BigDecimal payRate = coupon.getValue().divide(BigDecimal.valueOf(10), 4, BigDecimal.ROUND_HALF_UP);
            BigDecimal discountRate = BigDecimal.ONE.subtract(payRate);
            discountAmount = orderAmount.multiply(discountRate);
        } else {
            // 现金减免券：直接使用value
            discountAmount = coupon.getValue();
        }

        // 确保优惠金额不超过订单金额
        if (discountAmount.compareTo(orderAmount) > 0) {
            discountAmount = orderAmount;
        }

        return orderAmount.subtract(discountAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCouponValid(Long couponId, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("优惠券不存在: " + couponId));

        // 检查是否启用
        if (!coupon.getIsEnabled()) {
            logger.warn("优惠券未启用: {}", couponId);
            return false;
        }

        // 检查时间范围
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            logger.warn("优惠券不在有效期内: {}", couponId);
            return false;
        }

        // 检查使用门槛
        if (coupon.getMinAmount() != null && orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            logger.warn("订单金额未达到优惠券使用门槛: {}, 订单金额: {}, 门槛: {}", 
                    couponId, orderAmount, coupon.getMinAmount());
            return false;
        }

        // 检查总发放数量
        if (coupon.getTotalLimit() != null) {
            long issuedCount = couponRepository.countIssuedCoupons(couponId);
            if (issuedCount >= coupon.getTotalLimit()) {
                logger.warn("优惠券已达到发放上限: {}", couponId);
                return false;
            }
        }

        return true;
    }

    /**
     * 将实体转换为DTO
     */
    private CouponDTO convertToDTO(Coupon coupon) {
        CouponDTO dto = new CouponDTO();
        BeanUtils.copyProperties(coupon, dto);
        dto.setType(toApiCouponType(coupon.getType()));
        
        // 统计已发放数量
        long issuedCount = couponRepository.countIssuedCoupons(coupon.getId());
        dto.setIssuedCount(issuedCount);
        
        return dto;
    }

    private Coupon.CouponType parseCouponType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("优惠券类型不能为空");
        }

        String normalized = type.trim().toUpperCase().replace('-', '_');
        if ("DISCOUNT".equals(normalized)) {
            return Coupon.CouponType.discount;
        }
        if ("AMOUNT_OFF".equals(normalized) || "AMOUNTOFF".equals(normalized)) {
            return Coupon.CouponType.amount_off;
        }

        for (Coupon.CouponType value : Coupon.CouponType.values()) {
            if (value.name().equalsIgnoreCase(type.trim())) {
                return value;
            }
        }
        throw new IllegalArgumentException("不支持的优惠券类型: " + type);
    }

    private String toApiCouponType(Coupon.CouponType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case discount -> "DISCOUNT";
            case amount_off -> "AMOUNT_OFF";
        };
    }
}

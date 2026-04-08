package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.CouponDTO;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务接口
 */
public interface CouponService {

    /**
     * 创建优惠券
     */
    CouponDTO createCoupon(CouponDTO couponDTO);

    /**
     * 更新优惠券
     */
    CouponDTO updateCoupon(Long id, CouponDTO couponDTO);

    /**
     * 删除优惠券
     */
    void deleteCoupon(Long id);

    /**
     * 根据ID查询优惠券
     */
    CouponDTO getCouponById(Long id);

    /**
     * 根据优惠券码查询
     */
    CouponDTO getCouponByCode(String code);

    /**
     * 分页查询所有优惠券
     */
    Page<CouponDTO> getAllCoupons(int page, int size);

    /**
     * 查询所有启用的优惠券
     */
    List<CouponDTO> getEnabledCoupons();

    /**
     * 启用/禁用优惠券
     */
    CouponDTO toggleCouponStatus(Long id, Boolean enabled);

    /**
     * 计算优惠券折扣金额
     * @param couponId 优惠券ID
     * @param orderAmount 订单金额
     * @return 折扣后的金额
     */
    BigDecimal calculateDiscountAmount(Long couponId, BigDecimal orderAmount);

    /**
     * 验证优惠券是否可用
     */
    boolean isCouponValid(Long couponId, BigDecimal orderAmount);
}

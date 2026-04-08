import axios from 'axios'

/**
 * 优惠券相关 API
 */

// ==================== 用户端接口 ====================

/**
 * 查询所有可用的优惠券列表
 * @returns {Promise}
 */
export const getAvailableCoupons = () => axios.get('/api/coupons/available')

/**
 * 根据优惠券码查询详情
 * @param {string} code - 优惠券码
 * @returns {Promise}
 */
export const getCouponByCode = (code) => axios.get(`/api/coupons/code/${code}`)

/**
 * 领取优惠券（通过兑换码）
 * @param {Object} data - { couponCode, userId }
 * @returns {Promise}
 */
export const redeemCoupon = (data) => axios.post('/api/coupons/redeem', data)

/**
 * 查询用户的优惠券列表
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export const getUserCoupons = (userId) => axios.get(`/api/coupons/user/${userId}`)

/**
 * 查询用户可用的优惠券
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export const getUserAvailableCoupons = (userId) => axios.get(`/api/coupons/user/${userId}/available`)

/**
 * 计算使用优惠券后的金额
 * @param {Object} data - { couponId, orderAmount }
 * @returns {Promise}
 */
export const calculateDiscount = (data) => axios.post('/api/coupons/calculate', data)

/**
 * 验证优惠券是否可用
 * @param {Object} data - { couponId, orderAmount }
 * @returns {Promise}
 */
export const validateCoupon = (data) => axios.post('/api/coupons/validate', data)

/**
 * 查询用户的核销记录
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export const getUserRedemptions = (userId) => axios.get(`/api/coupons/user/${userId}/redemptions`)

/**
 * 统计用户优惠总金额
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export const getUserTotalDiscount = (userId) => axios.get(`/api/coupons/user/${userId}/total-discount`)

// ==================== 管理端接口 ====================

/**
 * 创建优惠券
 * @param {Object} data - 优惠券信息
 * @returns {Promise}
 */
export const createCoupon = (data) => axios.post('/api/admin/coupons', data)

/**
 * 更新优惠券
 * @param {number} id - 优惠券ID
 * @param {Object} data - 优惠券信息
 * @returns {Promise}
 */
export const updateCoupon = (id, data) => axios.put(`/api/admin/coupons/${id}`, data)

/**
 * 删除优惠券
 * @param {number} id - 优惠券ID
 * @returns {Promise}
 */
export const deleteCoupon = (id) => axios.delete(`/api/admin/coupons/${id}`)

/**
 * 查询优惠券详情
 * @param {number} id - 优惠券ID
 * @returns {Promise}
 */
export const getCouponDetail = (id) => axios.get(`/api/admin/coupons/${id}`)

/**
 * 分页查询所有优惠券
 * @param {Object} params - { page, size }
 * @returns {Promise}
 */
export const getAllCoupons = (params) => axios.get('/api/admin/coupons', { params })

/**
 * 启用/禁用优惠券
 * @param {number} id - 优惠券ID
 * @param {Object} data - { enabled }
 * @returns {Promise}
 */
export const toggleCouponStatus = (id, data) => axios.put(`/api/admin/coupons/${id}/status`, data)

/**
 * 发放优惠券给用户
 * @param {Object} data - { couponId, userId, sourceType, remark }
 * @returns {Promise}
 */
export const issueCoupon = (data) => axios.post('/api/admin/coupons/issue', data)

/**
 * 批量发放优惠券
 * @param {Object} data - { couponId, userIds, sourceType, remark }
 * @returns {Promise}
 */
export const batchIssueCoupons = (data) => axios.post('/api/admin/coupons/batch-issue', data)

/**
 * 查询用户的优惠券（分页）
 * @param {number} userId - 用户ID
 * @param {Object} params - { page, size }
 * @returns {Promise}
 */
export const adminGetUserCoupons = (userId, params) => axios.get(`/api/admin/coupons/user/${userId}`, { params })

/**
 * 作废用户优惠券
 * @param {number} userCouponId - 用户优惠券ID
 * @param {Object} data - { reason }
 * @returns {Promise}
 */
export const voidUserCoupon = (userCouponId, data) => axios.put(`/api/admin/coupons/user-coupon/${userCouponId}/void`, data)

/**
 * 查询用户的核销记录（分页）
 * @param {number} userId - 用户ID
 * @param {Object} params - { page, size }
 * @returns {Promise}
 */
export const adminGetUserRedemptions = (userId, params) => axios.get(`/api/admin/coupons/redemptions/user/${userId}`, { params })

/**
 * 回滚核销记录
 * @param {number} redemptionId - 核销记录ID
 * @param {Object} data - { reason }
 * @returns {Promise}
 */
export const rollbackRedemption = (redemptionId, data) => axios.put(`/api/admin/coupons/redemption/${redemptionId}/rollback`, data)

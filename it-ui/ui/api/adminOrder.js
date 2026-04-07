import axios from 'axios'

/**
 * 后台订单管理 API
 */

/**
 * 分页查询订单列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码（从0开始）
 * @param {number} params.size - 每页大小
 * @param {string} params.status - 订单状态（可选）
 * @param {string} params.type - 订单类型（可选）
 * @returns {Promise}
 */
export const getOrdersPage = (params) => {
  return axios.get('/api/admin/orders/page', { params })
}

/**
 * 查询订单详情（包含支付记录）
 * @param {number} id - 订单ID
 * @returns {Promise}
 */
export const getOrderDetail = (id) => {
  return axios.get(`/api/admin/orders/${id}`)
}

/**
 * 更新订单状态
 * @param {number} id - 订单ID
 * @param {string} status - 新状态
 * @returns {Promise}
 */
export const updateOrderStatus = (id, status) => {
  return axios.put(`/api/admin/orders/${id}/status`, { status })
}

/**
 * 删除订单
 * @param {number} id - 订单ID
 * @returns {Promise}
 */
export const deleteOrder = (id) => {
  return axios.delete(`/api/admin/orders/${id}`)
}

/**
 * 获取订单统计数据
 * @returns {Promise}
 */
export const getOrderStatistics = () => {
  return axios.get('/api/admin/orders/statistics')
}

/**
 * 查询用户的订单列表
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export const getOrdersByUserId = (userId) => {
  return axios.get(`/api/admin/orders/user/${userId}`)
}

/**
 * 查询订单的支付记录
 * @param {number} orderId - 订单ID
 * @returns {Promise}
 */
export const getPaymentRecords = (orderId) => {
  return axios.get(`/api/creator-withdraw-requests/payment-records/order/${orderId}`)
}

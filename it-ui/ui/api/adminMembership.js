import axios from 'axios'

/**
 * 后台会员管理 API
 */

/**
 * 分页查询会员列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码（从0开始）
 * @param {number} params.size - 每页大小
 * @param {string} params.status - 会员状态（可选）
 * @param {number} params.userId - 用户ID（可选）
 * @returns {Promise}
 */
export const getMembershipsPage = (params) => {
  return axios.get('/api/admin/memberships/page', { params })
}

/**
 * 查询会员详情
 * @param {number} id - 会员ID
 * @returns {Promise}
 */
export const getMembershipDetail = (id) => {
  return axios.get(`/api/admin/memberships/${id}`)
}

/**
 * 查询用户的会员记录
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export const getMembershipsByUserId = (userId) => {
  return axios.get(`/api/admin/memberships/user/${userId}`)
}

/**
 * 更新会员状态
 * @param {number} id - 会员ID
 * @param {string} status - 新状态
 * @returns {Promise}
 */
export const updateMembershipStatus = (id, status) => {
  return axios.put(`/api/admin/memberships/${id}/status`, { status })
}

/**
 * 手动开通/续费会员
 * @param {Object} data - 开通参数
 * @param {number} data.userId - 用户ID
 * @param {number} data.levelId - 会员等级ID
 * @param {number} data.durationDays - 天数（可选，默认使用等级的天数）
 * @returns {Promise}
 */
export const grantMembership = (data) => {
  return axios.post('/api/admin/memberships/grant', data)
}

/**
 * 删除会员记录
 * @param {number} id - 会员ID
 * @returns {Promise}
 */
export const deleteMembership = (id) => {
  return axios.delete(`/api/admin/memberships/${id}`)
}

/**
 * 获取会员统计数据
 * @returns {Promise}
 */
export const getMembershipStatistics = () => {
  return axios.get('/api/admin/memberships/statistics')
}

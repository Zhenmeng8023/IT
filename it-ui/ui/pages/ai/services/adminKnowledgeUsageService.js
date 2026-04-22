import request from '@/utils/request'

function normalizePageParams(params = {}) {
  return {
    page: params.page ?? 0,
    size: params.size ?? 10,
    ...params
  }
}

function extractApiData(res) {
  if (res == null) return null
  const payload = res.data !== undefined ? res.data : res
  if (payload && payload.data !== undefined) {
    return payload.data
  }
  return payload
}

function extractPageContent(res) {
  const data = extractApiData(res) || {}
  return {
    content: data.content || [],
    total: Number(data.totalElements || 0)
  }
}

export const adminKnowledgeUsageService = {
  fetchUsers(params = {}) {
    return request({
      url: '/admin/ai/knowledge-usage/users',
      method: 'get',
      params: normalizePageParams(params)
    })
  },

  fetchUserStatus(userId) {
    return request({
      url: `/admin/ai/knowledge-usage/users/${userId}/status`,
      method: 'get'
    })
  },

  updateQuota(userId, data) {
    return request({
      url: `/admin/ai/knowledge-usage/users/${userId}/quota`,
      method: 'put',
      data
    })
  },

  freezeUser(userId, data = {}) {
    return request({
      url: `/admin/ai/knowledge-usage/users/${userId}/freeze`,
      method: 'post',
      data
    })
  },

  unfreezeUser(userId, data = {}) {
    return request({
      url: `/admin/ai/knowledge-usage/users/${userId}/unfreeze`,
      method: 'post',
      data
    })
  },

  disableImport(userId, data = {}) {
    return request({
      url: `/admin/ai/knowledge-usage/users/${userId}/disable-import`,
      method: 'post',
      data
    })
  },

  enableImport(userId, data = {}) {
    return request({
      url: `/admin/ai/knowledge-usage/users/${userId}/enable-import`,
      method: 'post',
      data
    })
  },

  disableQa(userId, data = {}) {
    return request({
      url: `/admin/ai/knowledge-usage/users/${userId}/disable-qa`,
      method: 'post',
      data
    })
  },

  enableQa(userId, data = {}) {
    return request({
      url: `/admin/ai/knowledge-usage/users/${userId}/enable-qa`,
      method: 'post',
      data
    })
  },

  extractApiData,
  extractPageContent
}

export default adminKnowledgeUsageService

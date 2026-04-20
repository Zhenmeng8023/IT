import { pickAvatarUrl } from '@/utils/avatar'

function toNumber(value, fallback = 0) {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

export function buildAdminUserPageParams(pagination = {}, filters = {}) {
  const params = {
    page: Math.max(toNumber(pagination.currentPage, 1) - 1, 0),
    size: Math.max(toNumber(pagination.pageSize, 10), 1)
  }

  if (filters.username) params.username = String(filters.username).trim()
  if (filters.email) params.email = String(filters.email).trim()
  if (filters.phone) params.phone = String(filters.phone).trim()
  if (filters.status) params.status = filters.status
  if (filters.roleId !== '' && filters.roleId !== null && filters.roleId !== undefined) {
    params.roleId = toNumber(filters.roleId)
  }

  return params
}

export function unwrapAdminUserPage(response) {
  const payload = response?.data !== undefined ? response.data : response
  if (!payload || typeof payload !== 'object') {
    return { list: [], total: 0 }
  }

  if (Array.isArray(payload.list)) {
    return { list: payload.list, total: toNumber(payload.total, payload.list.length) }
  }

  // Transitional compatibility for old Spring Page response.
  if (Array.isArray(payload.content)) {
    return { list: payload.content, total: toNumber(payload.totalElements, payload.content.length) }
  }

  return { list: [], total: 0 }
}

export function normalizeAdminUser(user = {}) {
  return {
    id: user.id,
    username: user.username || '',
    email: user.email || '',
    phone: user.phone || '',
    nickname: user.nickname || '',
    roleId: toNumber(user.roleId ?? user.role_id, 4),
    status: user.status || 'active',
    gender: String(user.gender ?? '0'),
    avatarUrl: pickAvatarUrl(user.avatarUrl, user.avatar_url, user.avatar),
    createdAt: user.createdAt || user.created_at || '',
    lastLoginAt: user.lastLoginAt || user.last_login_at || '',
    balance: user.balance || 0,
    loginCount: toNumber(user.loginCount ?? user.login_count, 0)
  }
}

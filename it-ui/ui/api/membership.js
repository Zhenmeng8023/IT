import http from './shared/http'

function unwrapPayload(response) {
  return response && response.data !== undefined ? response.data : response
}

function unwrapDataField(payload, fallback = null) {
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'data')) {
    return payload.data
  }
  return payload !== undefined ? payload : fallback
}

export async function getVipLevels({ enabledOnly = false } = {}) {
  const url = enabledOnly ? '/api/membership-levels/enabled/true' : '/api/membership-levels'
  return unwrapPayload(await http.get(url)) || []
}

export async function getUserActiveMembership(userId) {
  if (!userId) {
    return null
  }

  return unwrapPayload(await http.get(`/api/membership/user/${userId}/active`))
}

export async function buyVipMembership({ userId, membershipLevelId, couponId } = {}) {
  const params = {
    userId,
    membershipLevelId
  }

  if (couponId) {
    params.couponId = couponId
  }

  return unwrapPayload(await http.post('/api/membership/buy-vip', null, { params }))
}

export async function getAvailableMembershipCoupons(userId) {
  if (!userId) {
    return []
  }

  const payload = unwrapPayload(await http.get(`/api/coupons/user/${userId}/available`))
  return unwrapDataField(payload, [])
}

export async function calculateMembershipDiscount(data) {
  return unwrapPayload(await http.post('/api/coupons/calculate', data))
}

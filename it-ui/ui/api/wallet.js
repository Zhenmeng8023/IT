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

export async function getCurrentUserProfile() {
  return unwrapPayload(await http.get('/api/users/current'))
}

export async function getUserBalance() {
  return unwrapPayload(await http.get('/api/users/balance'))
}

export async function getRevenueRecordsBySourceUserId(userId) {
  if (!userId) {
    return []
  }

  return unwrapPayload(await http.get(`/api/revenue-records/source-user/${userId}`)) || []
}

export async function getUserTotalRevenue(userId) {
  if (!userId) {
    return 0
  }

  return unwrapPayload(await http.get(`/api/revenue-records/user/${userId}/total`)) || 0
}

export async function getSettlementAccountsByUserId(userId) {
  if (!userId) {
    return []
  }

  return unwrapPayload(await http.get(`/api/creator-settlement-accounts/user/${userId}`)) || []
}

export async function getAvailableWithdrawBalance(userId) {
  if (!userId) {
    return 0
  }

  const payload = unwrapPayload(await http.get(`/api/creator-withdraw-requests/user/${userId}/available-balance`))
  return Number(unwrapDataField(payload, 0) || 0)
}

export async function submitWithdrawRequest(data) {
  return unwrapPayload(await http.post('/api/creator-withdraw-requests/apply', data))
}

export async function getWithdrawRequestsByUserId(userId) {
  if (!userId) {
    return []
  }

  return unwrapPayload(await http.get(`/api/creator-withdraw-requests/user/${userId}`)) || []
}

import request from '@/utils/request'

function unwrapManageResponse(payload) {
  if (!payload || typeof payload !== 'object') {
    return payload
  }
  if (!Object.prototype.hasOwnProperty.call(payload, 'data') || payload.data === payload) {
    return payload
  }
  return unwrapManageResponse(payload.data)
}

function normalizeManageIds(ids = []) {
  const uniqueIds = []
  ids.forEach((id) => {
    const normalized = Number(id)
    if (Number.isFinite(normalized) && normalized > 0 && !uniqueIds.includes(normalized)) {
      uniqueIds.push(normalized)
    }
  })
  return uniqueIds
}

export function parseCircleManageError(error, fallback = '请求失败') {
  if (error && error.response && error.response.data && error.response.data.message) {
    return error.response.data.message
  }
  if (error && error.message) {
    return error.message
  }
  return fallback
}

export function requireCircleManageData(payload, fallback = '请求失败') {
  if (!payload || typeof payload !== 'object') {
    throw new Error(fallback)
  }

  if (typeof payload.code === 'number' && payload.code !== 200) {
    throw new Error(payload.message || fallback)
  }

  if (payload.success === false) {
    throw new Error(payload.message || fallback)
  }

  return unwrapManageResponse(payload)
}

export function normalizeCircleManagePage(payload, fallback = '加载列表失败') {
  const data = requireCircleManageData(payload, fallback)
  if (Array.isArray(data)) {
    return {
      list: data,
      total: data.length,
      currentPage: 1,
      pageSize: data.length
    }
  }

  const pageData = data && typeof data === 'object' ? data : {}
  const list = Array.isArray(pageData.list) ? pageData.list : []
  const total = Number(pageData.total)
  const currentPage = Number(pageData.currentPage)
  const pageSize = Number(pageData.pageSize)

  return {
    list,
    total: Number.isFinite(total) ? total : list.length,
    currentPage: Number.isFinite(currentPage) && currentPage > 0 ? currentPage : 1,
    pageSize: Number.isFinite(pageSize) && pageSize > 0 ? pageSize : list.length
  }
}

function buildBatchPayload(ids = []) {
  return normalizeManageIds(ids)
}

export function getCircleManageStats() {
  return request({
    url: '/circle/manage/stats',
    method: 'get'
  })
}

export function getCircleManageList(params = {}) {
  return request({
    url: '/circle/manage/list',
    method: 'get',
    params
  })
}

export function createCircle(data) {
  return request({
    url: '/circle',
    method: 'post',
    data
  })
}

export function updateCircle(circleId, data) {
  return request({
    url: `/circle/${circleId}`,
    method: 'put',
    data
  })
}

export function approveCircle(circleId) {
  return request({
    url: `/circle/manage/approve/${circleId}`,
    method: 'put'
  })
}

export function rejectCircle(circleId) {
  return request({
    url: `/circle/manage/reject/${circleId}`,
    method: 'put'
  })
}

export function closeCircle(circleId) {
  return request({
    url: `/circle/manage/close/${circleId}`,
    method: 'put'
  })
}

export function deleteCircle(circleId) {
  return request({
    url: `/circle/manage/delete/${circleId}`,
    method: 'delete'
  })
}

export function batchApproveCircles(ids = []) {
  return request({
    url: '/circle/manage/batch-approve',
    method: 'post',
    data: buildBatchPayload(ids)
  })
}

export function batchCloseCircles(ids = []) {
  return request({
    url: '/circle/manage/batch-close',
    method: 'post',
    data: buildBatchPayload(ids)
  })
}

export function batchDeleteCircles(ids = []) {
  return request({
    url: '/circle/manage/batch-delete',
    method: 'post',
    data: buildBatchPayload(ids)
  })
}

export function getCircleMembers(circleId) {
  return request({
    url: `/circle/manage/members/${circleId}`,
    method: 'get'
  })
}

export function setCircleAdmin(memberId, role = 'admin') {
  return request({
    url: `/circle/manage/set-admin/${memberId}`,
    method: 'put',
    params: { role }
  })
}

export function removeCircleMember(memberId) {
  return request({
    url: `/circle/manage/remove-member/${memberId}`,
    method: 'delete'
  })
}

export function getCirclePosts(circleId) {
  return request({
    url: `/circle/manage/posts/${circleId}`,
    method: 'get'
  })
}

export function approveCirclePost(postId) {
  return request({
    url: `/circle/manage/approve-post/${postId}`,
    method: 'put'
  })
}

export function deleteCirclePost(postId) {
  return request({
    url: `/circle/manage/delete-post/${postId}`,
    method: 'delete'
  })
}

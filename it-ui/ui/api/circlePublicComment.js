import request from '@/utils/request'

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function isEnvelope(value) {
  if (!isObject(value)) {
    return false
  }

  return (
    Object.prototype.hasOwnProperty.call(value, 'code') ||
    Object.prototype.hasOwnProperty.call(value, 'success') ||
    Object.prototype.hasOwnProperty.call(value, 'message') ||
    Object.prototype.hasOwnProperty.call(value, 'msg')
  )
}

function unwrapApiData(payload) {
  let current = payload
  let depth = 0

  while (depth < 6 && current !== undefined && current !== null) {
    if (Array.isArray(current)) {
      return current
    }

    if (!isObject(current)) {
      return current
    }

    if (isEnvelope(current) && current.data !== undefined) {
      current = current.data
      depth += 1
      continue
    }

    if (current.data !== undefined && (isObject(current.data) || Array.isArray(current.data))) {
      current = current.data
      depth += 1
      continue
    }

    return current
  }

  return current
}

function pickList(payload) {
  const data = unwrapApiData(payload)

  if (Array.isArray(data)) {
    return data
  }

  if (!isObject(data)) {
    return []
  }

  const list = data.list || data.records || data.rows || data.items || data.content || data.comments
  return Array.isArray(list) ? list : []
}

function pickEntity(payload) {
  const data = unwrapApiData(payload)
  if (isObject(data)) {
    return data
  }
  return {}
}

export function getCirclePostDetail(postId) {
  return request({
    url: `/circle/comments/${postId}`,
    method: 'get'
  }).then(pickEntity)
}

export function getCirclePostComments(postId) {
  return request({
    url: `/circle/posts/${postId}/comments/all`,
    method: 'get'
  }).then(pickList)
}

export function createCircleComment(payload) {
  return request({
    url: '/circle/comments',
    method: 'post',
    data: payload
  }).then(pickEntity)
}

export function extractApiErrorMessage(error, fallback = '请求失败') {
  if (!error) {
    return fallback
  }

  const responseData = error.response ? error.response.data : null
  const unwrapped = unwrapApiData(responseData)

  if (isObject(unwrapped)) {
    if (unwrapped.message) return unwrapped.message
    if (unwrapped.msg) return unwrapped.msg
  }

  if (isObject(responseData)) {
    if (responseData.message) return responseData.message
    if (responseData.msg) return responseData.msg
  }

  if (error.message) {
    return error.message
  }

  return fallback
}

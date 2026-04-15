import request from '@/utils/request'

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

export function searchCircleByName(name) {
  return request({
    url: `/circle/name/${encodeURIComponent(name)}`,
    method: 'get'
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

export function toggleCircleRecommend(circleId) {
  return request({
    url: `/circle/manage/toggle-recommend/${circleId}`,
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
    data: ids
  })
}

export function batchCloseCircles(ids = []) {
  return request({
    url: '/circle/manage/batch-close',
    method: 'post',
    data: ids
  })
}

export function batchDeleteCircles(ids = []) {
  return request({
    url: '/circle/manage/batch-delete',
    method: 'post',
    data: ids
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

export function getCirclePosts(circleId, params = {}) {
  return request({
    url: `/circle/${circleId}/posts`,
    method: 'get',
    params
  })
}

export function getCirclePostDetail(postId) {
  return request({
    url: `/circle/comments/${postId}`,
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
    url: `/circle/comments/${postId}`,
    method: 'delete'
  })
}

export function getUserById(userId) {
  return request({
    url: `/users/${userId}`,
    method: 'get'
  })
}

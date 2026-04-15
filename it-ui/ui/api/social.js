import request from '@/utils/request'

export function getFriendList(params = {}) {
  return request({
    url: '/circle/friend/list',
    method: 'get',
    params
  })
}

export function getFriendRequests(params = {}) {
  return request({
    url: '/circle/friend/request/list',
    method: 'get',
    params
  })
}

export function getFriendGroups(params = {}) {
  return request({
    url: '/circle/friend/group/list',
    method: 'get',
    params
  })
}

export function sendFriendRequest(data) {
  return request({
    url: '/circle/friend/request/send',
    method: 'post',
    data
  })
}

export function updateFriendRemark(friendId, data) {
  return request({
    url: `/circle/friend/remark/${friendId}`,
    method: 'post',
    data
  })
}

export function moveFriendGroup(friendId, data) {
  return request({
    url: `/circle/friend/group/${friendId}`,
    method: 'post',
    data
  })
}

export function toggleSpecialFocus(friendId, data = {}) {
  return request({
    url: `/circle/friend/special/${friendId}`,
    method: 'post',
    data
  })
}

export function deleteFriend(friendId) {
  return request({
    url: `/circle/friend/remove/${friendId}`,
    method: 'delete'
  })
}

export function approveFriendRequest(requestId, data = {}) {
  return request({
    url: `/circle/friend/request/approve/${requestId}`,
    method: 'post',
    data
  })
}

export function rejectFriendRequest(requestId, data = {}) {
  return request({
    url: `/circle/friend/request/reject/${requestId}`,
    method: 'post',
    data
  })
}

export function createFriendGroup(data) {
  return request({
    url: '/circle/friend/group/add',
    method: 'post',
    data
  })
}

export function updateFriendGroup(groupId, data) {
  return request({
    url: `/circle/friend/group/${groupId}`,
    method: 'put',
    data
  })
}

export function deleteFriendGroup(groupId, params = {}) {
  return request({
    url: `/circle/friend/group/delete/${groupId}`,
    method: 'delete',
    params
  })
}

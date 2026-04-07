
import request from '@/utils/request'

export function submitProjectJoinRequest(projectId, data) {
  return request({
    url: `/project/${projectId}/join-requests`,
    method: 'post',
    data
  })
}

export function listProjectJoinRequests(projectId) {
  return request({
    url: `/project/${projectId}/join-requests`,
    method: 'get'
  })
}

export function getMyProjectJoinRequestStatus(projectId) {
  return request({
    url: `/project/${projectId}/join-requests/my-status`,
    method: 'get'
  })
}

export function auditProjectJoinRequest(requestId, data) {
  return request({
    url: `/project/join-requests/${requestId}/audit`,
    method: 'put',
    data
  })
}

export function cancelProjectJoinRequest(requestId) {
  return request({
    url: `/project/join-requests/${requestId}/cancel`,
    method: 'post'
  })
}

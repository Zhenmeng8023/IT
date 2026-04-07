
import request from '@/utils/request'

export function createProjectInvite(projectId, data) {
  return request({
    url: `/project/${projectId}/invites`,
    method: 'post',
    data
  })
}

export function listProjectInvites(projectId) {
  return request({
    url: `/project/${projectId}/invites`,
    method: 'get'
  })
}

export function getProjectInviteDetail(inviteCode) {
  return request({
    url: `/project/invites/${inviteCode}`,
    method: 'get'
  })
}

export function acceptProjectInvite(inviteCode, data = {}) {
  return request({
    url: `/project/invites/${inviteCode}/accept`,
    method: 'post',
    data
  })
}

export function cancelProjectInvite(inviteId) {
  return request({
    url: `/project/invites/${inviteId}/cancel`,
    method: 'post'
  })
}

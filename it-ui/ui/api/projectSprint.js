import request from '@/utils/request'

export function listProjectSprints(projectId, params = {}) {
  return request({
    url: '/project/sprint/list',
    method: 'get',
    params: { projectId, ...params }
  })
}

export function getCurrentProjectSprint(projectId) {
  return request({
    url: '/project/sprint/current',
    method: 'get',
    params: { projectId }
  })
}

export function createProjectSprint(data) {
  return request({
    url: '/project/sprint',
    method: 'post',
    data
  })
}

export function updateProjectSprint(id, data) {
  return request({
    url: `/project/sprint/${id}`,
    method: 'put',
    data
  })
}

export function changeProjectSprintStatus(id, status) {
  return request({
    url: `/project/sprint/${id}/status`,
    method: 'put',
    params: { status }
  })
}

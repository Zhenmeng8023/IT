import request from '@/utils/request'

export function listProjectMilestones(projectId, params = {}) {
  return request({
    url: '/project/milestone/list',
    method: 'get',
    params: { projectId, ...params }
  })
}

export function getProjectMilestoneOverview(projectId) {
  return request({
    url: '/project/milestone/overview',
    method: 'get',
    params: { projectId }
  })
}

export function createProjectMilestone(data) {
  return request({
    url: '/project/milestone',
    method: 'post',
    data
  })
}

export function updateProjectMilestone(id, data) {
  return request({
    url: `/project/milestone/${id}`,
    method: 'put',
    data
  })
}

export function changeProjectMilestoneStatus(id, status) {
  return request({
    url: `/project/milestone/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function deleteProjectMilestone(id) {
  return request({
    url: `/project/milestone/${id}`,
    method: 'delete'
  })
}

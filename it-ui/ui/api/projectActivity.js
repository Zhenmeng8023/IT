import request from '@/utils/request'

export function getProjectActivities(projectId, params = {}) {
  return request({
    url: `/project/${projectId}/activities`,
    method: 'get',
    params
  })
}

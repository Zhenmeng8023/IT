import request from '@/utils/request'

export function getProjectActivities(projectId, params = {}) {
  return request({
    url: `/project/${projectId}/activities`,
    method: 'get',
    params
  })
}

export function pageProjectActivities(projectId, params = {}) {
  return request({
    url: `/project/${projectId}/activities`,
    method: 'get',
    params
  })
}

export function getProjectActivityPosition(projectId, activityId, params = {}) {
  return request({
    url: `/project/${projectId}/activities/${activityId}/position`,
    method: 'get',
    params
  })
}

import request from '@/utils/request'

export function getProjectStatOverview(projectId) {
  return request({
    url: '/project/stat/overview',
    method: 'get',
    params: { projectId }
  })
}

export function getProjectStatTrend(projectId, params = {}) {
  return request({
    url: '/project/stat/trend',
    method: 'get',
    params: { projectId, ...params }
  })
}

export function pageProjectStatDaily(projectId, params = {}) {
  return request({
    url: '/project/stat/daily/page',
    method: 'get',
    params: { projectId, ...params }
  })
}

export function rebuildProjectStatDaily(projectId, params = {}) {
  return request({
    url: '/project/stat/rebuild',
    method: 'post',
    params: { projectId, ...params }
  })
}

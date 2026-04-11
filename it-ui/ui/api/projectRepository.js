import request from '@/utils/request'

export function initProjectRepository(projectId) {
  return request({
    url: '/project/repo/init',
    method: 'post',
    params: { projectId }
  })
}

export function getProjectRepository(projectId) {
  return request({
    url: '/project/repo/detail',
    method: 'get',
    params: { projectId }
  })
}

import request from '@/utils/request'

export function listProjectBranches(projectId) {
  return request({
    url: '/project/branch/list',
    method: 'get',
    params: { projectId }
  })
}

export function createProjectBranch(data) {
  return request({
    url: '/project/branch/create',
    method: 'post',
    data
  })
}

export function protectProjectBranch(branchId, params) {
  return request({
    url: `/project/branch/${branchId}/protect`,
    method: 'put',
    params
  })
}

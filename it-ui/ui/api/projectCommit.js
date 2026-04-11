import request from '@/utils/request'

export function listProjectCommits(projectId, branchId) {
  return request({
    url: '/project/commit/list',
    method: 'get',
    params: { projectId, branchId }
  })
}

export function getProjectCommitDetail(commitId) {
  return request({
    url: '/project/commit/detail',
    method: 'get',
    params: { commitId }
  })
}

export function compareProjectCommits(fromCommitId, toCommitId) {
  return request({
    url: '/project/commit/compare',
    method: 'get',
    params: { fromCommitId, toCommitId }
  })
}

export function rollbackToCommit(commitId) {
  return request({
    url: `/project/commit/rollback-to/${commitId}`,
    method: 'post'
  })
}

import request from '@/utils/request'

export function listProjectMergeRequests(projectId, status) {
  return request({
    url: '/project/mr/list',
    method: 'get',
    params: {
      projectId,
      ...(status ? { status } : {})
    }
  })
}

export function createProjectMergeRequest(data) {
  return request({
    url: '/project/mr/create',
    method: 'post',
    data
  })
}

export function reviewProjectMergeRequest(mergeRequestId, data) {
  return request({
    url: `/project/mr/${mergeRequestId}/review`,
    method: 'post',
    data
  })
}

export function mergeProjectMergeRequest(mergeRequestId) {
  return request({
    url: `/project/mr/${mergeRequestId}/merge`,
    method: 'post'
  })
}

export function runProjectCheck(data) {
  return request({
    url: '/project/check/run',
    method: 'post',
    data
  })
}

export function listProjectChecksByCommit(commitId) {
  return request({
    url: '/project/check/list-by-commit',
    method: 'get',
    params: { commitId }
  })
}

export function listProjectChecksByMergeRequest(mergeRequestId) {
  return request({
    url: '/project/check/list-by-mr',
    method: 'get',
    params: { mergeRequestId }
  })
}

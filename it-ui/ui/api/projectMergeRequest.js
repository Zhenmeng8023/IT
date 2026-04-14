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

export function getProjectMergeCheckLatest(mergeRequestId) {
  return request({
    url: `/project/mr/${mergeRequestId}/merge-check/latest`,
    method: 'get'
  })
}

export function recheckProjectMerge(mergeRequestId) {
  return request({
    url: `/project/mr/${mergeRequestId}/merge-check/recheck`,
    method: 'post'
  })
}

export function resolveProjectMergeConflicts(mergeRequestId, data) {
  return request({
    url: `/project/mr/${mergeRequestId}/merge-check/conflicts/resolve`,
    method: 'post',
    data
  })
}

export function getProjectMergeContentConflict(mergeRequestId, conflictId) {
  return request({
    url: `/project/mr/${mergeRequestId}/merge-check/conflicts/${conflictId}/content`,
    method: 'get'
  })
}

export function resolveProjectMergeContentConflict(mergeRequestId, data) {
  return request({
    url: `/project/mr/${mergeRequestId}/merge-check/conflicts/content/resolve`,
    method: 'post',
    data
  })
}

export function getProjectPreMergeCheck(mergeRequestId) {
  return request({
    url: `/project/mr/${mergeRequestId}/pre-merge-check`,
    method: 'get'
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

import request from '@/utils/request'

export function listProjectReleases(projectId, params = {}) {
  return request({
    url: '/project/release/list',
    method: 'get',
    params: { projectId, ...params }
  })
}

export function getLatestProjectRelease(projectId) {
  return request({
    url: '/project/release/latest',
    method: 'get',
    params: { projectId }
  })
}

export function getProjectReleaseDetail(id) {
  return request({
    url: '/project/release/detail',
    method: 'get',
    params: { id }
  })
}

export function createProjectRelease(data) {
  return request({
    url: '/project/release',
    method: 'post',
    data
  })
}

export function updateProjectRelease(id, data) {
  return request({
    url: `/project/release/${id}`,
    method: 'put',
    data
  })
}

export function publishProjectRelease(id) {
  return request({
    url: `/project/release/${id}/publish`,
    method: 'put'
  })
}

export function archiveProjectRelease(id) {
  return request({
    url: `/project/release/${id}/archive`,
    method: 'put'
  })
}

export function bindProjectReleaseFiles(id, fileIds) {
  return request({
    url: `/project/release/${id}/bind-files`,
    method: 'post',
    data: fileIds
  })
}

export function listProjectReleaseBindableFiles(projectId, commitId) {
  return request({
    url: '/project/release/bindable-files',
    method: 'get',
    params: { projectId, commitId }
  })
}

export function removeProjectReleaseFile(id, releaseFileId) {
  return request({
    url: `/project/release/${id}/file/${releaseFileId}`,
    method: 'delete'
  })
}

import request from '@/utils/request'

export function listProjectDocs(projectId, params = {}) {
  return request({
    url: `/project/${projectId}/docs`,
    method: 'get',
    params
  })
}

export function getProjectPrimaryReadme(projectId) {
  return request({
    url: `/project/${projectId}/docs/primary-readme`,
    method: 'get'
  })
}

export function createProjectDoc(projectId, data) {
  return request({
    url: `/project/${projectId}/docs`,
    method: 'post',
    data
  })
}

export function getProjectDoc(docId) {
  return request({
    url: `/project/docs/${docId}`,
    method: 'get'
  })
}

export function updateProjectDoc(docId, data) {
  return request({
    url: `/project/docs/${docId}`,
    method: 'put',
    data
  })
}

export function setPrimaryProjectDoc(docId) {
  return request({
    url: `/project/docs/${docId}/primary`,
    method: 'put'
  })
}

export function listProjectDocSidebar(projectId) {
  return request({
    url: `/project/${projectId}/docs/sidebar`,
    method: 'get'
  })
}

export function deleteProjectDoc(docId) {
  return request({
    url: `/project/docs/${docId}`,
    method: 'delete'
  })
}

export function listProjectDocVersions(docId) {
  return request({
    url: `/project/docs/${docId}/versions`,
    method: 'get'
  })
}

export function getProjectDocVersion(docId, versionNo) {
  return request({
    url: `/project/docs/${docId}/versions/${versionNo}`,
    method: 'get'
  })
}

export function compareProjectDocVersions(docId, fromVersionNo, toVersionNo) {
  return request({
    url: `/project/docs/${docId}/versions/compare`,
    method: 'get',
    params: {
      fromVersionNo,
      toVersionNo
    }
  })
}

export function rollbackProjectDocVersion(docId, versionNo) {
  return request({
    url: `/project/docs/${docId}/rollback/${versionNo}`,
    method: 'post'
  })
}

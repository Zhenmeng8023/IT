import request from '@/utils/request'

export function getCurrentWorkspace(projectId, branchId) {
  return request({
    url: '/project/workspace/current',
    method: 'get',
    params: { projectId, branchId }
  })
}

export function getWorkspaceItems(projectId, branchId) {
  return request({
    url: '/project/workspace/items',
    method: 'get',
    params: { projectId, branchId }
  })
}

export function stageWorkspaceFile(projectId, branchId, canonicalPath, file) {
  const formData = new FormData()
  formData.append('projectId', projectId)
  formData.append('branchId', branchId)
  formData.append('canonicalPath', canonicalPath)
  formData.append('file', file)

  return request({
    url: '/project/workspace/stage-file',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function stageWorkspaceDelete(projectId, branchId, canonicalPath) {
  return request({
    url: '/project/workspace/stage-delete',
    method: 'post',
    params: { projectId, branchId, canonicalPath }
  })
}

export function commitWorkspace(data) {
  return request({
    url: '/project/workspace/commit',
    method: 'post',
    data
  })
}

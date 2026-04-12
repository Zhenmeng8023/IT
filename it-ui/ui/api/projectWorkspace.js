import request from '@/utils/request'

const WORKSPACE_UPLOAD_LOG_PREFIX = '[project-workspace-upload]'

function describeUploadFile(file) {
  if (!file) {
    return null
  }
  return {
    name: file.name || '',
    size: file.size || 0,
    type: file.type || '',
    relativePath: file.__relativePath || file.webkitRelativePath || file.relativePath || ''
  }
}

function logWorkspaceUpload(step, payload) {
  if (typeof console !== 'undefined' && console.info) {
    console.info(`${WORKSPACE_UPLOAD_LOG_PREFIX} ${step}`, payload)
  }
}

function logWorkspaceUploadError(step, error) {
  if (typeof console !== 'undefined' && console.error) {
    console.error(`${WORKSPACE_UPLOAD_LOG_PREFIX} ${step}`, {
      message: error && error.message,
      code: error && error.code,
      status: error && error.response && error.response.status,
      response: error && error.response && error.response.data
    })
  }
}

function withUploadDebug(promise, step) {
  return promise
    .then(response => {
      logWorkspaceUpload(`${step}:success`, response)
      return response
    })
    .catch(error => {
      logWorkspaceUploadError(`${step}:failed`, error)
      throw error
    })
}

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

  logWorkspaceUpload('stage-file:request', {
    projectId,
    branchId,
    canonicalPath,
    file: describeUploadFile(file)
  })

  return withUploadDebug(request({
    url: '/project/workspace/stage-file',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000
  }), 'stage-file')
}

export function stageWorkspaceZip(projectId, branchId, file) {
  const formData = new FormData()
  formData.append('projectId', projectId)
  formData.append('branchId', branchId)
  formData.append('file', file)

  return request({
    url: '/project/workspace/stage-zip',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000
  })
}

export function stageWorkspaceBatch(projectId, branchId, files = [], targetDir = '', relativePaths = []) {
  const formData = new FormData()
  formData.append('projectId', projectId)
  formData.append('branchId', branchId)
  if (targetDir) {
    formData.append('targetDir', targetDir)
  }
  ;(files || []).forEach((file, index) => {
    if (file) {
      const relativePath = relativePaths[index] ||
        file.__relativePath ||
        file.webkitRelativePath ||
        file.relativePath ||
        file.name ||
        ''
      formData.append('files', file, file.name || relativePath || 'file')
      formData.append('relativePaths', relativePath)
    }
  })

  logWorkspaceUpload('stage-batch:request', {
    projectId,
    branchId,
    targetDir,
    fileCount: (files || []).filter(Boolean).length,
    files: (files || []).filter(Boolean).slice(0, 20).map(describeUploadFile),
    relativePaths: (relativePaths || []).slice(0, 20)
  })

  return withUploadDebug(request({
    url: '/project/workspace/stage-batch',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000
  }), 'stage-batch')
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

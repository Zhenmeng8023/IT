import request from '@/utils/request'

const WORKSPACE_UPLOAD_LOG_PREFIX = '[project-workspace-upload]'
const WORKSPACE_BATCH_FILE_LIMIT = 20
const WORKSPACE_BATCH_BYTES_LIMIT = 20 * 1024 * 1024

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

function normalizeUploadPath(value) {
  return String(value || '')
    .replace(/\\/g, '/')
    .replace(/^[A-Za-z]:\/+/, '')
    .replace(/^\/+/, '')
    .split('/')
    .map(item => item.trim())
    .filter(Boolean)
    .filter(item => item !== '.')
    .join('/')
}

function buildBatchCanonicalKey(targetDir, relativePath) {
  const target = normalizeUploadPath(targetDir)
  const relative = normalizeUploadPath(relativePath)
  return `/${[target, relative].filter(Boolean).join('/')}`
}

function buildBatchEntries(files = [], relativePaths = []) {
  return (files || [])
    .map((file, index) => {
      if (!file) return null
      const relativePath = relativePaths[index] ||
        file.__relativePath ||
        file.webkitRelativePath ||
        file.relativePath ||
        file.name ||
        ''
      return {
        file,
        relativePath
      }
    })
    .filter(Boolean)
}

function assertNoDuplicateBatchPaths(entries, targetDir) {
  const seen = new Set()
  for (const entry of entries) {
    const canonicalPath = buildBatchCanonicalKey(targetDir, entry.relativePath || (entry.file && entry.file.name))
    if (seen.has(canonicalPath)) {
      throw new Error(`Duplicate upload path: ${canonicalPath}`)
    }
    seen.add(canonicalPath)
  }
}

function splitBatchEntries(entries) {
  const chunks = []
  let current = []
  let currentBytes = 0

  entries.forEach(entry => {
    const fileSize = entry && entry.file && entry.file.size ? entry.file.size : 0
    const shouldStartNextChunk = current.length > 0 &&
      (current.length >= WORKSPACE_BATCH_FILE_LIMIT ||
        currentBytes + fileSize > WORKSPACE_BATCH_BYTES_LIMIT)

    if (shouldStartNextChunk) {
      chunks.push(current)
      current = []
      currentBytes = 0
    }

    current.push(entry)
    currentBytes += fileSize
  })

  if (current.length) {
    chunks.push(current)
  }

  return chunks
}

function createBatchFormData(projectId, branchId, targetDir, entries) {
  const formData = new FormData()
  formData.append('projectId', projectId)
  formData.append('branchId', branchId)
  if (targetDir) {
    formData.append('targetDir', targetDir)
  }
  ;(entries || []).forEach(entry => {
    const file = entry.file
    const relativePath = entry.relativePath || file.__relativePath || file.webkitRelativePath || file.relativePath || file.name || ''
    formData.append('files', file, file.name || relativePath || 'file')
    formData.append('relativePaths', relativePath)
  })
  return formData
}

function postStageBatch(formData, timeout = 600000) {
  return request({
    url: '/project/workspace/stage-batch',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout
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
  const entries = buildBatchEntries(files, relativePaths)
  assertNoDuplicateBatchPaths(entries, targetDir)
  const chunks = splitBatchEntries(entries)

  logWorkspaceUpload('stage-batch:request', {
    projectId,
    branchId,
    targetDir,
    fileCount: entries.length,
    chunkCount: chunks.length,
    files: entries.slice(0, 20).map(entry => describeUploadFile(entry.file)),
    relativePaths: (relativePaths || []).slice(0, 20)
  })

  return withUploadDebug((async () => {
    const results = []
    for (let index = 0; index < chunks.length; index += 1) {
      const chunk = chunks[index]
      logWorkspaceUpload('stage-batch:chunk-request', {
        chunkIndex: index + 1,
        chunkCount: chunks.length,
        fileCount: chunk.length,
        files: chunk.slice(0, 20).map(entry => describeUploadFile(entry.file))
      })
      const response = await postStageBatch(createBatchFormData(projectId, branchId, targetDir, chunk))
      const data = response && response.data !== undefined ? response.data : response
      if (Array.isArray(data)) {
        results.push(...data)
      }
    }
    return {
      code: 200,
      message: 'ok',
      data: results
    }
  })(), 'stage-batch')
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

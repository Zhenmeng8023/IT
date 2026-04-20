import request from '@/utils/request'

const WORKSPACE_BATCH_FILE_LIMIT = 20
const WORKSPACE_BATCH_BYTES_LIMIT = 20 * 1024 * 1024

function normalizeUploadPath(value, options = {}) {
  const preserveNested = options.preserveNested !== false
  const segments = String(value || '')
    .replace(/\\/g, '/')
    .replace(/^[A-Za-z]:\/+/, '')
    .replace(/^\/+/, '')
    .split('/')
    .map(item => item.trim())
    .filter(Boolean)
    .filter(item => item !== '.')

  if (segments.some(item => item === '..' || item.includes(':'))) {
    throw new Error(`Invalid upload path: ${value}`)
  }
  if (!segments.length) {
    return ''
  }
  if (!preserveNested) {
    return segments[segments.length - 1]
  }
  return segments.join('/')
}

function buildBatchCanonicalKey(targetDir, relativePath) {
  const target = normalizeUploadPath(targetDir)
  const relative = normalizeUploadPath(relativePath)
  return `/${[target, relative].filter(Boolean).join('/')}`
}

function buildBatchEntries(files = [], relativePaths = []) {
  const hasRelativePaths = Array.isArray(relativePaths) && relativePaths.length > 0
  if (hasRelativePaths && relativePaths.length !== (files || []).length) {
    throw new Error('relativePaths length must match files length')
  }

  return (files || [])
    .map((file, index) => {
      if (!file) return null
      const rawRelativePath = hasRelativePaths
        ? relativePaths[index]
        : (file.__relativePath || file.webkitRelativePath || file.relativePath || file.name || '')
      const relativePath = normalizeUploadPath(rawRelativePath, {
        preserveNested: hasRelativePaths
      })
      if (!relativePath) {
        throw new Error(`Missing upload path for file index ${index}`)
      }
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
    const relativePath = entry.relativePath || file.name || ''
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

  return request({
    url: '/project/workspace/stage-file',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000
  })
}

export function stageWorkspaceZip(projectId, branchId, file) {
  return Promise.reject(new Error('ZIP import is disabled. Use stage-file or stage-batch.'))
}

export function stageWorkspaceBatch(projectId, branchId, files = [], targetDir = '', relativePaths = []) {
  const entries = buildBatchEntries(files, relativePaths)
  if (!entries.length) {
    throw new Error('No files to upload')
  }
  assertNoDuplicateBatchPaths(entries, targetDir)
  const chunks = splitBatchEntries(entries)

  return (async () => {
    const results = []
    for (let index = 0; index < chunks.length; index += 1) {
      const chunk = chunks[index]
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
  })()
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

import request from '@/utils/request'
import { buildAuthHeaders as buildSharedAuthHeaders } from '@/utils/auth'
import { API_V1_BASE_URL } from '@/utils/backend'
import { listEnabledAiModels } from '@/api/aiAdmin'
import {
  normalizeKnowledgeBaseEmbeddingPayload
} from '@/api/knowledgeBase'

const ADMIN_PLATFORM_KB_BASE = '/admin/ai/platform-knowledge-bases'

function normalizePageParams(params = {}) {
  return {
    page: params.page !== undefined ? params.page : 0,
    size: params.size !== undefined ? params.size : 10,
    ...params
  }
}

function normalizePlatformPayload(data = {}) {
  return {
    ...normalizeKnowledgeBaseEmbeddingPayload(data),
    scopeType: 'PLATFORM',
    projectId: null
  }
}

function buildFileFormData(files = [], fieldName = 'files') {
  const formData = new FormData()
  files.forEach(file => {
    formData.append(fieldName, file)
  })
  return formData
}

function getApiBaseUrl() {
  const baseURL =
    request && request.defaults && request.defaults.baseURL
      ? request.defaults.baseURL
      : API_V1_BASE_URL
  return String(baseURL).replace(/\/$/, '')
}

function buildAuthHeaders(extraHeaders = {}) {
  return {
    'Content-Type': 'application/json',
    ...buildSharedAuthHeaders(extraHeaders),
    ...extraHeaders
  }
}

function parseFileNameFromDisposition(disposition, fallback) {
  if (!disposition) return fallback
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match && utf8Match[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch (error) {
      return utf8Match[1]
    }
  }
  const normalMatch = disposition.match(/filename="?([^";]+)"?/i)
  if (normalMatch && normalMatch[1]) {
    return normalMatch[1]
  }
  return fallback
}

async function fetchBinary(url, options = {}, fallbackName = 'download.bin') {
  const response = await fetch(`${getApiBaseUrl()}${url}`, {
    credentials: 'include',
    headers: buildAuthHeaders(options.headers || {}),
    method: options.method || 'GET',
    body: options.body
  })

  if (!response.ok) {
    throw new Error(`Download failed: ${response.status}`)
  }

  const blob = await response.blob()
  const fileName = parseFileNameFromDisposition(
    response.headers.get('content-disposition'),
    fallbackName
  )
  return { blob, fileName }
}

export const adminPlatformKnowledgeBaseService = {
  fetchModels() {
    return listEnabledAiModels()
  },

  fetchKnowledgeBases({ page, size } = {}) {
    return request({
      url: ADMIN_PLATFORM_KB_BASE,
      method: 'get',
      params: normalizePageParams({ page, size })
    })
  },

  fetchKnowledgeBaseDetail(id) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/${id}`,
      method: 'get'
    })
  },

  saveKnowledgeBase(mode, payload, id) {
    const normalized = normalizePlatformPayload(payload)
    return mode === 'edit' && id
      ? request({
          url: `${ADMIN_PLATFORM_KB_BASE}/${id}`,
          method: 'put',
          data: normalized
        })
      : request({
          url: ADMIN_PLATFORM_KB_BASE,
          method: 'post',
          data: normalized
        })
  },

  fetchDocuments(knowledgeBaseId, page, size) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/${knowledgeBaseId}/documents`,
      method: 'get',
      params: normalizePageParams({ page, size })
    })
  },

  addDocument(knowledgeBaseId, payload) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/${knowledgeBaseId}/documents`,
      method: 'post',
      data: payload
    })
  },

  uploadDocuments(knowledgeBaseId, files = []) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/${knowledgeBaseId}/documents/upload`,
      method: 'post',
      data: buildFileFormData(files, 'files'),
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  uploadZip(knowledgeBaseId, file, extra = {}) {
    const formData = new FormData()
    formData.append('file', file)
    Object.keys(extra).forEach(key => {
      if (extra[key] !== undefined && extra[key] !== null && extra[key] !== '') {
        formData.append(key, extra[key])
      }
    })
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/${knowledgeBaseId}/documents/upload-zip`,
      method: 'post',
      data: formData,
      timeout: 10 * 60 * 1000,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  fetchDocumentChunks(documentId) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/documents/${documentId}/chunks`,
      method: 'get'
    })
  },

  previewDocumentChunks(documentId, payload = {}) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/documents/${documentId}/chunk-preview`,
      method: 'post',
      data: payload
    })
  },

  downloadDocument(documentId) {
    return fetchBinary(
      `${ADMIN_PLATFORM_KB_BASE}/documents/${documentId}/download`,
      { method: 'GET' },
      `knowledge-document-${documentId}.bin`
    )
  },

  downloadDocumentsZip(knowledgeBaseId, documentIds = []) {
    return fetchBinary(
      `${ADMIN_PLATFORM_KB_BASE}/${knowledgeBaseId}/documents/download-zip`,
      {
        method: 'POST',
        body: JSON.stringify({ documentIds })
      },
      `knowledge-base-${knowledgeBaseId}-documents.zip`
    )
  },

  fetchImportTask(taskId) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/import-tasks/${taskId}`,
      method: 'get'
    })
  },

  createIndexTask(knowledgeBaseId, payload = {}) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/${knowledgeBaseId}/index-tasks`,
      method: 'post',
      data: payload || {}
    })
  },

  fetchKnowledgeBaseTasks(knowledgeBaseId) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/${knowledgeBaseId}/index-tasks`,
      method: 'get'
    })
  },

  fetchDocumentTasks(documentId) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/documents/${documentId}/index-tasks`,
      method: 'get'
    })
  },

  fetchKnowledgeBaseEmbeddingStatus(knowledgeBaseId) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/${knowledgeBaseId}/embedding-status`,
      method: 'get'
    })
  },

  fetchDocumentEmbeddingStatus(documentId) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/documents/${documentId}/embedding-status`,
      method: 'get'
    })
  },

  backfillKnowledgeBaseEmbeddings(knowledgeBaseId, payload = {}) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/${knowledgeBaseId}/embedding-backfill`,
      method: 'post',
      data: payload
    })
  },

  backfillDocumentEmbeddings(documentId, payload = {}) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/documents/${documentId}/embedding-backfill`,
      method: 'post',
      data: payload
    })
  },

  debugSearch(knowledgeBaseId, payload) {
    return request({
      url: `${ADMIN_PLATFORM_KB_BASE}/${knowledgeBaseId}/search-debug`,
      method: 'post',
      data: payload
    })
  },

  normalizeKnowledgeBaseEmbeddingPayload: normalizePlatformPayload
}

export default adminPlatformKnowledgeBaseService

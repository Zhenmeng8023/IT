import request from '@/utils/request'
import { getToken } from '@/utils/auth'

const KB_BASE = '/ai/knowledge-bases'
const SESSION_BASE = '/ai/sessions'
const CHAT_BASE = '/ai/chat'
const LOG_BASE = '/ai/logs'

function normalizePageParams(params = {}) {
  return {
    page: params.page !== undefined ? params.page : 0,
    size: params.size !== undefined ? params.size : 10,
    ...params
  }
}

function safeJsonParse(raw, fallback = null) {
  try {
    return JSON.parse(raw)
  } catch (e) {
    return fallback
  }
}

function readUserInfo() {
  if (typeof window === 'undefined') return null
  const candidates = [
    localStorage.getItem('userInfo'),
    localStorage.getItem('user'),
    sessionStorage.getItem('userInfo'),
    sessionStorage.getItem('user')
  ]
  for (const item of candidates) {
    if (!item) continue
    const parsed = safeJsonParse(item, null)
    if (parsed && typeof parsed === 'object') return parsed
  }
  return null
}

function readUserId() {
  const user = readUserInfo()
  if (!user) return null
  return user.id || user.userId || user.uid || null
}

function getApiBaseUrl() {
  const baseURL =
    request && request.defaults && request.defaults.baseURL
      ? request.defaults.baseURL
      : 'http://localhost:18080/api'
  return String(baseURL).replace(/\/$/, '')
}

function readToken() {
  const tokenFromAuth = typeof getToken === 'function' ? getToken() : ''
  if (tokenFromAuth) return tokenFromAuth
  if (typeof window !== 'undefined') {
    return (
      localStorage.getItem('token') ||
      localStorage.getItem('Authorization') ||
      sessionStorage.getItem('token') ||
      sessionStorage.getItem('Authorization') ||
      ''
    )
  }
  return ''
}

function buildAuthHeaders(extraHeaders = {}) {
  const token = readToken()
  const rawToken = token && token.startsWith('Bearer ') ? token.slice(7).trim() : token
  return {
    'Content-Type': 'application/json',
    ...(rawToken ? { Authorization: `Bearer ${rawToken}`, 'X-Token': rawToken } : {}),
    ...extraHeaders
  }
}

function parseFileNameFromDisposition(disposition, fallback) {
  if (!disposition) return fallback
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match && utf8Match[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch (e) {
      return utf8Match[1]
    }
  }
  const normalMatch = disposition.match(/filename="?([^";]+)"?/i)
  if (normalMatch && normalMatch[1]) {
    return normalMatch[1]
  }
  return fallback
}

export function pageKnowledgeBasesByOwner(ownerId, params = {}) {
  return request({
    url: `${KB_BASE}/owner/${ownerId}`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function pageKnowledgeBasesByProject(projectId, params = {}) {
  return request({
    url: `${KB_BASE}/project/${projectId}`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function getKnowledgeBase(id) {
  return request({
    url: `${KB_BASE}/${id}`,
    method: 'get'
  })
}

export function createKnowledgeBase(data) {
  return request({
    url: KB_BASE,
    method: 'post',
    data
  })
}

export function updateKnowledgeBase(id, data) {
  return request({
    url: `${KB_BASE}/${id}`,
    method: 'put',
    data
  })
}

export function pageKnowledgeDocuments(knowledgeBaseId, params = {}) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/documents`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function addKnowledgeDocument(knowledgeBaseId, data) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/documents`,
    method: 'post',
    data
  })
}

export function uploadKnowledgeDocuments(knowledgeBaseId, formData) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/documents/upload`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function uploadKnowledgeDocumentsZipWithProgress(knowledgeBaseId, formData, options = {}) {
  const { onProgress, timeout = 0 } = options
  const xhr = new XMLHttpRequest()
  const promise = new Promise((resolve, reject) => {
    xhr.open('POST', `${getApiBaseUrl()}${KB_BASE}/${knowledgeBaseId}/documents/upload-zip`, true)
    const token = readToken()
    const rawToken = token && token.startsWith('Bearer ') ? token.slice(7).trim() : token
    if (rawToken) {
      xhr.setRequestHeader('Authorization', `Bearer ${rawToken}`)
      xhr.setRequestHeader('X-Token', rawToken)
    }
    if (timeout && timeout > 0) {
      xhr.timeout = timeout
    }
    xhr.upload.onprogress = evt => {
      if (!evt.lengthComputable) return
      const percent = Math.max(0, Math.min(100, Math.round((evt.loaded / evt.total) * 100)))
      onProgress && onProgress({ ...evt, percent })
    }
    xhr.onreadystatechange = () => {
      if (xhr.readyState !== 4) return
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const data = xhr.responseText ? JSON.parse(xhr.responseText) : null
          resolve(data)
        } catch (e) {
          resolve(xhr.responseText)
        }
      } else if (xhr.status !== 0) {
        reject(new Error(`ZIP 上传失败: ${xhr.status}`))
      }
    }
    xhr.onerror = () => reject(new Error('ZIP 上传失败，网络异常'))
    xhr.ontimeout = () => reject(new Error('ZIP 上传超时'))
    xhr.onabort = () => reject(new Error('上传已取消'))
    xhr.send(formData)
  })
  promise.abort = () => xhr.abort()
  return promise
}

export function getKnowledgeImportTask(taskId) {
  return request({
    url: `/ai/knowledge-import-tasks/${taskId}`,
    method: 'get'
  })
}

export function listKnowledgeImportTasks(knowledgeBaseId) {
  return request({
    url: `/ai/knowledge-import-tasks/knowledge-base/${knowledgeBaseId}`,
    method: 'get'
  })
}

export function cancelKnowledgeImportTask(taskId) {
  return request({
    url: `/ai/knowledge-import-tasks/${taskId}/cancel`,
    method: 'post'
  })
}

export function listDocumentChunks(documentId) {
  return request({
    url: `${KB_BASE}/documents/${documentId}/chunks`,
    method: 'get'
  })
}


export function previewDocumentChunks(documentId, data = {}) {
  return request({
    url: `${KB_BASE}/documents/${documentId}/chunk-preview`,
    method: 'post',
    data
  })
}

export function backfillKnowledgeBaseEmbeddings(knowledgeBaseId, data = {}) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/embedding-backfill`,
    method: 'post',
    data
  })
}

export function backfillDocumentEmbeddings(documentId, data = {}) {
  return request({
    url: `${KB_BASE}/documents/${documentId}/embedding-backfill`,
    method: 'post',
    data
  })
}

export function getKnowledgeBaseEmbeddingStatus(knowledgeBaseId) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/embedding-status`,
    method: 'get'
  })
}

export function getDocumentEmbeddingStatus(documentId) {
  return request({
    url: `${KB_BASE}/documents/${documentId}/embedding-status`,
    method: 'get'
  })
}

export function searchKnowledgeBaseDebug(knowledgeBaseId, data = {}) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/search-debug`,
    method: 'post',
    data
  })
}

export async function downloadKnowledgeDocument(documentId) {
  const fallbackName = `knowledge-document-${documentId}.bin`
  const response = await fetch(`${getApiBaseUrl()}${KB_BASE}/documents/${documentId}/download`, {
    method: 'GET',
    headers: buildAuthHeaders({})
  })
  if (!response.ok) {
    throw new Error(`下载文件失败: ${response.status}`)
  }
  const blob = await response.blob()
  const fileName = parseFileNameFromDisposition(
    response.headers.get('content-disposition'),
    fallbackName
  )
  return { blob, fileName }
}

export async function downloadKnowledgeDocumentsZip(knowledgeBaseId, documentIds = []) {
  const fallbackName = `knowledge-base-${knowledgeBaseId}-documents.zip`
  const response = await fetch(`${getApiBaseUrl()}${KB_BASE}/${knowledgeBaseId}/documents/download-zip`, {
    method: 'POST',
    headers: buildAuthHeaders({}),
    body: JSON.stringify({ documentIds })
  })
  if (!response.ok) {
    throw new Error(`打包下载失败: ${response.status}`)
  }
  const blob = await response.blob()
  const fileName = parseFileNameFromDisposition(
    response.headers.get('content-disposition'),
    fallbackName
  )
  return { blob, fileName }
}

export function listKnowledgeBaseMembers(knowledgeBaseId) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/members`,
    method: 'get'
  })
}

export function addKnowledgeBaseMember(knowledgeBaseId, data) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/members`,
    method: 'post',
    data
  })
}

export function removeKnowledgeBaseMember(knowledgeBaseId, memberId) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/members/${memberId}`,
    method: 'delete'
  })
}

export function createKnowledgeIndexTask(knowledgeBaseId, data = {}) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/index-tasks`,
    method: 'post',
    data: data || {}
  })
}

export function listKnowledgeBaseIndexTasks(knowledgeBaseId) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/index-tasks`,
    method: 'get'
  })
}

export function listDocumentIndexTasks(documentId) {
  return request({
    url: `${KB_BASE}/documents/${documentId}/index-tasks`,
    method: 'get'
  })
}

export function createAiSession(data) {
  return request({
    url: SESSION_BASE,
    method: 'post',
    data
  })
}

export function getAiSession(sessionId) {
  return request({
    url: `${SESSION_BASE}/${sessionId}`,
    method: 'get'
  })
}

export function pageAiSessions(params = {}) {
  const normalized = normalizePageParams(params)
  if (!normalized.userId) {
    const userId = readUserId()
    if (userId) normalized.userId = userId
  }
  return request({
    url: SESSION_BASE,
    method: 'get',
    params: normalized
  })
}

export function pageAiSessionMessages(sessionId, params = {}) {
  return request({
    url: `${SESSION_BASE}/${sessionId}/messages`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function bindSessionKnowledgeBases(sessionId, knowledgeBaseIds = []) {
  return request({
    url: `${SESSION_BASE}/${sessionId}/knowledge-bases`,
    method: 'put',
    data: { knowledgeBaseIds }
  })
}

export function archiveAiSession(sessionId) {
  return request({
    url: `${SESSION_BASE}/${sessionId}/archive`,
    method: 'put'
  })
}

export function deleteAiSession(sessionId) {
  return request({
    url: `${SESSION_BASE}/${sessionId}`,
    method: 'delete'
  })
}

export function chatWithKnowledgeBase(data) {
  return request({
    url: `${CHAT_BASE}/turn`,
    method: 'post',
    data
  })
}

export function pageSessionCallLogs(sessionId, params = {}) {
  return request({
    url: `${LOG_BASE}/session/${sessionId}/calls`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function listCallRetrievals(callLogId) {
  return request({
    url: `${LOG_BASE}/call/${callLogId}/retrievals`,
    method: 'get'
  })
}

export function streamChatWithKnowledgeBase({ body, onMessage, onError, onFinish, headers = {} }) {
  const controller = new AbortController()

  const promise = (async () => {
    try {
      const response = await fetch(`${getApiBaseUrl()}${CHAT_BASE}/stream`, {
        method: 'POST',
        headers: buildAuthHeaders(headers),
        body: JSON.stringify(body || {}),
        signal: controller.signal
      })

      if (!response.ok) {
        throw new Error(`流式请求失败: ${response.status}`)
      }

      if (!response.body) {
        throw new Error('当前浏览器不支持流式响应')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { value, done } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split('\n\n')
        buffer = parts.pop() || ''

        for (const part of parts) {
          const lines = part
            .split('\n')
            .map(line => line.trim())
            .filter(Boolean)

          for (const line of lines) {
            if (!line.startsWith('data:')) continue
            const raw = line.slice(5).trim()
            if (!raw) continue
            try {
              onMessage && onMessage(JSON.parse(raw))
            } catch (e) {
              onMessage && onMessage(raw)
            }
          }
        }
      }

      if (buffer) {
        const lines = buffer
          .split('\n')
          .map(line => line.trim())
          .filter(Boolean)

        for (const line of lines) {
          if (!line.startsWith('data:')) continue
          const raw = line.slice(5).trim()
          if (!raw) continue
          try {
            onMessage && onMessage(JSON.parse(raw))
          } catch (e) {
            onMessage && onMessage(raw)
          }
        }
      }

      onFinish && onFinish()
    } catch (err) {
      if (err && err.name !== 'AbortError') {
        onError && onError(err)
        throw err
      }
    }
  })()

  promise.abort = () => controller.abort()
  return promise
}

export function uploadKnowledgeDocumentsZip(knowledgeBaseId, formData) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/documents/upload-zip`,
    method: 'post',
    data: formData,
    timeout: 10 * 60 * 1000,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export default {
  pageKnowledgeBasesByOwner,
  pageKnowledgeBasesByProject,
  getKnowledgeBase,
  createKnowledgeBase,
  updateKnowledgeBase,
  pageKnowledgeDocuments,
  addKnowledgeDocument,
  uploadKnowledgeDocuments,
  uploadKnowledgeDocumentsZip,
  uploadKnowledgeDocumentsZipWithProgress,
  getKnowledgeImportTask,
  listKnowledgeImportTasks,
  cancelKnowledgeImportTask,
  listDocumentChunks,
  previewDocumentChunks,
  downloadKnowledgeDocument,
  downloadKnowledgeDocumentsZip,
  listKnowledgeBaseMembers,
  addKnowledgeBaseMember,
  removeKnowledgeBaseMember,
  createKnowledgeIndexTask,
  listKnowledgeBaseIndexTasks,
  listDocumentIndexTasks,
  backfillKnowledgeBaseEmbeddings,
  backfillDocumentEmbeddings,
  getKnowledgeBaseEmbeddingStatus,
  getDocumentEmbeddingStatus,
  searchKnowledgeBaseDebug,
  createAiSession,
  getAiSession,
  pageAiSessions,
  pageAiSessionMessages,
  bindSessionKnowledgeBases,
  archiveAiSession,
  deleteAiSession,
  chatWithKnowledgeBase,
  pageSessionCallLogs,
  listCallRetrievals,
  streamChatWithKnowledgeBase
}
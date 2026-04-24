import request from '@/utils/request'
import { buildAuthHeaders as buildSharedAuthHeaders, getCurrentUser } from '@/utils/auth'
import { API_V1_BASE_URL } from '@/utils/backend'
import {
  classifyAiError,
  consumeSseBuffer,
  isTerminalStreamChunk,
  parseStreamPayload
} from '@/utils/aiRuntime'

const KB_BASE = '/ai/knowledge-bases'
const FRONT_KB_BASE = '/ai/front/knowledge-bases'
const ADMIN_KB_BASE = '/admin/ai/knowledge-bases'
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
  const currentUser = getCurrentUser()
  return currentUser && typeof currentUser === 'object' ? currentUser : null
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

export async function pageMyKnowledgeBases(params = {}, options = {}) {
  const normalizedParams = normalizePageParams(params)
  try {
    return await request({
      url: `${KB_BASE}/my`,
      method: 'get',
      params: normalizedParams
    })
  } catch (error) {
    const status = Number((error && (error.status || (error.response && error.response.status))) || 0)
    const shouldFallbackToOwner = status === 400 || status === 404 || status === 405
    if (!shouldFallbackToOwner) throw error

    const fallbackOwnerId = options.ownerId || readUserId()
    if (!fallbackOwnerId) throw error

    return request({
      url: `${KB_BASE}/owner/${fallbackOwnerId}`,
      method: 'get',
      params: normalizedParams
    })
  }
}

export function pageMyFrontKnowledgeBases(params = {}) {
  return request({
    url: `${FRONT_KB_BASE}/my`,
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

export function pageFrontKnowledgeBasesByProject(projectId, params = {}) {
  return request({
    url: `${FRONT_KB_BASE}/projects/${projectId}`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function pageAllKnowledgeBases(params = {}) {
  return request({
    url: ADMIN_KB_BASE,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function freezeAdminKnowledgeBase(id) {
  return request({
    url: `${ADMIN_KB_BASE}/${id}/freeze`,
    method: 'put'
  })
}

export function archiveAdminKnowledgeBase(id) {
  return request({
    url: `${ADMIN_KB_BASE}/${id}/archive`,
    method: 'put'
  })
}

export function deleteAdminKnowledgeBase(id) {
  return request({
    url: `${ADMIN_KB_BASE}/${id}`,
    method: 'delete'
  })
}

export function getKnowledgeBase(id) {
  return request({
    url: `${KB_BASE}/${id}`,
    method: 'get'
  })
}

export function getFrontKnowledgeBase(id) {
  return request({
    url: `${FRONT_KB_BASE}/${id}`,
    method: 'get'
  })
}

export function createKnowledgeBase(data) {
  return request({
    url: KB_BASE,
    method: 'post',
    data: normalizeKnowledgeBaseEmbeddingPayload(data)
  })
}

export function createFrontKnowledgeBase(data) {
  return request({
    url: FRONT_KB_BASE,
    method: 'post',
    data: normalizeKnowledgeBaseEmbeddingPayload(data)
  })
}

export function updateKnowledgeBase(id, data) {
  return request({
    url: `${KB_BASE}/${id}`,
    method: 'put',
    data: normalizeKnowledgeBaseEmbeddingPayload(data)
  })
}

export function updateFrontKnowledgeBase(id, data) {
  return request({
    url: `${FRONT_KB_BASE}/${id}`,
    method: 'put',
    data: normalizeKnowledgeBaseEmbeddingPayload(data)
  })
}

export function deleteKnowledgeBase(id) {
  return request({
    url: `${KB_BASE}/${id}`,
    method: 'delete'
  })
}

export function deleteFrontKnowledgeBase(id) {
  return request({
    url: `${FRONT_KB_BASE}/${id}`,
    method: 'delete'
  })
}

export function pageKnowledgeDocuments(knowledgeBaseId, params = {}) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/documents`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function pageFrontKnowledgeDocuments(knowledgeBaseId, params = {}) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/documents`,
    method: 'get',
    params: normalizePageParams(params)
  })
}

export function deleteFrontKnowledgeDocument(knowledgeBaseId, documentId) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/documents/${documentId}`,
    method: 'delete'
  })
}

export function addKnowledgeDocument(knowledgeBaseId, data) {
  return request({
    url: `${KB_BASE}/${knowledgeBaseId}/documents`,
    method: 'post',
    data
  })
}

export function addFrontKnowledgeDocument(knowledgeBaseId, data) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/documents`,
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

export function uploadFrontKnowledgeDocuments(knowledgeBaseId, formData) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/documents/upload`,
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
    xhr.withCredentials = true
    const authHeaders = buildSharedAuthHeaders({})
    Object.keys(authHeaders).forEach(key => {
      xhr.setRequestHeader(key, authHeaders[key])
    })
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
        const err = new Error(`ZIP 上传失败: ${xhr.status}`)
        err.status = xhr.status
        reject(err)
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

export function getFrontKnowledgeImportTask(taskId) {
  return request({
    url: `${FRONT_KB_BASE}/import-tasks/${taskId}`,
    method: 'get'
  })
}

export function listKnowledgeImportTasks(knowledgeBaseId) {
  return request({
    url: `/ai/knowledge-import-tasks/knowledge-base/${knowledgeBaseId}`,
    method: 'get'
  })
}

export function listFrontKnowledgeImportTasks(knowledgeBaseId) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/import-tasks`,
    method: 'get'
  })
}

export function listFrontKnowledgeBaseIndexTasks(knowledgeBaseId) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/index-tasks`,
    method: 'get'
  })
}

export function createFrontKnowledgeIndexTask(knowledgeBaseId, data = {}) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/index-tasks`,
    method: 'post',
    data
  })
}

export function listFrontDocumentIndexTasks(documentId) {
  return request({
    url: `${FRONT_KB_BASE}/documents/${documentId}/index-tasks`,
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

export function listFrontDocumentChunks(documentId) {
  return request({
    url: `${FRONT_KB_BASE}/documents/${documentId}/chunks`,
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

export function getFrontKnowledgeBaseEmbeddingStatus(knowledgeBaseId) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/embedding-status`,
    method: 'get'
  })
}

export function getDocumentEmbeddingStatus(documentId) {
  return request({
    url: `${KB_BASE}/documents/${documentId}/embedding-status`,
    method: 'get'
  })
}

export function getFrontDocumentEmbeddingStatus(documentId) {
  return request({
    url: `${FRONT_KB_BASE}/documents/${documentId}/embedding-status`,
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


export async function fetchKnowledgeBaseEmbeddingRuntimeState(knowledgeBaseId) {
  const status = await getKnowledgeBaseEmbeddingStatus(knowledgeBaseId)
  let tasks = []
  try {
    tasks = await listKnowledgeBaseIndexTasks(knowledgeBaseId)
  } catch (e) {
    tasks = []
  }
  return { status, tasks }
}

export function isConflictRequest(error) {
  return !!(error && error.response && Number(error.response.status || 0) === 409)
}

export async function downloadKnowledgeDocument(documentId) {
  const fallbackName = `knowledge-document-${documentId}.bin`
  const response = await fetch(`${getApiBaseUrl()}${KB_BASE}/documents/${documentId}/download`, {
    method: 'GET',
    headers: buildAuthHeaders({}),
    credentials: 'include'
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

export async function downloadFrontKnowledgeDocument(documentId) {
  const fallbackName = `knowledge-document-${documentId}.bin`
  const response = await fetch(`${getApiBaseUrl()}${FRONT_KB_BASE}/documents/${documentId}/download`, {
    method: 'GET',
    headers: buildAuthHeaders({}),
    credentials: 'include'
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
    credentials: 'include',
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

export async function downloadFrontKnowledgeDocumentsZip(knowledgeBaseId, documentIds = []) {
  const fallbackName = `knowledge-base-${knowledgeBaseId}-documents.zip`
  const response = await fetch(`${getApiBaseUrl()}${FRONT_KB_BASE}/${knowledgeBaseId}/documents/download-zip`, {
    method: 'POST',
    headers: buildAuthHeaders({}),
    credentials: 'include',
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

export function listFrontKnowledgeBaseMembers(knowledgeBaseId) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/members`,
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

export function addFrontKnowledgeBaseMember(knowledgeBaseId, data) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/members`,
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

export function removeFrontKnowledgeBaseMember(knowledgeBaseId, memberId) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/members/${memberId}`,
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
    let finishedByChunk = false
    try {
      const response = await fetch(`${getApiBaseUrl()}${CHAT_BASE}/stream`, {
        method: 'POST',
        headers: buildAuthHeaders(headers),
        credentials: 'include',
        body: JSON.stringify(body || {}),
        signal: controller.signal
      })

      if (!response.ok) {
        const err = new Error(`流式请求失败: ${response.status}`)
        err.status = response.status
        err.response = { status: response.status, data: { message: err.message } }
        throw err
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
        buffer = consumeSseBuffer(buffer, event => {
          const chunk = parseStreamPayload(event.data)
          if (chunk === null || chunk === undefined) return
          if (isTerminalStreamChunk(chunk)) {
            finishedByChunk = true
          }
          onMessage && onMessage(chunk)
        })
      }

      if (buffer) {
        consumeSseBuffer(`${buffer}\n\n`, event => {
          const chunk = parseStreamPayload(event.data)
          if (chunk === null || chunk === undefined) return
          if (isTerminalStreamChunk(chunk)) {
            finishedByChunk = true
          }
          onMessage && onMessage(chunk)
        })
      }

      onFinish && onFinish({ finishedByChunk })
    } catch (err) {
      if (err && err.name !== 'AbortError') {
        err.aiError = classifyAiError(err)
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

export function uploadFrontKnowledgeDocumentsZip(knowledgeBaseId, formData) {
  return request({
    url: `${FRONT_KB_BASE}/${knowledgeBaseId}/documents/upload-zip`,
    method: 'post',
    data: formData,
    timeout: 10 * 60 * 1000,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export default {
  pageMyKnowledgeBases,
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
  deleteKnowledgeBase,
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

const AI_MODEL_BASE = '/ai/models'

export function listEnabledEmbeddingModels() {
  return request({
    url: `${AI_MODEL_BASE}/enabled`,
    method: 'get'
  })
}

export function normalizeKnowledgeBaseEmbeddingPayload(data = {}) {
  return {
    ...data,
    embeddingProvider: String(data.embeddingProvider || '').trim(),
    embeddingModel: String(data.embeddingModel || '').trim()
  }
}

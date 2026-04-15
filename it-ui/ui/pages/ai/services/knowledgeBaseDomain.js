export function safeParsePermissionPayload(raw) {
  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}

export function appendPermissionCodes(target, source) {
  if (!source) return
  if (Array.isArray(source)) {
    source.forEach(item => appendPermissionCodes(target, item))
    return
  }
  if (typeof source === 'string') {
    const code = source.trim()
    if (code) target.add(code)
    return
  }
  if (typeof source !== 'object') return

  ;[
    source.permissionCode,
    source.permission,
    source.code,
    source.authority,
    source.value,
    source.name
  ].forEach(item => appendPermissionCodes(target, item))

  ;[
    source.permissions,
    source.permissionCodes,
    source.authorities,
    source.roles,
    source.menus,
    source.children,
    source.buttonPermissions
  ].forEach(item => appendPermissionCodes(target, item))
}

export function readBrowserPermissionCodes() {
  if (typeof window === 'undefined') return []
  const set = new Set()
  const storages = [window.localStorage, window.sessionStorage]
  const keys = ['permissions', 'permissionCodes', 'authorities', 'menus', 'userInfo', 'user', 'loginUser']

  storages.forEach(storage => {
    keys.forEach(key => {
      try {
        const raw = storage.getItem(key)
        if (!raw) return
        const parsed = safeParsePermissionPayload(raw)
        if (parsed == null) {
          appendPermissionCodes(set, raw)
        } else {
          appendPermissionCodes(set, parsed)
        }
      } catch (error) {
      }
    })
  })

  return Array.from(set)
}

export function extractResponseData(res) {
  if (!res) return null
  const first = Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
  if (
    first &&
    typeof first === 'object' &&
    Object.prototype.hasOwnProperty.call(first, 'data') &&
    (Object.prototype.hasOwnProperty.call(first, 'code') ||
      Object.prototype.hasOwnProperty.call(first, 'msg') ||
      Object.prototype.hasOwnProperty.call(first, 'message'))
  ) {
    return first.data
  }
  return first
}

export function extractResponseMessage(res, fallback = '') {
  if (!res) return fallback
  const first = Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
  return first.msg || first.message || fallback
}

export function extractPageData(res) {
  const data = extractResponseData(res) || {}
  return {
    content: data.content || data.records || data.list || [],
    total: data.totalElements || data.total || 0
  }
}

export function extractListData(res) {
  const data = extractResponseData(res)
  if (Array.isArray(data)) return data
  if (data && Array.isArray(data.content)) return data.content
  if (data && Array.isArray(data.records)) return data.records
  if (data && Array.isArray(data.list)) return data.list
  return []
}

export function getKnowledgeBaseModelStorageKey(kbId) {
  if (!kbId) return ''
  return `kb_default_model_${kbId}`
}

export function readKnowledgeBaseDefaultModel(kbId) {
  const key = getKnowledgeBaseModelStorageKey(kbId)
  if (!key || typeof localStorage === 'undefined') return null
  const raw = localStorage.getItem(key)
  return raw ? Number(raw) || null : null
}

export function persistKnowledgeBaseDefaultModel(kbId, modelId) {
  const key = getKnowledgeBaseModelStorageKey(kbId)
  if (!key || typeof localStorage === 'undefined') return
  if (modelId) {
    localStorage.setItem(key, String(modelId))
  } else {
    localStorage.removeItem(key)
  }
}

export function getSessionStorageKey(kbId, userId) {
  const uid = userId || 'guest'
  if (!kbId) return ''
  return `kb_chat_session_${uid}_${kbId}`
}

export function readPersistedSessionId(kbId, userId) {
  const key = getSessionStorageKey(kbId, userId)
  if (!key || typeof localStorage === 'undefined') return null
  const raw = localStorage.getItem(key)
  return raw ? Number(raw) || null : null
}

export function persistSessionId(kbId, userId, sessionId) {
  const key = getSessionStorageKey(kbId, userId)
  if (!key || typeof localStorage === 'undefined') return
  if (sessionId) {
    localStorage.setItem(key, String(sessionId))
  } else {
    localStorage.removeItem(key)
  }
}

export function persistCurrentKnowledgeBaseToAssistant(kb) {
  if (typeof window === 'undefined') return

  if (!kb || !kb.id) {
    localStorage.removeItem('ai_assistant_current_kb')
    window.dispatchEvent(new CustomEvent('ai-assistant-kb-change', { detail: null }))
    return
  }

  const payload = {
    id: kb.id,
    name: kb.name || '',
    ownerId: kb.ownerId || null,
    projectId: kb.projectId || null,
    scopeType: kb.scopeType || '',
    description: kb.description || '',
    status: kb.status || '',
    updatedAt: Date.now()
  }

  localStorage.setItem('ai_assistant_current_kb', JSON.stringify(payload))
  window.dispatchEvent(new CustomEvent('ai-assistant-kb-change', { detail: payload }))
}

export function normalizeKnowledgeBase(raw = {}) {
  return {
    id: raw.id || null,
    ownerId: raw.ownerId || raw.userId || null,
    scopeType: raw.scopeType || raw.scope || '',
    projectId: raw.projectId || null,
    name: raw.name || '',
    description: raw.description || '',
    sourceType: raw.sourceType || '',
    embeddingProvider: raw.embeddingProvider || '',
    embeddingModel: raw.embeddingModel || '',
    chunkStrategy: raw.chunkStrategy || '',
    defaultTopK: raw.defaultTopK || raw.topK || null,
    defaultModelId: raw.defaultModelId || raw.defaultAiModelId || raw.modelId || readKnowledgeBaseDefaultModel(raw.id || null) || null,
    visibility: raw.visibility || '',
    status: raw.status || '',
    lastIndexedAt: raw.lastIndexedAt || null,
    createdAt: raw.createdAt || null,
    updatedAt: raw.updatedAt || null
  }
}

export function normalizeDocument(raw = {}) {
  return {
    id: raw.id || raw.documentId || null,
    title: raw.title || raw.name || '',
    sourceType: raw.sourceType || '',
    status: raw.status || '',
    indexedAt: raw.indexedAt || null,
    updatedAt: raw.updatedAt || raw.createdAt || null,
    fileType: raw.fileType || raw.contentType || raw.extension || '',
    archiveName: raw.archiveName || raw.zipFileName || '',
    archiveEntryPath: raw.archiveEntryPath || raw.filePath || raw.relativePath || '',
    importBatchId: raw.importBatchId || raw.batchId || '',
    createdAt: raw.createdAt || null
  }
}

export function normalizeMember(raw = {}) {
  return {
    id: raw.id || null,
    userId: raw.userId || null,
    roleCode: raw.roleCode || raw.role || '',
    grantedBy: raw.grantedBy || raw.grantedByUserId || null,
    createdAt: raw.createdAt || null
  }
}

export function normalizeChunk(raw = {}) {
  return {
    id: raw.id || raw.chunkId || null,
    chunkIndex:
      raw.chunkIndex !== undefined && raw.chunkIndex !== null
        ? raw.chunkIndex
        : raw.rankNo !== undefined && raw.rankNo !== null
          ? raw.rankNo
          : null,
    tokenCount: raw.tokenCount || raw.tokens || null,
    content: raw.content || raw.text || ''
  }
}

export function normalizeTask(raw = {}) {
  return {
    id: raw.id || raw.taskId || null,
    taskType: raw.taskType || raw.type || '',
    status: raw.status || '',
    documentId: raw.documentId || null,
    message: raw.message || raw.errorMessage || '',
    createdAt: raw.createdAt || null,
    updatedAt: raw.updatedAt || raw.finishedAt || null
  }
}

export function normalizeSession(raw = {}) {
  return {
    id: raw.id || raw.sessionId || null,
    title: raw.sessionTitle || raw.title || raw.name || '',
    archived: !!(raw.archived || raw.isArchived || raw.status === 'ARCHIVED'),
    updatedAt: raw.updatedAt || raw.lastMessageAt || raw.modifiedAt || raw.createdAt || raw.createTime || null,
    createdAt: raw.createdAt || raw.createTime || null,
    defaultKnowledgeBaseId:
      raw.defaultKnowledgeBaseId ||
      (raw.defaultKnowledgeBase && raw.defaultKnowledgeBase.id) ||
      (raw.knowledgeBase && raw.knowledgeBase.id) ||
      null,
    knowledgeBaseIds:
      raw.knowledgeBaseIds ||
      raw.kbIds ||
      (Array.isArray(raw.knowledgeBases) ? raw.knowledgeBases.map(item => item.id) : [])
  }
}

export function normalizeSources(list = [], extra = {}) {
  if (!Array.isArray(list)) return []
  return list.map(item => {
    const document = item.document || {}
    const chunk = item.chunk || {}
    const knowledgeBase = item.knowledgeBase || {}
    return {
      id: item.id || item.chunkId || chunk.id || item.documentId || document.id || null,
      callLogId: extra.callLogId || item.callLogId || null,
      title: item.title || item.documentTitle || item.documentName || document.title || '',
      documentId: item.documentId || document.id || null,
      chunkId: item.chunkId || chunk.id || null,
      knowledgeBaseName: item.knowledgeBaseName || item.kbName || knowledgeBase.name || '',
      chunkIndex:
        item.chunkIndex !== undefined && item.chunkIndex !== null
          ? item.chunkIndex
          : chunk.chunkIndex !== undefined && chunk.chunkIndex !== null
            ? chunk.chunkIndex
            : item.rankNo !== undefined && item.rankNo !== null
              ? item.rankNo
              : null,
      rankNo: item.rankNo !== undefined && item.rankNo !== null ? item.rankNo : null,
      score: item.score !== undefined ? item.score : null,
      retrievalMethod: item.retrievalMethod || item.method || '',
      keywordScore: item.keywordScore !== undefined ? item.keywordScore : null,
      vectorScore: item.vectorScore !== undefined ? item.vectorScore : null,
      fileName: item.fileName || document.fileName || '',
      archiveEntryPath: item.archiveEntryPath || item.path || document.archiveEntryPath || '',
      content: item.content || item.chunkContent || item.text || item.snippet || chunk.content || ''
    }
  })
}

export function extractAnswerSources(data = {}, extra = {}) {
  const sourceList =
    data.sources ||
    data.citations ||
    data.references ||
    data.retrievals ||
    data.retrievedChunks ||
    []
  return normalizeSources(sourceList, extra)
}

export function normalizeMessage(raw = {}) {
  const role = String(raw.role || '').toLowerCase()
  return {
    id: raw.id || null,
    role: role === 'assistant' || role === 'user' ? role : role === 'tool' ? 'assistant' : 'user',
    content: raw.content || '',
    totalTokens: raw.totalTokens || null,
    latencyMs: raw.latencyMs || null,
    modelName:
      raw.modelName ||
      (raw.model && (raw.model.name || raw.model.modelName || raw.model.code)) ||
      '',
    callLogId: raw.callLogId || null,
    finishReason: raw.finishReason || '',
    sources: [],
    sourceOpen: true
  }
}

export function normalizeEmbeddingStatus(raw = {}) {
  return {
    targetType: raw.targetType || '',
    targetId: raw.targetId || null,
    totalChunkCount: Number(raw.totalChunkCount || raw.total || 0) || 0,
    embeddedChunkCount: Number(raw.embeddedChunkCount || raw.embedded || 0) || 0,
    createdEmbeddingCount: Number(raw.createdEmbeddingCount || raw.created || 0) || 0,
    provider: raw.provider || '',
    modelName: raw.modelName || '',
    dimension: raw.dimension || null
  }
}

export function getEmbeddingCompletionRate(status = {}) {
  const total = Number(status.totalChunkCount || 0)
  const embedded = Number(status.embeddedChunkCount || 0)
  if (!total) return 0
  return Math.min(100, Math.round((embedded / total) * 100))
}

export function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

export function kbStatusTagType(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'ACTIVE' || value === 'READY') return 'success'
  if (value === 'PENDING' || value === 'BUILDING') return 'warning'
  if (value === 'FAILED' || value === 'DELETED') return 'danger'
  return 'info'
}

export function docStatusTagType(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'INDEXED' || value === 'READY') return 'success'
  if (value === 'PENDING' || value === 'UPLOADING' || value === 'PROCESSING') return 'warning'
  if (value === 'FAILED' || value === 'ERROR') return 'danger'
  return 'info'
}

export function taskStatusTagType(status) {
  const value = String(status || '').toUpperCase()
  if (value === 'SUCCESS' || value === 'DONE' || value === 'COMPLETED') return 'success'
  if (value === 'PENDING' || value === 'RUNNING') return 'warning'
  if (value === 'FAILED' || value === 'ERROR' || value === 'CANCELLED') return 'danger'
  return 'info'
}

export function readStoredUserInfo() {
  if (typeof localStorage === 'undefined') return null
  try {
    const raw = localStorage.getItem('userInfo') || localStorage.getItem('user')
    if (!raw) return null
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}
